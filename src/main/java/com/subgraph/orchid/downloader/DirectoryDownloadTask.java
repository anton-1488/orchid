package com.subgraph.orchid.downloader;

import com.subgraph.orchid.Globals;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.crypto.TorRandom;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.directory.DirectoryDownloader;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectoryDownloadTask implements Runnable {
    private static final ExecutorService executor = Globals.VIRTUAL_EXECUTOR;
    private static final Logger log = LoggerFactory.getLogger(DirectoryDownloadTask.class);
    private final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    private final TorConfig config;
    private final Directory directory;
    private final DirectoryDownloader downloader;
    private final DescriptorProcessor descriptorProcessor;
    private final AtomicInteger outstandingDescriptorTasks = new AtomicInteger();

    private volatile boolean isDownloadingCertificates;
    private volatile boolean isDownloadingConsensus;
    private volatile boolean isStopped;
    private ConsensusDocument currentConsensus;
    private Instant consensusDownloadTime;

    DirectoryDownloadTask(TorConfig config, Directory directory, DirectoryDownloader downloader) {
        this.config = config;
        this.directory = directory;
        this.downloader = downloader;
        this.descriptorProcessor = new DescriptorProcessor(config, directory);
    }

    public synchronized void stop() {
        if (isStopped) {
            return;
        }
        executor.shutdownNow();
        scheduledExecutor.shutdownNow();
        isStopped = true;
    }

    @Override
    public void run() {
        directory.loadFromStore();
        directory.waitUntilLoaded();
        setCurrentConsensus(directory.getCurrentConsensusDocument());
        scheduledExecutor.scheduleAtFixedRate(() -> {
            checkCertificates();
            checkConsensus();
            checkDescriptors();
        }, 0, 5000, TimeUnit.SECONDS);
    }

    private void checkCertificates() {
        if (isDownloadingCertificates || directory.getRequiredCertificates().isEmpty()) {
            return;
        }

        isDownloadingCertificates = true;
        executor.execute(() -> {
            try {
                for (KeyCertificate c : downloader.downloadKeyCertificates(directory.getRequiredCertificates())) {
                    directory.addCertificate(c);
                }
                directory.storeCertificates();
            } catch (DirectoryRequestFailedException e) {
                log.warn("Failed to download key certificates: {}", e.getMessage());
            } finally {
                isDownloadingCertificates = false;
            }
        });
    }

    public void setCurrentConsensus(ConsensusDocument consensus) {
        if (consensus != null) {
            currentConsensus = consensus;
            consensusDownloadTime = chooseDownloadTimeForConsensus(consensus);
        } else {
            currentConsensus = null;
            consensusDownloadTime = null;
        }
    }

    /**
     * dir-spec 5.1: Downloading network-status documents
     * <p>
     * To avoid swarming the caches whenever a consensus expires, the clients
     * download new consensuses at a randomly chosen time after the caches are
     * expected to have a fresh consensus, but before their consensus will
     * expire. (This time is chosen uniformly at random from the interval
     * between the time 3/4 into the first interval after the consensus is no
     * longer fresh, and 7/8 of the time remaining after that before the
     * consensus is invalid.)
     * <p>
     * [For example, if a cache has a consensus that became valid at 1:00, and
     * is fresh until 2:00, and expires at 4:00, that cache will fetch a new
     * consensus at a random time between 2:45 and 3:50, since 3/4 of the
     * one-hour interval is 45 minutes, and 7/8 of the remaining 75 minutes is
     * 65 minutes.]
     */
    private Instant chooseDownloadTimeForConsensus(@NotNull ConsensusDocument consensus) {
        long va = consensus.getValidAfterTime().toEpochMilli();
        long fu = consensus.getFreshUntilTime().toEpochMilli();
        long vu = consensus.getValidUntilTime().toEpochMilli();
        long i1 = fu - va;
        long start = fu + ((i1 * 3) / 4);
        long i2 = ((vu - start) * 7) / 8;
        long r = TorRandom.nextLong(i2);
        long download = start + r;
        return Instant.ofEpochMilli(download);
    }

    private boolean needConsensusDownload() {
        if (directory.hasPendingConsensus()) {
            return false;
        }
        if (currentConsensus == null || !currentConsensus.isLive()) {
            if (currentConsensus == null) {
                log.info("Downloading consensus because we have no consensus document");
            } else {
                log.info("Downloading consensus because the document we have is not live");
            }
            return true;
        }
        return consensusDownloadTime.isBefore(Instant.now());
    }

    private void checkConsensus() {
        if (isDownloadingConsensus || !needConsensusDownload()) {
            return;
        }
        isDownloadingConsensus = true;
        executor.execute(() -> {
            try {
                ConsensusDocument consensus = downloader.downloadCurrentConsensus(useMicrodescriptors());
                setCurrentConsensus(consensus);
                directory.addConsensusDocument(consensus, false);

            } catch (DirectoryRequestFailedException e) {
                log.warn("Failed to download current consensus document: {}", e.getMessage());
            } finally {
                isDownloadingConsensus = false;
            }
        });
    }

    private void checkDescriptors() {
        if (outstandingDescriptorTasks.get() > 0) {
            return;
        }
        List<List<HexDigest>> ds = descriptorProcessor.getDescriptorDigestsToDownload();
        if (ds.isEmpty()) {
            return;
        }
        for (List<HexDigest> dlist : ds) {
            outstandingDescriptorTasks.incrementAndGet();
            executor.execute(() -> {
                try {
                    Set<HexDigest> fingerprint = new HashSet<>(dlist);
                    if (useMicrodescriptors()) {
                        directory.addRouterMicrodescriptors(downloader.downloadRouterMicrodescriptors(fingerprint));
                    } else {
                        directory.addRouterDescriptors(downloader.downloadRouterDescriptors(fingerprint));
                    }
                } catch (DirectoryRequestFailedException e) {
                    log.warn("Failed to download router descriptors: {}", e.getMessage());
                } finally {
                    outstandingDescriptorTasks.decrementAndGet();
                }
            });
        }
    }

    private boolean useMicrodescriptors() {
        return config.useMicroDescriptors();
    }
}