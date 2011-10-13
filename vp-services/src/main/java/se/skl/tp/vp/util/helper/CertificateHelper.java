package se.skl.tp.vp.util.helper;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.exceptions.VpSemanticException;
import se.skl.tp.vp.util.VPUtil;

/**
 * Helper class when dealing with certificates
 * 
 * @author Marcus Krantz [marcus.krantz@callistaenterprise.se]
 */
public class CertificateHelper extends VPHelperSupport {

	private static Logger log = LoggerFactory.getLogger(CertificateHelper.class);
	
	public CertificateHelper(MuleMessage muleMessage) {
		super(muleMessage);
	}
	
	/**
	 * Extract the sender id from a X509 certificate
	 * @param certificate
	 * @param pattern
	 * @return
	 */
	public String extractSenderIdFromCertificate(final X509Certificate certificate, final Pattern pattern) {
		
		log.debug("Extracting sender id from certificate.");
		
		if (certificate == null) {
			throw new NullPointerException("Cannot extract any sender because the certificate was null");
		}
		
		if (pattern == null) {
			throw new NullPointerException("Cannot extract any sender becuase the pattern used to find it was null");
		}
		
		final String principalName = certificate.getSubjectX500Principal().getName();
		final Matcher matcher = pattern.matcher(principalName);
		
		if (matcher.find()) {
			final String senderId = matcher.group(1);
			
			log.debug("Found sender id: {}", senderId);
			return senderId.startsWith("#") ? this.convertFromHexToString(senderId) : senderId;
		} else {
			throw new VpSemanticException("VP002 No senderId found in Certificate: " + principalName);
		}
	}
	
	/**
	 * Extract a X509Certificate
	 * @param reverseProxyMode
	 * @return
	 */
	public X509Certificate extractCertificate() {
		final boolean isReverseProxy = this.isReverseProxy();
		
		log.debug("Extracting X509Certificate. Reverse proxy mode: {}", isReverseProxy);
		
		if (!isReverseProxy) {
			return this.extraxtCertFromChain();
		} else {
			return this.extractCertFromHeader();
		}
	}
	
	/**
	 * Extract the certificate from the cert chain
	 * @return
	 */
	private X509Certificate extraxtCertFromChain() {
		log.debug("Extracting from certificate chain...");
		final Certificate[] certificateChain = (Certificate[]) this.getMuleMessage().getProperty(VPUtil.PEER_CERTIFICATES);
		if (certificateChain != null) {
			try {
				return (X509Certificate) certificateChain[0];
			} catch (final ClassCastException e) {
				throw new VpSemanticException("VP002 No senderId found in Certificate: First certificate in chain is not X509Certificate");
			}
		} else {
			throw new VpSemanticException("VP002 no certificates. The certificate chain was null");
		}
	}
	
	/**
	 * Extract the certificate from the http header
	 * @return
	 */
	private X509Certificate extractCertFromHeader() {
		log.debug("Extracting from http header...");
		/*
		 * Try to get the certificate from
		 * http header
		 */
		try {
			return (X509Certificate) this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME);
		} catch (final ClassCastException e) {
			throw new VpSemanticException("VP002 Exception caught when trying to extract certificate from VP_CERTIFICATE http header. Expected a X509Certificate");
		}
	}
	
	private String convertFromHexToString(final String hexString) {
		byte [] txtInByte = new byte [hexString.length() / 2];
		int j = 0;
		for (int i = 0; i < hexString.length(); i += 2)
		{
			txtInByte[j++] = Byte.parseByte(hexString.substring(i, i + 2), 16);
		}
		return new String(txtInByte);
	}
	
	private boolean isReverseProxy() {
		return this.getMuleMessage().getProperty(VPUtil.REVERSE_PROXY_HEADER_NAME) != null;
	}
}
