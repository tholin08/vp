package se.skl.tp.vp.vagvalrouter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mule.tck.FunctionalTestCase;

import se.skl.tjanst1.wsdl.Product;
import se.skl.tp.vagval.wsdl.v1.VisaVagvalsInterface;
import se.skl.tp.vp.util.VPUtil;
import se.skl.tp.vp.vagvalagent.SokVagvalsInfoMockInput;
import se.skl.tp.vp.vagvalagent.VagvalAgent;
import se.skl.tp.vp.vagvalagent.VagvalMockInputRecord;
import se.skl.tp.vp.vagvalrouter.consumer.VpFullServiceTestConsumer_MuleClient;

public class VpFullServiceWithOriginalConsumerTest extends FunctionalTestCase {

	private static final String NOT_AUHTORIZED_CONSUMER_HSAID = "some-not-authorized-consumer-hsaid";
	private static final String AUHTORIZED_CONSUMER_HSAID = "some-authorized-consumer-hsaid";
	private static final String PRODUCT_ID = "SW123";
	private static final String TJANSTE_ADRESS = "https://localhost:20000/vp/tjanst1";
	
	private static VpFullServiceTestConsumer_MuleClient testConsumer = null;

	public VpFullServiceWithOriginalConsumerTest() {
		super();
		
		setDisposeManagerPerSuite(true);
		
		SokVagvalsInfoMockInput svimi = new SokVagvalsInfoMockInput();
		List<VagvalMockInputRecord> vagvalInputs = new ArrayList<VagvalMockInputRecord>();

		VagvalMockInputRecord vi_ORIGINAL_CONSUMER = new VagvalMockInputRecord();
		vi_ORIGINAL_CONSUMER.receiverId = "vp-test-producer";
		vi_ORIGINAL_CONSUMER.senderId = AUHTORIZED_CONSUMER_HSAID;
		vi_ORIGINAL_CONSUMER.rivVersion = "RIVTABP20";
		vi_ORIGINAL_CONSUMER.serviceNamespace = "urn:skl:tjanst1:rivtabp20";
		vi_ORIGINAL_CONSUMER.adress = "https://localhost:19000/vardgivare-b/tjanst1";

		vagvalInputs.add(vi_ORIGINAL_CONSUMER);
		svimi.setVagvalInputs(vagvalInputs);
	}
	
	@Override
	protected String getConfigResources() {
		return 
			"soitoolkit-mule-jms-connector-activemq-embedded.xml," + 
			"vp-common.xml," +
			"services/VagvalRouter-service.xml," +
			"vp-teststubs-and-services-config.xml";
	}
	
	@Override
	protected void doSetUp() throws Exception {
		if (testConsumer == null) {
			testConsumer = new VpFullServiceTestConsumer_MuleClient(muleContext, "VPConsumerConnector");
		}
	}

	public void testAuthorizedOriginalConsumer() throws Exception {
		
		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(VagvalRouter.X_VP_CONSUMER_ID, AUHTORIZED_CONSUMER_HSAID);

    	Product p = testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, properties);
		assertEquals(PRODUCT_ID, p.getId());
	}
	
	public void testNotAuthorizedOriginalConsumer() throws Exception {
		
 		Map<String, String> properties = new HashMap<String, String>();
    	properties.put(VagvalRouter.X_VP_CONSUMER_ID, NOT_AUHTORIZED_CONSUMER_HSAID);

    	try {
    		testConsumer.callGetProductDetail(PRODUCT_ID, TJANSTE_ADRESS, properties);
    		fail("Expected error here!");
    	} catch (Exception ex) {
    		assertTrue(ex.getMessage().contains("VP007 Authorization missing for serviceNamespace: urn:skl:tjanst1:rivtabp20, receiverId: vp-test-producer, senderId: " + NOT_AUHTORIZED_CONSUMER_HSAID));
    	}
	}
}