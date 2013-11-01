/**
 * Copyright (c) 2013 Center for eHalsa i samverkan (CeHis).
 * 							<http://cehis.se/>
 *
 * This file is part of SKLTP.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package se.skl.tp.vp.vagvalagent;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;

import se.skl.tp.hsa.cache.HsaCache;
import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheRequest;
import se.skl.tp.vagval.wsdl.v1.ResetVagvalCacheResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalRequest;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalResponse;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.AnropsBehorighetsInfoType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaAnropsBehorigheterResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.HamtaAllaVirtualiseringarResponseType;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsInfoInterface;
import se.skl.tp.vagvalsinfo.wsdl.v1.SokVagvalsServiceSoap11LitDocService;
import se.skl.tp.vagvalsinfo.wsdl.v1.VirtualiseringsInfoType;
import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.ClientUtil;

public class VagvalAgent implements VisaVagvalsInterface {

	private static final Logger logger = LoggerFactory.getLogger(VagvalAgent.class);
	public static final boolean FORCE_RESET = true;
	public static final boolean DONT_FORCE_RESET = false;

	// cache
	@XmlRootElement
	static class PersistentCache implements Serializable {
		private static final long serialVersionUID = 1L;
		@XmlElement
		private List<VirtualiseringsInfoType> virtualiseringsInfo;
		@XmlElement
		private List<AnropsBehorighetsInfoType> anropsBehorighetsInfo;
	}

	private static final JaxbUtil JAXB = new JaxbUtil(PersistentCache.class);
	
	private String localTakCache;

	private VagvalHandler vagvalHandler = null;
	private BehorighetHandler behorighetHandler = null;
	private HsaCache hsaCache;

	private String endpointAddressTjanstekatalog;
	private String addressDelimiter;
	
	private SokVagvalsInfoInterface port = null;

	public VagvalAgent() {

	}

	public void setEndpointAddress(String endpointAddressTjanstekatalog) {
		this.endpointAddressTjanstekatalog = endpointAddressTjanstekatalog;
	}

	public void setAddressDelimiter(String addressDelimiter) {
		this.addressDelimiter = addressDelimiter;
	}

	public void setHsaCache(HsaCache hsaCache) {
		this.hsaCache = hsaCache;
	}
	
	public void setLocalTakCache(String localTakCache) {
		this.localTakCache = localTakCache;
	}

	/**
	 * Initialize VagvalAgent resources. Force a init by setting forceReset=true, use
	 * constants VagvalAgent.FORCE_RESET or VagvalAgent.DONT_FORCE_RESET.
	 * If not forced, init checks if necessary resources are loaded, otherwise
	 * resources are loaded.
	 * 
	 * @param forceReset Force a init by setting forceReset=true
	 * @return a processing log containing status for loading TAK resources
	 */
	public VagvalAgentProcessingLog init(boolean forceReset) {
		if (forceReset || !isInitialized()) {
			logger.info("Initialize VagvalAgent TAK resources...");
			
			//Create a processing log
			VagvalAgentProcessingLog processingLog = new VagvalAgentProcessingLog();
			processingLog.addLog("Initialize VagvalAgent TAK resources...");
			
			List<VirtualiseringsInfoType> v = getVirtualiseringar();
			List<AnropsBehorighetsInfoType> p = getBehorigheter();
			setState(v, p);

			if (isInitialized()) {
			    processingLog.addLog("Succeeded to get virtualizations and/or permissions from TAK, save to local TAK copy...");
				saveToLocalCopy(localTakCache, processingLog);
			} else {
			    processingLog.addLog("Failed to get virtualizations and/or permissions from TAK, see logfiles for details. Restore from local TAK copy...");
				restoreFromLocalCopy(localTakCache, processingLog);
			}

			if (isInitialized()) {
				logger.info("Init VagvalAgent loaded number of permissions: {}", behorighetHandler.size());
				logger.info("Init VagvalAgent loaded number of virtualizations: {}", vagvalHandler.size());
				processingLog.addLog("Init VagvalAgent loaded number of permissions: " + behorighetHandler.size());
				processingLog.addLog("Init VagvalAgent loaded number of virtualizations: " + vagvalHandler.size());
			}
			
			logger.info("Init VagvalAgent done");
			
			return processingLog;
		}
		return null;
	}

	/**
	 * Sets state.
	 * 
	 * @param v
	 *            the virtualization state.
	 * @param p
	 *            the permission state.
	 */
	private synchronized void setState(List<VirtualiseringsInfoType> v, List<AnropsBehorighetsInfoType> p) {
		this.vagvalHandler     = (v == null) ? null : new VagvalHandler(hsaCache, v);
		this.behorighetHandler = (p == null) ? null : new BehorighetHandler(hsaCache, p);
	}

	/**
	 * Return if cache has been initialized.
	 * 
	 * @return true if cache has been initalized, otherwise false.
	 */
	private synchronized boolean isInitialized() {
		return (this.behorighetHandler != null) && (this.vagvalHandler != null);
	}

	private SokVagvalsInfoInterface getPort() {
	    if(port == null){
	        SokVagvalsServiceSoap11LitDocService service = new SokVagvalsServiceSoap11LitDocService(
	                ClientUtil.createEndpointUrlFromServiceAddress(endpointAddressTjanstekatalog));
	        port = service.getSokVagvalsSoap11LitDocPort();
	    }
		return port;
	}
	
	protected void setPort(SokVagvalsInfoInterface port) {
        this.port = port;
    }

	/**
	 * Return virtualizations from TK, or from local cache if TK is unavailable
	 * 
	 * @return virtualizations, or null on any error.
	 */
	protected List<VirtualiseringsInfoType> getVirtualiseringar() {
		List<VirtualiseringsInfoType> l = null;
		try {
			logger.info("Fetch all virtualizations from TAK...");
			HamtaAllaVirtualiseringarResponseType t = getPort().hamtaAllaVirtualiseringar(null);
			l = t.getVirtualiseringsInfo();
		} catch (Exception e) {
			logger.error("Unable to get virtualizations from TAK", e);
		}
		return l;
	}

	/**
	 * Return permissions from TK, or from local cache if TK is unavailable
	 * 
	 * @return permissions, or null on any error.
	 */
	protected List<AnropsBehorighetsInfoType> getBehorigheter() {
		List<AnropsBehorighetsInfoType> l = null;
		try {
			logger.info("Fetch all permissions from TAK...");
			HamtaAllaAnropsBehorigheterResponseType t = getPort().hamtaAllaAnropsBehorigheter(null);
			l = t.getAnropsBehorighetsInfo();
		} catch (Exception e) {
			logger.error("Unable to get permissions from TAK", e);
		}
		return l;
	}

	// restore saved object
	private void restoreFromLocalCopy(String fileName, VagvalAgentProcessingLog processingLog) {
		PersistentCache pc = null;
		InputStream is = null;
		final File file = new File(fileName);
		try {
			if (file.exists()) {
				logger.info("Restore virtualizations and permissions from local TAK copy: {}", fileName);
				is = new FileInputStream(file);
				pc = (PersistentCache) JAXB.unmarshal(is);
				processingLog.addLog("Succesfully restored virtualizations and permissions from local TAK copy: " + fileName);
			}else{
			    logger.error("Failed to find following file containing local TAK copy: {}", fileName);
	            processingLog.addLog("Failed to find following file containing local TAK copy: " + fileName);
			}
		} catch (Exception e) {
			logger.error("Failed to restore virtualizations and permissions from local TAK copy: {}", fileName, e);
			processingLog.addLog("Failed to restore virtualizations and permissions from local TAK copy: " + fileName);
			processingLog.addLog("Reason for failure: " + e.getMessage());
			
			// remove erroneous file.
			if (is != null) {
				file.delete();
			}
		} finally {
			close(is);
		}

		setState((pc == null) ? null : pc.virtualiseringsInfo, (pc == null) ? null : pc.anropsBehorighetsInfo);
	}

	// save object
	private void saveToLocalCopy(String fileName, VagvalAgentProcessingLog processingLog) {
		PersistentCache pc = new PersistentCache();
		pc.anropsBehorighetsInfo = this.behorighetHandler.getAnropsBehorighetsInfoList();
		pc.virtualiseringsInfo = this.vagvalHandler.getVirtualiseringsInfo();

		logger.info("Save virtualizations and permissions to local TAK copy: {}", fileName);
		OutputStream os = null;
		try {
			File file = new File(fileName);
			os = new FileOutputStream(file);
			os.write(JAXB.marshal(pc).getBytes("UTF-8"));
			processingLog.addLog("Succesfully saved virtualizations and permissions to local TAK copy: " + fileName);
		} catch (Exception e) {
			logger.error("Failed to save virtualizations and permissions to local TAK copy: {}" + fileName, e);
			processingLog.addLog("Failed to save virtualizations and permissions to local TAK copy: " + fileName);
			processingLog.addLog("Reason for failure: " + e.getMessage());
		} finally {
			close(os);
		}
	}

	// close resource, ignore errors
	private static void close(Closeable c) {
		if (c != null) {
			try {
				c.close();
			} catch (Exception e) {
			}
		}
	}

	public List<AnropsBehorighetsInfoType> getAnropsBehorighetsInfoList() {

		// Dont force a reset, initialize only if needed
		init(DONT_FORCE_RESET);

		return (behorighetHandler == null) ? null : behorighetHandler.getAnropsBehorighetsInfoList();
	}
	
	protected List<VirtualiseringsInfoType> getVirtualiseringsInfo() {
        return (vagvalHandler == null) ? null: vagvalHandler.getVirtualiseringsInfo();
    }

	/**
	 * Resets cache.
	 */
	public ResetVagvalCacheResponse resetVagvalCache(ResetVagvalCacheRequest parameters) {
		logger.info("Start force a reset of VagvalAgent...");
		
		ResetVagvalCacheResponse response = new ResetVagvalCacheResponse();
				
		//Force reset in init
        VagvalAgentProcessingLog processingLog = init(FORCE_RESET);

		if (!isInitialized()) {
			response.setResetResult(false);
			logger.info("Failed force reset VagvalAgent");
		} else {
			response.setResetResult(true);
			logger.info("Successfully force reset VagvalAgent");
		}
		
		response.getProcessingLog().addAll(processingLog.getLog());
		return response;
	}

	/**
	 * 
	 * @param request
	 *            Receiver, Sender, ServiceName(TjansteKontrakt namespace), Time
	 * @throws VpSemanticException
	 *             if no AnropsBehorighet is found
	 */
	public VisaVagvalResponse visaVagval(VisaVagvalRequest request) {
		if (logger.isDebugEnabled()) {
			logger.debug("entering vagvalAgent visaVagval");
		}
				
		// Dont force a reset, initialize only if needed
		init(DONT_FORCE_RESET);

		if (!isInitialized()) {
			String errorMessage = "VP008 No contact with Tjanstekatalogen at startup, and no local cache to fallback on, not possible to route call";
			logger.error(errorMessage);
			throw new VpSemanticException(errorMessage);
		}

		// Determine if delimiter is set and present in request logical address.
		// Delimiter is used in deprecated default routing (VG#VE).
		boolean useDeprecatedDefaultRouting = addressDelimiter != null && addressDelimiter.length() > 0
				&& request.getReceiverId().contains(addressDelimiter);
		List<String> receiverAddresses = extractReceiverAdresses(request, useDeprecatedDefaultRouting);

		// Get possible routes (vagval)
		VisaVagvalResponse response = vagvalHandler.getRoutingInformation(request, useDeprecatedDefaultRouting, receiverAddresses);

		// No routing was found neither on requested receiver nor using the HSA
		// tree for parents. No need to continue to check authorization.
		if (vagvalHandler.containsNoRouting(response)) {
			return response;
		}

		// Check in TAK if sender is authorized to call the requested
		// receiver,if not check if sender is authorized to call any of the
		// receiver parents using HSA tree. 
		//
		// Note: If old school default routing (VG#VE)HSA tree is used then we only get one address (the first one found routing info for) to check permissions for.
		if (!behorighetHandler.isAuthorized(request, receiverAddresses)) {
			throwNotAuthorizedException(request);
		}

		return response;
	}

	/*
	 * Extract all separate addresses in receiverId if it contains delimiter
	 * character.
	 */
	private List<String> extractReceiverAdresses(VisaVagvalRequest request, boolean useDeprecatedDefaultRouting) {
		List<String> receiverAddresses = new ArrayList<String>();
		if (useDeprecatedDefaultRouting) {
			StringTokenizer strToken = new StringTokenizer(request.getReceiverId(), addressDelimiter);
			while (strToken.hasMoreTokens()) {
				String tempAddress = (String) strToken.nextElement();
				if (!receiverAddresses.contains(tempAddress)) {
					receiverAddresses.add(0, tempAddress);
				}
			}
		} else {
			receiverAddresses.add(request.getReceiverId());
		}
		return receiverAddresses;
	}

	private void throwNotAuthorizedException(VisaVagvalRequest request) {
		String errorMessage = ("VP007 Authorization missing for serviceNamespace: " + request.getTjanstegranssnitt()
				+ ", receiverId: " + request.getReceiverId() + ", senderId: " + request.getSenderId());
		logger.info(errorMessage);
		throw new VpSemanticException(errorMessage);
	}
}