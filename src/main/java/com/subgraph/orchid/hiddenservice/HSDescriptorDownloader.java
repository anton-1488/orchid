package com.subgraph.orchid.hiddenservice;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.subgraph.orchid.directory.DirectoryCircuit;
import com.subgraph.orchid.circuits.InternalCircuit;
import com.subgraph.orchid.exceptions.OpenFailedException;
import com.subgraph.orchid.router.Router;
import com.subgraph.orchid.Stream;
import com.subgraph.orchid.exceptions.StreamConnectFailedException;
import com.subgraph.orchid.exceptions.TorException;
import com.subgraph.orchid.circuits.CircuitManagerImpl;
import com.subgraph.orchid.document.DocumentFieldParserImpl;
import com.subgraph.orchid.exceptions.DirectoryRequestFailedException;
import com.subgraph.orchid.downloader.request.TorHttpClient;
import com.subgraph.orchid.parsing.DocumentParsingResultHandler;

public class HSDescriptorDownloader {
	private final static Logger logger = LoggerFactory.getLogger(HSDescriptorDownloader.class);

	private final HiddenService hiddenService;
	private final CircuitManagerImpl circuitManager;
	private final List<HSDescriptorDirectory> directories;
	
	public HSDescriptorDownloader(HiddenService hiddenService, CircuitManagerImpl circuitManager, List<HSDescriptorDirectory> directories) {
		this.hiddenService = hiddenService;
		this.circuitManager = circuitManager;
		this.directories = directories;
	}

	
	public HSDescriptor downloadDescriptor() {
		for(HSDescriptorDirectory d: directories) {
			HSDescriptor descriptor = downloadDescriptorFrom(d);
			if(descriptor != null) {
				return descriptor;
			}
		}
		// All directories failed
		return null;
	}
	
	private HSDescriptor downloadDescriptorFrom(HSDescriptorDirectory dd) {
		logger.debug("Downloading descriptor from "+ dd.getDirectory());
		
		Stream stream = null;
		try {
			stream = openHSDirectoryStream(dd.getDirectory());
			// Rewritten: old stream-based API removed
			String url = "http://127.0.0.1/tor/rendezvous2/" + dd.getDescriptorId().toBase32();
			
			if(true) { // TODO: check response status via new API
				return null; // TODO: reimplement with TorHttpClient.sendGetRequest()
			} else {
				// TODO: handle failure
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		} catch (TimeoutException e) {
			logger.debug("Timeout downloading HS descriptor from "+ dd.getDirectory());
			e.printStackTrace();
			return null;
		} catch (OpenFailedException e) {
			logger.info("Failed to open stream to HS directory "+ dd.getDirectory() +" : "+ e.getMessage());
			return null;
		} catch (DirectoryRequestFailedException e) {
			logger.info("Directory request to HS directory "+ dd.getDirectory() + " failed "+ e.getMessage());
			return null;
		} finally {
			if(stream != null) {
				try { stream.close(); } catch (Exception ignored) {}
				stream.getCircuit().markForClose();
			}
		}
		
		return null;
		
	}
	
	private Stream openHSDirectoryStream(Router directory) throws TimeoutException, InterruptedException, OpenFailedException {

		final InternalCircuit circuit = circuitManager.getCleanInternalCircuit();
		
		try {
			final DirectoryCircuit dc = circuit.cannibalizeToDirectory(directory);
			return dc.openDirectoryStream(10000, true);
		} catch (StreamConnectFailedException e) {
			circuit.markForClose();
			throw new OpenFailedException("Failed to open directory stream");
		} catch (TorException e) {
			circuit.markForClose();
			throw new OpenFailedException("Failed to extend circuit to HS directory: "+ e.getMessage());
		}
	}

	private HSDescriptor readDocument(HSDescriptorDirectory dd, ByteBuffer body) {
		DocumentFieldParserImpl fieldParser = new DocumentFieldParserImpl(body);
		HSDescriptorParser parser = new HSDescriptorParser(hiddenService, fieldParser, hiddenService.getAuthenticationCookie());
		DescriptorParseResult result = new DescriptorParseResult(dd);
		parser.parse(result);
		return result.getDescriptor();
	}
	
	private static class DescriptorParseResult implements DocumentParsingResultHandler<HSDescriptor> {
		HSDescriptorDirectory dd;
		HSDescriptor descriptor;
		
		public DescriptorParseResult(HSDescriptorDirectory dd) {
			this.dd = dd;
		}
	
		HSDescriptor getDescriptor() {
			return descriptor;
		}
		public void documentParsed(HSDescriptor document) {
			this.descriptor = document;
		}

		public void documentInvalid(HSDescriptor document, String message) {
			logger.info("Invalid HS descriptor document received from "+ dd.getDirectory() + " for descriptor "+ dd.getDescriptorId());
		}

		public void parsingError(String message) {
			logger.info("Failed to parse HS descriptor document received from "+ dd.getDirectory() + " for descriptor "+ dd.getDescriptorId() + " : " + message);
		}
	}
}