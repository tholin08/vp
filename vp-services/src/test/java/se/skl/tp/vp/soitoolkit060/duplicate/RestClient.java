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
package se.skl.tp.vp.soitoolkit060.duplicate;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;

public class RestClient {
	private MuleClient muleClient;
	private String muleConnector;

	public RestClient(MuleContext muleContext, String muleConnector) {
		try {
			muleClient = new MuleClient(muleContext);
			this.muleConnector = muleConnector;
		} catch (MuleException e) {
			throw new RuntimeException(e);
		}
	}

	public RestClient(MuleContext muleContext) {
		try {
			muleClient = new MuleClient(muleContext);
			this.muleConnector = null;
		} catch (MuleException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Perform a HTTP POST call with json request and utf8 charset
	 * 
	 * @param url
	 * @param payload
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpPostRequest_JsonContent(String url, String payload) {    	

		return doHttpSendRequest(url, "POST", payload, "application/json;charset=utf-8");
	}
	
	/**
	 * Perform a HTTP POST call with xml request and utf8 charset
	 * 
	 * @param url
	 * @param payload
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpPostRequest_XmlContent(String url, String payload) {    	

		return doHttpSendRequest(url, "POST", payload, "application/xml;charset=utf-8");
	}

	/**
	 * Perform a HTTP POST call with xml request and utf8 charset
	 * 
	 * @param url
	 * @param payload
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpPostRequest_XmlContent(String url, String payload, Map<String, String> properties) {    	

		return doHttpSendRequest(url, "POST", payload, "application/xml;charset=utf-8", properties);
	}

	
	/**
	 * Perform a HTTP GET call with json response and utf8 charset
	 * 
	 * @param url
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpGetRequest_JsonContent(String url) {    	

		return doHttpReceiveRequest(url, "GET", "application/json", "utf-8");
	}

	/**
	 * Perform a HTTP GET call with xml response and utf8 charset
	 * 
	 * @param url
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpGetRequest_XmlContent(String url) {    	

		return doHttpReceiveRequest(url, "GET", "application/xml", "utf-8");
	}

	/**
	 * Perform a HTTP PUT call with json request and utf8 charset
	 * 
	 * @param url
	 * @param payload
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpPutRequest_JsonContent(String url, String payload) {    	

		return doHttpSendRequest(url, "PUT", payload, "application/json;charset=utf-8");
	}

	/**
	 * Perform a HTTP PUT call with xml request and utf8 charset
	 * 
	 * @param url
	 * @param payload
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpPutRequest_XmlContent(String url, String payload) {    	

		return doHttpSendRequest(url, "PUT", payload, "application/xml;charset=utf-8");
	}

	/**
	 * Perform a HTTP DELETE call with json response and utf8 charset
	 * 
	 * @param url
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpDeleteRequest_JsonContent(String url) {    	

		return doHttpReceiveRequest(url, "DELETE", "application/json", "utf-8");
	}

	/**
	 * Perform a HTTP DELETE call with xml response and utf8 charset
	 * 
	 * @param url
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpDeleteRequest_XmlContent(String url) {    	

		return doHttpReceiveRequest(url, "DELETE", "application/xml", "utf-8");
	}
	
	/**
	 * Perform a HTTP call sending information to the server using POST or PUT
	 * 
	 * @param url
	 * @param method, e.g. "POST" or "PUT"
	 * @param payload
	 * @param contentType
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpSendRequest(String url, String method, String payload, String contentType) {    	

		Map<String, String> properties = new HashMap<String, String>();
    	properties.put("http.method",    method);
    	properties.put("Content-Type",   contentType);

    	MuleMessage response = send(url, payload, properties);
    	
    	return response;
	}
	
	/**
	 * Perform a HTTP call sending information to the server using POST or PUT
	 * 
	 * @param url
	 * @param method, e.g. "POST" or "PUT"
	 * @param payload
	 * @param contentType
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpSendRequest(String url, String method, String payload, String contentType, Map<String, String> inProperties) {    	

		Map<String, String> properties = new HashMap<String, String>();
    	properties.put("http.method",    method);
    	properties.put("Content-Type",   contentType);

    	if (inProperties != null) {
    		properties.putAll(inProperties);
    	}

    	MuleMessage response = send(url, payload, properties);
    	
    	return response;
	}

	/**
	 * Perform a HTTP call receiving information from the server using GET or DELETE
	 * 
	 * @param url
	 * @param method, e.g. "GET" or "DELETE"
	 * @param acceptConentType
	 * @param acceptCharSet
	 * @return
	 * @throws MuleException
	 */
	public MuleMessage doHttpReceiveRequest(String url, String method, String acceptConentType, String acceptCharSet) {    	

		Map<String, String> properties = new HashMap<String, String>();
    	properties.put("http.method",    method);
    	properties.put("Accept",         acceptConentType);
    	properties.put("Accept-Charset", acceptCharSet);

    	MuleMessage response = send(url, null, properties);
    	
    	return response;
	}

	private MuleMessage send(String url, String payload, Map<String, String> properties) {
		if (muleConnector != null) {
			url += "?connector=" + muleConnector;
		}
		try {
			return muleClient.send(url, payload, properties);
		} catch (MuleException e) {
			throw new RuntimeException(e);
		}
	}	
}