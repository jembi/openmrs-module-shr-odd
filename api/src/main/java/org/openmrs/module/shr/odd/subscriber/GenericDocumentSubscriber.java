package org.openmrs.module.shr.odd.subscriber;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Visit;
import org.openmrs.module.shr.cdahandler.api.CdaImportSubscriber;
import org.openmrs.module.shr.odd.OnDemandDocumentConstants;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;

/**
 * Represents a document subscriber that is capable of subscribing to any document
 * and registers a CCD
 */
public class GenericDocumentSubscriber implements CdaImportSubscriber {
	
	private static Object s_lockObject = new Object();
	private static GenericDocumentSubscriber s_instance;
	
	// Meta-data utility
	protected final OddMetadataUtil m_metaDataUtil = OddMetadataUtil.getInstance();
	
	/**
	 * Private ctor for singleton
	 */
    private GenericDocumentSubscriber() {
	    // TODO Auto-generated constructor stub
    }
	
    /**
     * Gets the singleton instance of the import subscriber
     */
    public static GenericDocumentSubscriber getInstance()
    {
    	if(s_instance == null)
    		synchronized (s_lockObject) {
    			if(s_instance == null)
    				s_instance = new GenericDocumentSubscriber();
            }
    	return s_instance;
    }

    /**
     * Handle the on-document imported 
     */
	@Override
	public void onDocumentImported(ClinicalDocument rawDocument, Visit processedVisit) {
		
		// Document was imported, and it was a generic document! 
		// So we'll register an ODD for the patient
		OnDemandDocumentRegistration registration = new OnDemandDocumentRegistration();
		registration.setPatient(processedVisit.getPatient());
		registration.setType(m_metaDataUtil.getOddType(OnDemandDocumentConstants.ODD_TYPE_CCD_UUID));
		
		
	}
	
}
