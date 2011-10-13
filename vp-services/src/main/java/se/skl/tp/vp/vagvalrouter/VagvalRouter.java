/**
 * Copyright 2009 Sjukvardsradgivningen
 *
 *   This library is free software; you can redistribute it and/or modify
 *   it under the terms of version 2.1 of the GNU Lesser General Public

 *   License as published by the Free Software Foundation.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the

 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the
 *   Free Software Foundation, Inc., 59 Temple Place, Suite 330,

 *   Boston, MA 02111-1307  USA
 */
package se.skl.tp.vp.vagvalrouter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;

import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.endpoint.EndpointBuilder;
import org.mule.api.endpoint.EndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.routing.CouldNotRouteOutboundMessageException;
import org.mule.api.routing.RoutingException;
import org.mule.endpoint.EndpointURIEndpointBuilder;
import org.mule.endpoint.URIBuilder;
import org.mule.routing.outbound.AbstractRecipientList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.dashboard.ServiceStatistics;
import se.skl.tp.vp.exceptions.VpTechnicalException;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.util.helper.AddressingHelper;
import se.skl.tp.vp.util.helper.PayloadHelper;

public class VagvalRouter extends AbstractRecipientList {

	private static final Logger logger = LoggerFactory.getLogger(VagvalRouter.class);

	private VisaVagvalsInterface vagvalAgent;

	private Pattern pattern;

	private String senderIdPropertyName;
	
	private String responseTimeout;

	private Map<String, ServiceStatistics> statistics = new HashMap<String, ServiceStatistics>();
	
	private static final String IS_HTTPS = "isHttps";
	
	public void setResponseTimeout(final String responseTimeout) {
		this.responseTimeout = responseTimeout;
	}

	public void setSenderIdPropertyName(String senderIdPropertyName) {
		this.senderIdPropertyName = senderIdPropertyName;
		pattern = Pattern.compile(this.senderIdPropertyName + "=([^,]+)");
		if (logger.isInfoEnabled()) {
			logger.info("senderIdPropertyName set to: " + senderIdPropertyName);
		}
	}

	// Not private to make the method testable...
	public void setVagvalAgent(VisaVagvalsInterface vagvalAgent) {
		this.vagvalAgent = vagvalAgent;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List getRecipients(MuleMessage message) throws CouldNotRouteOutboundMessageException {
		final AddressingHelper addrHelper = new AddressingHelper(message, vagvalAgent, pattern);
		return Collections.singletonList(addrHelper.getAddress());
	}

	/**
	 * Override this method to be able to collect statistics per
	 * Contract/Service Producer
	 */
	@Override
	public MuleMessage route(MuleMessage message, MuleSession session) throws RoutingException {
		
		final PayloadHelper routerHelper = new PayloadHelper(message);
		
		final String receiverId = routerHelper.extractReceiverFromPayload();
		message.setProperty(VPUtil.RECEIVER_ID, receiverId);

		long beforeCall = System.currentTimeMillis();
		String serviceId = VPUtil.extractNamespaceFromService((QName) message.getProperty(VPUtil.SERVICE_NAMESPACE)) + "-"
				+ message.getProperty(VPUtil.RECEIVER_ID);

		synchronized (statistics) {

			if (statistics.keySet().size() == 0) {
				try {
					muleContext.getRegistry().registerObject("tp-statistics", statistics);
				} catch (RegistrationException e) {
					logger.error("Stats not possible to register" + e);
					// Dont let this interfere with the actual processing of
					// messages
				}
			}

			ServiceStatistics serverStatistics = statistics.get(serviceId);
			if (serverStatistics == null) {
				serverStatistics = new ServiceStatistics();
				serverStatistics.producerId = serviceId;
				statistics.put(serviceId, serverStatistics);
			}
			serverStatistics.noOfCalls++;
		}

		// Do the actual routing
		MuleMessage replyMessage = super.route(message, session);
		
		/*
		 * Restore properties
		 */
		for (final Object prop: message.getPropertyNames()) {
			replyMessage.setProperty((String) prop, message.getProperty((String) prop));
		}
		
		synchronized (statistics) {
			ServiceStatistics serverStatistics = statistics.get(serviceId);
			serverStatistics.noOfSuccesfullCalls++;
			long duration = System.currentTimeMillis() - beforeCall;
			serverStatistics.totalDuration += duration;
			serverStatistics.averageDuration = serverStatistics.totalDuration
					/ serverStatistics.noOfSuccesfullCalls;
		}
		
		return replyMessage;
	}

	@Override
	protected OutboundEndpoint getRecipientEndpoint(MuleMessage message, Object recipient)
			throws RoutingException {

		EndpointBuilder eb = new EndpointURIEndpointBuilder(new URIBuilder((String) recipient),
				muleContext);
		eb.setSynchronous(true);
		eb.setResponseTimeout(Integer.valueOf(this.responseTimeout));
		
		HashMap<String, String> properties = new HashMap<String, String>();
		properties.put("proxy", "true");
		properties.put("payload", "envelope");
		if (message.getBooleanProperty(IS_HTTPS, false)) {
			properties.put("protocolConnector", VPUtil.CONSUMER_CONNECTOR_NAME);
			logger.debug("Https protocolConnector set");
		}
		eb.setProperties(properties);

		try {
			return eb.buildOutboundEndpoint();
		} catch (InitialisationException e) {
			throw new VpTechnicalException(e);
		} catch (EndpointException e) {
			throw new VpTechnicalException(e);
		}
	}
}
