package org.openmrs.module.shr.odd.contenthandler;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.NotFoundException;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Encounter;
import org.openmrs.EncounterRole;
import org.openmrs.EncounterType;
import org.openmrs.Patient;
import org.openmrs.Provider;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.contenthandler.CdaContentHandler;
import org.openmrs.module.shr.cdahandler.everest.EverestUtil;
import org.openmrs.module.shr.contenthandler.api.Content;
import org.openmrs.module.shr.contenthandler.api.ContentHandler;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * The On-Demand-Documents content handler 
 */
public class OnDemandDocumentContentHandler implements ContentHandler {
	
	// Log for this processor
	protected final Log log = LogFactory.getLog(this.getClass());

	// The lock object
	private static final Object s_lockObject = new Object();
	// The singleton instance
	private static OnDemandDocumentContentHandler s_instance = null;
	
	/**
	 * Get the singleton instance of the content handler
	 */
	public static OnDemandDocumentContentHandler getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new OnDemandDocumentContentHandler();
            }
		return s_instance;
	}
	
	/**
	 * Clone the handler
	 * @see org.openmrs.module.shr.contenthandler.api.ContentHandler#cloneHandler()
	 */
	@Override
	public ContentHandler cloneHandler() {
		return OnDemandDocumentContentHandler.getInstance();
	}
	
	/**
	 * Fetch content
	 * @see org.openmrs.module.shr.contenthandler.api.ContentHandler#fetchContent(java.lang.String)
	 */
	@Override
	public Content fetchContent(String arg0) {
		OnDemandDocumentService oddService = Context.getService(OnDemandDocumentService.class);
		
		// Get the ODD registration
		List<OnDemandDocumentRegistration> registrations = oddService.getOnDemandDocumentRegistrationsByAccessionNumber(arg0);
		if(registrations.size() == 0)
			throw new OnDemandDocumentException(String.format("On-Demand Document with id %s not found", arg0));
		else
		{
			try {
	            ClinicalDocument generatedCda = oddService.generateOnDemandDocument(registrations.get(0));
	            
	            // Create the formatter and format to a stream
	            XmlIts1Formatter fmtr = EverestUtil.createFormatter();
	            ByteArrayOutputStream bos = new ByteArrayOutputStream();
	            fmtr.graph(bos, generatedCda);
	            String strPayload = new String(bos.toByteArray());
	            
	            // Now set the return content
	            Content retVal = new Content(arg0, strPayload, null, null, null);
	            return retVal;
            }
            catch (Exception e) {
            	throw new OnDemandDocumentException(e.getMessage(), e);
            }
		}
	}
	
	/**
	 * Save content (not applicable)
	 * @see org.openmrs.module.shr.contenthandler.api.ContentHandler#saveContent(org.openmrs.Patient, java.util.Map, org.openmrs.EncounterType, org.openmrs.module.shr.contenthandler.api.Content)
	 */
	@Override
	public Encounter saveContent(Patient arg0, Map<EncounterRole, Set<Provider>> arg1, EncounterType arg2, Content arg3) {
		throw new NotImplementedException();
	}
	
}
