package com.subgraph.orchid.downloader;

import com.subgraph.orchid.BootstrapStatus;
import com.subgraph.orchid.certificate.KeyCertificate;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.document.ConsensusDocument.RequiredCertificate;
import com.subgraph.orchid.events.TorInitializationTracker;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.router.RouterDescriptor;
import com.subgraph.orchid.router.RouterMicrodescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * Synchronously downloads directory documents.
 */
public class DirectoryDocumentRequestor {
    private final static int OPEN_DIRECTORY_STREAM_TIMEOUT = 10 * 1000;
    private final TorInitializationTracker initializationTracker;

    public DirectoryDocumentRequestor(TorInitializationTracker initializationTracker) {
        this.initializationTracker = initializationTracker;
    }

    public RouterDescriptor downloadBridgeDescriptor(Router bridge) throws DirectoryRequestFailedException {
        return fetchSingleDocument(new BridgeDescriptorFetcher());
    }

    public ConsensusDocument downloadCurrentConsensus(boolean useMicrodescriptors) throws DirectoryRequestFailedException {
        return fetchSingleDocument(new ConsensusFetcher(useMicrodescriptors), BootstrapStatus.LOADING_STATUS);
    }

    public List<KeyCertificate> downloadKeyCertificates(Set<RequiredCertificate> required) throws DirectoryRequestFailedException {
        return fetchDocuments(new CertificateFetcher(required), BootstrapStatus.LOADING_KEYS);
    }

    public List<RouterDescriptor> downloadRouterDescriptors(Set<HexDigest> fingerprints) throws DirectoryRequestFailedException {
        return fetchDocuments(new RouterDescriptorFetcher(fingerprints), BootstrapStatus.LOADING_DESCRIPTORS);
    }

    public List<RouterMicrodescriptor> downloadRouterMicrodescriptors(Set<HexDigest> fingerprints) throws DirectoryRequestFailedException {
        return fetchDocuments(new MicrodescriptorFetcher(fingerprints), BootstrapStatus.LOADING_DESCRIPTORS);
    }

    private <T> T fetchSingleDocument(DocumentFetcher<T> fetcher) throws DirectoryRequestFailedException {
        return fetchSingleDocument(fetcher, BootstrapStatus.STARTING);
    }

    private <T> @Nullable T fetchSingleDocument(DocumentFetcher<T> fetcher, BootstrapStatus status) throws DirectoryRequestFailedException {
        List<T> result = fetchDocuments(fetcher, status);
        if (result.size() == 1) {
            return result.getFirst();
        }
        return null;
    }

    private <T> List<T> fetchDocuments(@NotNull DocumentFetcher<T> fetcher, BootstrapStatus status) throws DirectoryRequestFailedException {
        notifyInitialization(status);
        return fetcher.requestDocuments();
    }

    private void notifyInitialization(BootstrapStatus status) {
        if (initializationTracker != null) {
            initializationTracker.notifyEvent(status);
        }
    }
}