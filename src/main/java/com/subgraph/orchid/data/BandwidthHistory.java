package com.subgraph.orchid.data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Deprecated
public record BandwidthHistory(Instant reportingTime, int reportingInterval, List<Integer> samples) {
	public BandwidthHistory(Instant reportingTime, int reportingInterval) {
		this(reportingTime, reportingInterval, new ArrayList<>());
	}

	public List<Integer> getSamples() {
		return List.copyOf(samples);
	}

	@Deprecated
	public void addSample(Integer sample) {
		samples.add(sample);
	}

	public BandwidthHistory addNewSample(Integer sample) {
		List<Integer> newSamples = new ArrayList<>(samples);
		newSamples.add(sample);
		return new BandwidthHistory(reportingTime, reportingInterval, newSamples);
	}
}