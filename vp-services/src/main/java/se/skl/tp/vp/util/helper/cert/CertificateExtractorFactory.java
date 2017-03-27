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
package se.skl.tp.vp.util.helper.cert;

import java.util.regex.Pattern;

import org.mule.api.MuleMessage;
import org.mule.api.transport.PropertyScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vp.util.HttpHeaders;
import se.skl.tp.vp.util.WhiteListHandler;
import se.skl.tp.vp.util.helper.VPHelperSupport;

/**
 * Create a CertificateExtractor based on the mule message.
 */
public class CertificateExtractorFactory extends VPHelperSupport {

	private static Logger log = LoggerFactory.getLogger(CertificateExtractorFactory.class);
	
	private Pattern pattern;
	private WhiteListHandler whiteListHandler;

	public CertificateExtractorFactory(MuleMessage muleMessage, Pattern pattern, WhiteListHandler whiteListHandler) {
		super(muleMessage);
		this.pattern = pattern;
		this.whiteListHandler = whiteListHandler;
	}

	/**
	 * Extract a X509Certificate
	 * 
	 * @param reverseProxyMode
	 * @return
	 */
	public CertificateExtractor createCertificateExtractor() {

		final boolean isReverseProxy = this.isReverseProxy();
		log.debug("Get extractor for X509Certificate. Reverse proxy mode: {}", isReverseProxy);

		if (isReverseProxy) {
			return new CertificateHeaderExtractor(getMuleMessage(), getPattern(), getWhiteListHandler());
		} else {
			return new CertificateChainExtractor(getMuleMessage(), getPattern(), getWhiteListHandler());
		}
	}

	private boolean isReverseProxy() {
		return this.getMuleMessage().getProperty(HttpHeaders.REVERSE_PROXY_HEADER_NAME, PropertyScope.INBOUND) != null;
	}
	
	public Pattern getPattern() {
		return this.pattern;
	}
	
	public WhiteListHandler getWhiteListHandler() {
		return this.whiteListHandler;
	}

}
