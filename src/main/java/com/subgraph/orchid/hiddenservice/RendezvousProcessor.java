package com.subgraph.orchid.hiddenservice;
import com.subgraph.orchid.crypto.TorTapKeyAgreement;
import com.subgraph.orchid.cells.enums.RelayCellCommand;

import java.math.BigInteger;
import java.util.logging.Logger;

import com.subgraph.orchid.cells.Cell;
import com.subgraph.orchid.circuits.HiddenServiceCircuit;
import com.subgraph.orchid.circuits.InternalCircuit;
import com.subgraph.orchid.cells.RelayCell;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.circuits.CircuitNodeCryptoState;
import com.subgraph.orchid.circuits.CircuitNodeImpl;
import com.subgraph.orchid.crypto.TorMessageDigest;
import com.subgraph.orchid.crypto.TorRandom;

import com.subgraph.orchid.data.HexDigest;

public class RendezvousProcessor {
	private final static Logger logger = Logger.getLogger(RendezvousProcessor.class.getName());
	
	private final static int RENDEZVOUS_COOKIE_LEN = 20;
	// TorRandom is now static - no instance needed
	
	private final InternalCircuit circuit;
	private final byte[] cookie;
	
	protected RendezvousProcessor(InternalCircuit circuit) {
		this.circuit = circuit;
		this.cookie = TorRandom.getBytes(RENDEZVOUS_COOKIE_LEN);
	}
	
	boolean establishRendezvous() {
		final RelayCell cell = circuit.createRelayCell(RelayCellCommand.ESTABLISH_RENDEZVOUS, 0, circuit.getFinalCircuitNode());
		cell.getCellWriter().putByteArray(cookie);
		circuit.sendRelayCell(cell);
		final RelayCell response = circuit.receiveRelayCell();
		if(response == null) {
			logger.info("Timeout waiting for Rendezvous establish response");
			return false;
		} else if(response.getRelayCommand() != RelayCellCommand.RENDEZVOUS_ESTABLISHED) {
			logger.info("Response received from Rendezvous establish was not expected acknowledgement, Relay Command: "+ response.getRelayCommand());
			return false;
		} else {
			return true;
		}
	}
	
	HiddenServiceCircuit processRendezvous2(TorTapKeyAgreement kex) {
		final RelayCell cell = circuit.receiveRelayCell();
		if(cell == null) {
			logger.info("Timeout waiting for RENDEZVOUS2");
			return null;
		} else if (cell.getRelayCommand() != RelayCellCommand.RENDEZVOUS2) {
			logger.info("Unexpected Relay cell type received while waiting for RENDEZVOUS2: "+ cell.getRelayCommand());
			return null;
		}
		final BigInteger peerPublic = readPeerPublic(cell);
		final HexDigest handshakeDigest = readHandshakeDigest(cell);
		if(peerPublic == null || handshakeDigest == null) {
			return null;
		}
		final byte[] verifyHash = new byte[TorMessageDigest.TOR_DIGEST_SIZE];
		final byte[] keyMaterial = new byte[CircuitNodeCryptoState.KEY_MATERIAL_SIZE];
		if(!kex.deriveKeysFromDHPublicAndHash(peerPublic, handshakeDigest.getRawBytes(), keyMaterial, verifyHash)) {
			logger.info("Error deriving session keys while extending to hidden service");
			return null;
		}
		return circuit.connectHiddenService(CircuitNodeImpl.createAnonymous(circuit.getFinalCircuitNode(), keyMaterial, verifyHash));
	}
	
	private BigInteger readPeerPublic(Cell cell) {
		final byte[] dhPublic = new byte[TorTapKeyAgreement.DH_LEN];
		cell.getCellReader().getByteArray(dhPublic);
		final BigInteger peerPublic = new BigInteger(1, dhPublic);
		if(!TorTapKeyAgreement.isValidPublicValue(peerPublic)) {
			logger.warning("Illegal DH public value received: "+ peerPublic);
			return null;
		}
		return peerPublic;
	}
	
	HexDigest readHandshakeDigest(Cell cell) {
		final byte[] digestBytes = new byte[TorMessageDigest.TOR_DIGEST_SIZE];
		cell.getCellReader().getByteArray(digestBytes);
		return HexDigest.createFromDigestBytes(digestBytes);
	}
	
	
	byte[] getCookie() {
		return cookie;
	}

	Router getRendezvousRouter() {
		return circuit.getFinalCircuitNode().getRouter();
	}
}
