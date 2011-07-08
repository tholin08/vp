package se.skl.tp.vp.pingservice;

import java.util.StringTokenizer;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingServiceResponseTransformer extends AbstractTransformer {

	private static final Logger log = LoggerFactory.getLogger(PingServiceResponseTransformer.class);
	
	/**
	 * Simplest possible transformer that ...
	 */
	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {

		StringTokenizer st = new StringTokenizer((String)src, ",");
		String msgType = st.nextToken().trim();
		String value = st.nextToken().trim();

		String xml = null;
		
		if (msgType.equals("msg-0001-resp")) {
			xml = 
			"<sampleResponse xmlns=\"urn:org.soitoolkit.refapps.sd.sample.schema:v1\">\n" +
			"	<value>" + value + "</value>\n" +
			"</sampleResponse>";

		} else if (msgType.equals("msg-error")) {
			
			String errorMessage = value;
			xml = createSoapFault(errorMessage);
			
		} else {

			String errorMessage = "Unknown message type: " + msgType;
			xml = createSoapFault(errorMessage);

		}

		return xml;
	}

	private String createSoapFault(String errorMessage) {
		return 
		"<soap:Fault xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" + 
		"	<faultcode>soap:Server</faultcode>\n" + 
		"	<faultstring>" + errorMessage + "</faultstring>\n" + 
		"</soap:Fault>";
	}
}