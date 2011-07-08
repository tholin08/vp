package se.skl.tp.vp.htmldashboardservice;

import static se.skl.tp.vp.VpMuleServer.MULE_SERVER_ID;
import static se.skl.tp.vp.htmldashboardservice.HtmlDashboardServiceTestConsumer.DEFAULT_SERVICE_ADDRESS;
import static se.skl.tp.vp.htmldashboardservice.HtmlDashboardServiceTestProducer.TEST_ID_FAULT_INVALID_ID;
import static se.skl.tp.vp.htmldashboardservice.HtmlDashboardServiceTestProducer.TEST_ID_FAULT_TIMEOUT;
import static se.skl.tp.vp.htmldashboardservice.HtmlDashboardServiceTestProducer.TEST_ID_OK;

import java.util.ResourceBundle;

import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.test.AbstractJmsTestUtil;
import org.soitoolkit.commons.mule.test.AbstractTestCaseWithServletEngine;
import org.soitoolkit.commons.mule.test.ActiveMqJmsTestUtil;
import org.soitoolkit.refapps.sd.sample.schema.v1.SampleResponse;
import org.soitoolkit.refapps.sd.sample.wsdl.v1.Fault;

public class HtmlDashboardServiceIntegrationTest extends AbstractTestCaseWithServletEngine {
	
	private static final Logger log = LoggerFactory.getLogger(HtmlDashboardServiceIntegrationTest.class);
	private static final ResourceBundle rb = ResourceBundle.getBundle("vp-config");

	private static final long   SERVICE_TIMOUT_MS = Long.parseLong(rb.getString("SERVICE_TIMEOUT_MS"));
	private static final String EXPECTED_ERR_TIMEOUT_MSG = "Response timed out (" + SERVICE_TIMOUT_MS + "ms) waiting for message response id ";


	private static final String REQUEST_QUEUE   = rb.getString("HTMLDASHBOARDSERVICE_REQUEST_QUEUE");
	private static final String RESPONSE_QUEUE  = rb.getString("HTMLDASHBOARDSERVICE_RESPONSE_QUEUE");
 
	private static final String ERROR_LOG_QUEUE = "SOITOOLKIT.LOG.ERROR";
	private AbstractJmsTestUtil jmsUtil = null;
	
    public HtmlDashboardServiceIntegrationTest() {
    	
    	// Initialize servlet engine in baseclass with proper values
    	//super(MULE_SERVER_ID, HTTP_PORT, CONTEXT_PATH, MULE_SERVLET_URI);
    	super(MULE_SERVER_ID, 0, "", "");
    	
    	// TODO: Should this be placed in the baseclass?
    	
    	// Only start up Mule once to make the tests run faster...
    	// Set to false if tests interfere with each other when Mule is started only once.
        setDisposeManagerPerSuite(true);

		// Increase the default 120 sec so that you have a chance to debug...
        setTestTimeoutSecs(120);
    }

	protected String getConfigResources() {
		return "soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
  
		"vp-common.xml," +
		"services/htmlDashboardService-service.xml," +
		"teststub-services/htmlDashboardService-teststub-service.xml";
    }

    @Override
	protected void doSetUp() throws Exception {
		super.doSetUp();

		doSetUpJms();
    }

	private void doSetUpJms() {
		// TODO: Fix lazy init of JMS connection et al so that we can create jmsutil in the declaration
		// (The embedded ActiveMQ queue manager is not yet started by Mule when jmsutil is delcared...)
		if (jmsUtil == null) jmsUtil = new ActiveMqJmsTestUtil();
		

		// Clear queues used for the outbound endpoint
		jmsUtil.clearQueues(REQUEST_QUEUE);
		jmsUtil.clearQueues(RESPONSE_QUEUE);
 
		// Clear queues used for error handling
		jmsUtil.clearQueues(ERROR_LOG_QUEUE);
    }

    public void test_ok() throws org.soitoolkit.refapps.sd.sample.wsdl.v1.Fault {
    	String id = TEST_ID_OK;
    	HtmlDashboardServiceTestConsumer consumer = new HtmlDashboardServiceTestConsumer(DEFAULT_SERVICE_ADDRESS);
		SampleResponse response = consumer.callService(id);
		assertEquals("Value" + id,  response.getValue());
	}

	public void test_fault_invalidInput() throws Fault {
		try {
	    	String id = TEST_ID_FAULT_INVALID_ID;
	    	HtmlDashboardServiceTestConsumer consumer = new HtmlDashboardServiceTestConsumer(DEFAULT_SERVICE_ADDRESS);
			consumer.callService(id);
	        fail("expected fault");
	    } catch (SOAPFaultException e) {
	    	assertEquals("Invalid Id: " + TEST_ID_FAULT_INVALID_ID, e.getMessage());
	    }
	}

	public void test_fault_timeout() throws Fault {
        try {
	    	String id = TEST_ID_FAULT_TIMEOUT;
	    	HtmlDashboardServiceTestConsumer consumer = new HtmlDashboardServiceTestConsumer(DEFAULT_SERVICE_ADDRESS);
			consumer.callService(id);
	        fail("expected fault");
        } catch (SOAPFaultException e) {
            assertTrue("Unexpected error message: " + e.getMessage(), e.getMessage().startsWith(EXPECTED_ERR_TIMEOUT_MSG));
        }
    }
}
