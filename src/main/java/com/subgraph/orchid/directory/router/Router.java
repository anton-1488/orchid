package com.subgraph.orchid.directory.router;

import com.subgraph.orchid.directory.document.Descriptor;
import com.subgraph.orchid.crypto.TorPublicKey;
import com.subgraph.orchid.data.HexDigest;

import java.net.InetAddress;
import java.util.Set;

public interface Router {

	String getNickname();
	String getCountryCode();
	InetAddress getAddress();
	int getOnionPort();
	int getDirectoryPort();
	TorPublicKey getIdentityKey();
	HexDigest getIdentityHash();
	boolean isDescriptorDownloadable();

	String getVersion();
	Descriptor getCurrentDescriptor();
	HexDigest getDescriptorDigest();
	HexDigest getMicrodescriptorDigest();

	TorPublicKey getOnionKey();
	byte[] getNTorOnionKey();
	
	boolean hasBandwidth();
	int getEstimatedBandwidth();
	int getMeasuredBandwidth();

	Set<String> getFamilyMembers();
	int getAverageBandwidth();
	int getBurstBandwidth();
	int getObservedBandwidth();
	boolean isHibernating();
	boolean isRunning();
	boolean isValid();
	boolean isBadExit();
	boolean isPossibleGuard();
	boolean isExit();
	boolean isFast();
	boolean isStable();
	boolean isHSDirectory();
	boolean exitPolicyAccepts(InetAddress address, int port);
	boolean exitPolicyAccepts(int port);
}
