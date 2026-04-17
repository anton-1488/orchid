package com.subgraph.orchid.directory;

import com.subgraph.orchid.GuardEntry;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.RandomList;
import com.subgraph.orchid.data.StateFile;
import com.subgraph.orchid.data.TrustedAuthorities;
import com.subgraph.orchid.directory.DirectoryStore.CacheFile;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.document.ConsensusDocument.ConsensusFlavor;
import com.subgraph.orchid.document.ConsensusDocument.RequiredCertificate;
import com.subgraph.orchid.document.Descriptor;
import com.subgraph.orchid.document.DescriptorCache;
import com.subgraph.orchid.document.DocumentParserFactoryImpl;
import com.subgraph.orchid.events.Event;
import com.subgraph.orchid.events.EventHandler;
import com.subgraph.orchid.events.EventManager;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.parsing.DocumentParser;
import com.subgraph.orchid.parsing.DocumentParserFactory;
import com.subgraph.orchid.parsing.DocumentParsingResult;
import com.subgraph.orchid.router.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DirectoryImpl implements Directory {
    private final static DocumentParserFactory parserFactory = new DocumentParserFactoryImpl();
    private static final Logger log = LoggerFactory.getLogger(DirectoryImpl.class);

    private final Object loadLock = new Object();
    private final DirectoryStore store;
    private final TorConfig config;
    private final StateFile stateFile;
    private final DescriptorCache<RouterMicrodescriptor> microdescriptorCache;
    private final DescriptorCache<RouterDescriptor> basicDescriptorCache;
    private final Map<HexDigest, RouterImpl> routersByIdentity = new ConcurrentHashMap<>();
    private final Map<String, RouterImpl> routersByNickname = new ConcurrentHashMap<>();
    private final RandomList<RouterImpl> directoryCaches = new RandomList<>();
    private final Set<ConsensusDocument.RequiredCertificate> requiredCertificates = new HashSet<>();
    private final EventManager eventManager = new EventManager();

    private boolean isLoaded = false;
    private ConsensusDocument currentConsensus;
    private ConsensusDocument consensusWaitingForCertificates;
    private boolean haveMinimumRouterInfo;
    private boolean needRecalculateMinimumRouterInfo;

    public DirectoryImpl(TorConfig config, DirectoryStore customDirectoryStore) {
        store = (customDirectoryStore == null) ? (new DirectoryStoreImpl(config)) : (customDirectoryStore);
        this.config = config;
        stateFile = new StateFile(store, this);
        microdescriptorCache = createMicrodescriptorCache(store);
        basicDescriptorCache = createBasicDescriptorCache(store);
    }

    @Contract("_ -> new")
    private static @NotNull DescriptorCache<RouterMicrodescriptor> createMicrodescriptorCache(DirectoryStore store) {
        return new DescriptorCache<>(store, CacheFile.MICRODESCRIPTOR_CACHE, CacheFile.MICRODESCRIPTOR_JOURNAL) {
            @Override
            protected DocumentParser<RouterMicrodescriptor> createDocumentParser(ByteBuffer buffer) {
                return parserFactory.createRouterMicrodescriptorParser(buffer);
            }
        };
    }

    @Contract("_ -> new")
    private static @NotNull DescriptorCache<RouterDescriptor> createBasicDescriptorCache(DirectoryStore store) {
        return new DescriptorCache<>(store, CacheFile.DESCRIPTOR_CACHE, CacheFile.DESCRIPTOR_JOURNAL) {
            @Override
            protected DocumentParser<RouterDescriptor> createDocumentParser(ByteBuffer buffer) {
                return parserFactory.createRouterDescriptorParser(buffer, false);
            }
        };
    }

    public synchronized boolean haveMinimumRouterInfo() {
        if (needRecalculateMinimumRouterInfo) {
            checkMinimumRouterInfo();
        }
        return haveMinimumRouterInfo;
    }

    private synchronized void checkMinimumRouterInfo() {
        if (currentConsensus == null || !currentConsensus.isLive()) {
            needRecalculateMinimumRouterInfo = true;
            haveMinimumRouterInfo = false;
            return;
        }

        int routerCount = 0;
        int descriptorCount = 0;
        for (Router r : routersByIdentity.values()) {
            routerCount++;
            if (!r.isDescriptorDownloadable()) {
                descriptorCount++;
            }
        }
        needRecalculateMinimumRouterInfo = false;
        haveMinimumRouterInfo = (descriptorCount * 4 > routerCount);
    }

    @Override
    public void loadFromStore() {
        synchronized (loadLock) {
            log.info("Loading cached network information from disk");

            if (isLoaded) {
                return;
            }

            boolean useMicrodescriptors = config.useMicroDescriptors();
            last = System.currentTimeMillis();
            log.info("Loading certificates");
            loadCertificates(store.loadCacheFile(CacheFile.CERTIFICATES));
            logElapsed();

            log.info("Loading consensus");
            loadConsensus(store.loadCacheFile(useMicrodescriptors ? CacheFile.CONSENSUS_MICRODESC : CacheFile.CONSENSUS));
            logElapsed();

            if (!useMicrodescriptors) {
                log.info("Loading descriptors");
                basicDescriptorCache.initialLoad();
            } else {
                log.info("Loading microdescriptor cache");
                microdescriptorCache.initialLoad();
            }
            needRecalculateMinimumRouterInfo = true;
            logElapsed();

            log.info("loading state file");
            stateFile.parseBuffer(store.loadCacheFile(CacheFile.STATE));
            logElapsed();

            isLoaded = true;
            loadLock.notifyAll();
        }
    }

    @Override
    public void close() {
        basicDescriptorCache.shutdown();
        microdescriptorCache.shutdown();
    }

    private long last = 0;

    private void logElapsed() {
        long now = System.currentTimeMillis();
        long elapsed = now - last;
        last = now;
        log.debug("Loaded in {} ms.", elapsed);
    }

    private void loadCertificates(ByteBuffer buffer) {
        DocumentParser<KeyCertificate> parser = parserFactory.createKeyCertificateParser(buffer);
        DocumentParsingResult<KeyCertificate> result = parser.parse();
        if (testResult(result, "certificates")) {
            for (KeyCertificate cert : result.getParsedDocuments()) {
                addCertificate(cert);
            }
        }
    }

    private void loadConsensus(ByteBuffer buffer) {
        DocumentParser<ConsensusDocument> parser = parserFactory.createConsensusDocumentParser(buffer);
        DocumentParsingResult<ConsensusDocument> result = parser.parse();
        if (testResult(result, "consensus")) {
            addConsensusDocument(result.getDocument(), true);
        }
    }

    private boolean testResult(@NotNull DocumentParsingResult<?> result, String type) {
        if (result.isOkay()) {
            return true;
        } else if (result.isError()) {
            log.warn("Parsing error loading {} : {}", type, result.getMessage());
        } else if (result.isInvalid()) {
            log.warn("Problem loading {} : {}", type, result.getMessage());
        } else {
            log.warn("Unknown problem loading {}", type);
        }
        return false;
    }

    @Override
    public void waitUntilLoaded() {
        synchronized (loadLock) {
            while (!isLoaded) {
                try {
                    loadLock.wait();
                } catch (InterruptedException e) {
                    log.warn("Thread interrupted while waiting for directory to load from disk");
                }
            }
        }
    }

    @Override
    public Collection<DirectoryServer> getDirectoryAuthorities() {
        return TrustedAuthorities.getInstance().getAuthorityServers();
    }

    @Override
    public DirectoryServer getRandomDirectoryAuthority() {
        List<DirectoryServer> servers = TrustedAuthorities.getInstance().getAuthorityServers();
        int idx = TorRandom.nextInt(servers.size());
        return servers.get(idx);
    }

    @Override
    public Set<ConsensusDocument.RequiredCertificate> getRequiredCertificates() {
        return Set.copyOf(requiredCertificates);
    }

    @Override
    public void addCertificate(KeyCertificate certificate) {
        boolean wasRequired = removeRequiredCertificate(certificate);
        DirectoryServer as = TrustedAuthorities.getInstance().getAuthorityServerByIdentity(certificate.getAuthorityFingerprint());
        if (as == null) {
            log.warn("Certificate read for unknown directory authority with identity: {}", certificate.getAuthorityFingerprint());
            return;
        }
        as.addCertificate(certificate);
        if (consensusWaitingForCertificates != null && wasRequired) {
            switch (consensusWaitingForCertificates.verifySignatures()) {
                case STATUS_FAILED:
                    consensusWaitingForCertificates = null;
                    return;
                case STATUS_VERIFIED:
                    addConsensusDocument(consensusWaitingForCertificates, false);
                    consensusWaitingForCertificates = null;
                    return;
                case STATUS_NEED_CERTS:
                    requiredCertificates.addAll(consensusWaitingForCertificates.getRequiredCertificates());
            }
        }
    }

    private boolean removeRequiredCertificate(KeyCertificate certificate) {
        Iterator<RequiredCertificate> it = requiredCertificates.iterator();
        while (it.hasNext()) {
            RequiredCertificate r = it.next();
            if (r.getSigningKey().equals(certificate.getAuthoritySigningKey().getFingerprint())) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public synchronized void storeCertificates() {
        List<KeyCertificate> certs = new ArrayList<>();
        for (DirectoryServer ds : TrustedAuthorities.getInstance().getAuthorityServers()) {
            certs.addAll(ds.getCertificates());
        }
        store.writeDocumentList(CacheFile.CERTIFICATES, certs);
    }

    @Override
    public void addRouterDescriptors(List<RouterDescriptor> descriptors) {
        basicDescriptorCache.addDescriptors(descriptors);
        needRecalculateMinimumRouterInfo = true;
    }

    @Override
    public synchronized void addConsensusDocument(ConsensusDocument consensus, boolean fromCache) {
        if (consensus.equals(currentConsensus)) {
            return;
        }

        if (currentConsensus != null && consensus.getValidAfterTime().isBefore(currentConsensus.getValidAfterTime())) {
            log.warn("New consensus document is older than current consensus document");
            return;
        }

        switch (consensus.verifySignatures()) {
            case STATUS_FAILED:
                log.warn("Unable to verify signatures on consensus document, discarding...");
                return;
            case STATUS_NEED_CERTS:
                consensusWaitingForCertificates = consensus;
                requiredCertificates.addAll(consensus.getRequiredCertificates());
                return;
        }
        requiredCertificates.addAll(consensus.getRequiredCertificates());
        Map<HexDigest, RouterImpl> oldRouterByIdentity = new HashMap<>(routersByIdentity);
        clearAll();

        for (RouterStatus status : consensus.getRouterStatusEntries()) {
            if (status.hasFlag("Running") && status.hasFlag("Valid")) {
                RouterImpl router = updateOrCreateRouter(status, oldRouterByIdentity);
                addRouter(router);
                classifyRouter(router);
            }
            Descriptor d = getDescriptorForRouterStatus(status, consensus.getFlavor() == ConsensusFlavor.MICRODESC);
            if (d != null) {
                d.setLastListed(consensus.getValidAfterTime().toEpochMilli());
            }
        }

        log.debug("Loaded {} routers from consensus document", routersByIdentity.size());
        currentConsensus = consensus;

        if (!fromCache) {
            storeCurrentConsensus();
        }

        eventManager.fireEvent(new Event() {
        });
    }

    private void storeCurrentConsensus() {
        if (currentConsensus != null) {
            if (currentConsensus.getFlavor() == ConsensusFlavor.MICRODESC) {
                store.writeDocument(CacheFile.CONSENSUS_MICRODESC, currentConsensus);
            } else {
                store.writeDocument(CacheFile.CONSENSUS, currentConsensus);
            }
        }
    }

    private Descriptor getDescriptorForRouterStatus(RouterStatus rs, boolean isMicrodescriptor) {
        if (isMicrodescriptor) {
            return microdescriptorCache.getDescriptor(rs.getMicrodescriptorDigest());
        } else {
            return basicDescriptorCache.getDescriptor(rs.getDescriptorDigest());
        }
    }

    private @NotNull RouterImpl updateOrCreateRouter(@NotNull RouterStatus status, @NotNull Map<HexDigest, RouterImpl> knownRouters) {
        RouterImpl router = knownRouters.get(status.getIdentity());
        if (router == null) {
            return RouterImpl.createFromRouterStatus(this, status);
        }
        router.updateStatus(status);
        return router;
    }

    private void clearAll() {
        routersByIdentity.clear();
        routersByNickname.clear();
        directoryCaches.clear();
    }

    private void classifyRouter(RouterImpl router) {
        if (isValidDirectoryCache(router)) {
            directoryCaches.add(router);
        } else {
            directoryCaches.remove(router);
        }
    }

    private boolean isValidDirectoryCache(@NotNull RouterImpl router) {
        if (router.getDirectoryPort() == 0) {
            return false;
        }
        if (router.hasFlag("BadDirectory")) {
            return false;
        }
        return router.hasFlag("V2Dir");
    }

    private void addRouter(RouterImpl router) {
        routersByIdentity.put(router.getIdentityHash(), router);
        addRouterByNickname(router);
    }

    private void addRouterByNickname(@NotNull RouterImpl router) {
        String name = router.getNickname();
        if (name == null || name.equals("Unnamed"))
            return;
        if (routersByNickname.containsKey(router.getNickname())) {
            return;
        }
        routersByNickname.put(name, router);
    }

    @Override
    public synchronized void addRouterMicrodescriptors(List<RouterMicrodescriptor> microdescriptors) {
        microdescriptorCache.addDescriptors(microdescriptors);
        needRecalculateMinimumRouterInfo = true;
    }

    @Override
    public synchronized List<Router> getRoutersWithDownloadableDescriptors() {
        waitUntilLoaded();
        List<Router> routers = new ArrayList<>();
        for (RouterImpl router : routersByIdentity.values()) {
            if (router.isDescriptorDownloadable())
                routers.add(router);
        }

        for (int i = 0; i < routers.size(); i++) {
            Router a = routers.get(i);
            int swapIdx = TorRandom.nextInt(routers.size());
            Router b = routers.get(swapIdx);
            routers.set(i, b);
            routers.set(swapIdx, a);
        }

        return routers;
    }

    @Override
    public ConsensusDocument getCurrentConsensusDocument() {
        return currentConsensus;
    }

    @Override
    public boolean hasPendingConsensus() {
        return consensusWaitingForCertificates != null;
    }

    @Override
    public void registerConsensusChangedHandler(EventHandler handler) {
        eventManager.addListener(handler);
    }

    @Override
    public void unregisterConsensusChangedHandler(EventHandler handler) {
        eventManager.removeListener(handler);
    }

    @Override
    public Router getRouterByName(@NotNull String name) {
        if (name.equals("Unnamed")) {
            return null;
        }
        if (name.length() == 41 && name.charAt(0) == '$') {
            try {
                HexDigest identity = HexDigest.createFromString(name.substring(1));
                return getRouterByIdentity(identity);
            } catch (Exception e) {
                return null;
            }
        }
        waitUntilLoaded();
        return routersByNickname.get(name);
    }

    @Override
    public Router getRouterByIdentity(HexDigest identity) {
        waitUntilLoaded();
        return routersByIdentity.get(identity);
    }

    @Override
    public List<Router> getRouterListByNames(@NotNull List<String> names) {
        waitUntilLoaded();
        List<Router> routers = new ArrayList<>();
        for (String n : names) {
            Router r = getRouterByName(n);
            if (r == null) {
                throw new TorException("Could not find router named: " + n);
            }
            routers.add(r);
        }
        return routers;
    }

    @Override
    public List<Router> getAllRouters() {
        waitUntilLoaded();
        return List.copyOf(routersByIdentity.values());
    }

    @Override
    public GuardEntry createGuardEntryFor(Router router) {
        waitUntilLoaded();
        return stateFile.createGuardEntryFor(router);
    }

    @Override
    public List<GuardEntry> getGuardEntries() {
        waitUntilLoaded();
        return stateFile.getGuardEntries();
    }

    @Override
    public void removeGuardEntry(GuardEntry entry) {
        waitUntilLoaded();
        stateFile.removeGuardEntry(entry);
    }

    @Override
    public void addGuardEntry(GuardEntry entry) {
        waitUntilLoaded();
        stateFile.addGuardEntry(entry);
    }

    @Override
    public RouterMicrodescriptor getMicrodescriptorFromCache(HexDigest descriptorDigest) {
        return microdescriptorCache.getDescriptor(descriptorDigest);
    }


    @Override
    public RouterDescriptor getBasicDescriptorFromCache(HexDigest descriptorDigest) {
        return basicDescriptorCache.getDescriptor(descriptorDigest);
    }
}