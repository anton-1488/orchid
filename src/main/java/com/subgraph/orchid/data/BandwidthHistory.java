package com.subgraph.orchid.data;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Deprecated(forRemoval = true)
public record BandwidthHistory(Instant reportingTime, int reportingInterval, List<Integer> samples) {
	public BandwidthHistory(Instant reportingTime, int reportingInterval) {
		this(reportingTime, reportingInterval, new ArrayList<>());
	}

	@Contract(pure = true)
	@Deprecated
	public @NotNull List<Integer> getSamples() {
		return List.copyOf(samples);
	}

	@Deprecated
	public void addSample(Integer sample) {
		samples.add(sample);
	}

	@Contract("_ -> new")
	@Deprecated
	public @NotNull BandwidthHistory addNewSample(Integer sample) {
		List<Integer> newSamples = new ArrayList<>(samples);
		newSamples.add(sample);
		return new BandwidthHistory(reportingTime, reportingInterval, newSamples);
	}
}