package se.skl.tp.vp.pingservice;

import org.mule.api.transformer.TransformerException;
import org.mule.transformer.AbstractTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.soitoolkit.commons.mule.jaxb.JaxbUtil;
import org.soitoolkit.refapps.sd.sample.schema.v1.Sample;

public class PingServiceRequestTransformer extends AbstractTransformer {

	private static final Logger log = LoggerFactory.getLogger(PingServiceRequestTransformer.class);

	private static final JaxbUtil jaxbUtil = new JaxbUtil(Sample.class);

	/**
	 * Simplest possible transformer that ...
	 */
	@Override
	protected Object doTransform(Object src, String encoding) throws TransformerException {

		log.debug("Transforming xml payload: {}", src);
		Sample s = (Sample) jaxbUtil.unmarshal(src);
		
		String csv = "msg-0001-req," + s.getId();

		return csv;
	}
}