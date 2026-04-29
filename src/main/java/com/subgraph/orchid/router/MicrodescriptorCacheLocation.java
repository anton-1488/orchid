package com.subgraph.orchid.router;

import org.jetbrains.annotations.NotNull;

public record MicrodescriptorCacheLocation(int offset, int length) {
	@Override
	public @NotNull String toString() {
		return "MD Cache offset: " + offset + " length: " + length;
	}
}