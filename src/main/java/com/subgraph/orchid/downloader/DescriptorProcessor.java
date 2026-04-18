package com.subgraph.orchid.downloader;

import com.subgraph.orchid.config.TorConfig;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.directory.Directory;
import com.subgraph.orchid.document.ConsensusDocument;
import com.subgraph.orchid.router.Router;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DescriptorProcessor {
    private final static int MAX_DL_PER_REQUEST = 96;
    private final static int MAX_DL_TO_DELAY = 16;
    private final static int MIN_DL_REQUESTS = 3;
    private final static int MAX_CLIENT_INTERVAL_WITHOUT_REQUEST = 10 * 60 * 1000;

    private final TorConfig config;
    private final Directory directory;

    private Instant lastDescriptorDownload;

    public DescriptorProcessor(TorConfig config, Directory directory) {
        this.config = config;
        this.directory = directory;
    }

    private boolean canDownloadDescriptors(int downloadableCount) {
        if (downloadableCount >= MAX_DL_TO_DELAY) {
            return true;
        }
        if (downloadableCount == 0) {
            return false;
        }
        if (lastDescriptorDownload == null) {
            return true;
        }
        Instant now = Instant.now();
        long diff = now.toEpochMilli() - lastDescriptorDownload.toEpochMilli();
        return diff > MAX_CLIENT_INTERVAL_WITHOUT_REQUEST;
    }

    /**
     * SEE: dir-spec.txt section 5.3
     *
     * @param descriptors list of descriptiors.
     * @return list of digests
     */
    private @NotNull List<List<HexDigest>> partitionDescriptors(@NotNull List<Router> descriptors) {
        int size = descriptors.size();
        List<List<HexDigest>> partitions = new ArrayList<>();
        if (size <= 10) {
            partitions.add(createPartitionList(descriptors, 0, size));
            return partitions;
        } else if (size <= (MIN_DL_REQUESTS * MAX_DL_PER_REQUEST)) {
            int chunk = size / MIN_DL_REQUESTS;
            int over = size % MIN_DL_REQUESTS;
            int off = 0;
            for (int i = 0; i < MIN_DL_REQUESTS; i++) {
                int sz = chunk;
                if (over != 0) {
                    sz++;
                    over--;
                }
                partitions.add(createPartitionList(descriptors, off, sz));
                off += sz;
            }
            return partitions;
        } else {
            int off = 0;
            while (off < descriptors.size()) {
                partitions.add(createPartitionList(descriptors, off, MAX_DL_PER_REQUEST));
                off += MAX_DL_PER_REQUEST;
            }
            return partitions;
        }
    }

    private @NotNull List<HexDigest> createPartitionList(List<Router> descriptors, int offset, int size) {
        List<HexDigest> newList = new ArrayList<>();
        for (int i = offset; i < (offset + size) && i < descriptors.size(); i++) {
            HexDigest digest = getDescriptorDigestForRouter(descriptors.get(i));
            newList.add(digest);
        }
        return newList;
    }

    private HexDigest getDescriptorDigestForRouter(Router r) {
        if (useMicrodescriptors()) {
            return r.getMicrodescriptorDigest();
        } else {
            return r.getDescriptorDigest();
        }
    }

    private boolean useMicrodescriptors() {
        return config.useMicroDescriptors();
    }

    public List<List<HexDigest>> getDescriptorDigestsToDownload() {
        ConsensusDocument consensus = directory.getCurrentConsensusDocument();
        if (consensus == null || !consensus.isLive()) {
            return Collections.emptyList();
        }
        List<Router> downloadables = directory.getRoutersWithDownloadableDescriptors();
        if (!canDownloadDescriptors(downloadables.size())) {
            return Collections.emptyList();
        }

        lastDescriptorDownload = Instant.now();
        return partitionDescriptors(downloadables);
    }
}