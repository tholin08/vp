package se.skl.tp.vp.htmldashboardservice;

import java.net.MalformedURLException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.refapps.sd.sample.schema.v1.Sample;
import org.soitoolkit.refapps.sd.sample.schema.v1.SampleResponse;
import org.soitoolkit.refapps.sd.sample.wsdl.v1.Fault;
import org.soitoolkit.refapps.sd.sample.wsdl.v1.SampleInterface;
import org.soitoolkit.refapps.sd.sample.wsdl.v1.SampleService;

public class HtmlDashboardServiceTestConsumer {

	private static final Logger log = LoggerFactory.getLogger(HtmlDashboardServiceTestConsumer.class);

	public static final String DEFAULT_SERVICE_ADDRESS = null;//getAddressForServletTransport("HTMLDASHBOARDSERVICE_INBOUND_URI");
	public static final int NO_OF_CALLS = 2; // 5
	
	private SampleInterface _service = null;

	public static void main(String[] args) throws Fault {
		String personnummer = "1234567890";
		String serviceAddress = DEFAULT_SERVICE_ADDRESS;
		if (args.length > 0) {
			serviceAddress = args[0];
		}

		executeTestCall(personnummer, serviceAddress);
	}

	private static void executeTestCall(String personnummer, String serviceAddress) throws Fault {
		HtmlDashboardServiceTestConsumer consumer = new HtmlDashboardServiceTestConsumer(serviceAddress);
		System.out.println("Consumer connecting to "  + serviceAddress);
		System.out.println("Warmup call...");
		consumer.callService(personnummer);
		System.out.println("Actual calls...");
		for (int i = 0; i < NO_OF_CALLS; i++) {
			long ts = System.currentTimeMillis();
			SampleResponse response = consumer.callService(personnummer);
			ts = System.currentTimeMillis() - ts;
			System.out.println("Returned value = " + response.getValue() + " ( in " + ts + " ms.)");
		}
	}
	
	public HtmlDashboardServiceTestConsumer(String serviceAddress) {
		_service = new SampleService(
			createEndpointUrlFromServiceAddress(serviceAddress)).getSamplePort();
	}
	
	public SampleResponse callService(String id) throws Fault {
		log.debug("Calling sample-soap-service with id = {}", id);
		Sample request = new Sample();
		request.setId(id);
		return _service.sample(request);
	}

    protected URL createEndpointUrlFromServiceAddress(String serviceAddress) {
        try {
            return new URL(serviceAddress + "?wsdl");
        } catch (MalformedURLException e) {
            throw new RuntimeException("Malformed URL Exception: " + e.getMessage());
        }
    }
}