package org.openmrs.module.shr.odd.subscriber;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Encounter;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.api.CdaImportSubscriber;
import org.openmrs.module.shr.odd.OnDemandDocumentConstants;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;
import org.openmrs.module.shr.odd.util.XdsUtil;

/**
 * Represents a document subscriber that is capable of subscribing to any document
 * and registers a CCD
 */
public class GenericDocumentSubscriber implements CdaImportSubscriber {
		
	// Singleton stuff
	private static Object s_lockObject = new Object();
	private static GenericDocumentSubscriber s_instance;
	
	// Meta-data utilities & configuration
	protected final OddMetadataUtil m_metaDataUtil = OddMetadataUtil.getInstance();
	protected final OnDemandDocumentConfiguration m_configuration = OnDemandDocumentConfiguration.getInstance();
	
	// ODD service
	protected final OnDemandDocumentService m_oddService = Context.getService(OnDemandDocumentService.class);
	
	// Get the log 
	protected final Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Private ctor for singleton
	 */
    private GenericDocumentSubscriber() {
	    // TODO Auto-generated constructor stub
    }
	
    /**
     * Gets the singleton instance of the import subscriber
     */
    public final static GenericDocumentSubscriber getInstance()
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

		try
		{
			// Get the document type
			OnDemandDocumentType documentType = this.m_metaDataUtil.getOddType(OnDemandDocumentConstants.ODD_TYPE_CCD_UUID);
			if(documentType == null)
				throw new OnDemandDocumentException("CCD Document Type not registered");
			
			// Generate the accession number
			String accessionNumber = String.format("%s.%s.%s", this.m_configuration.getOnDemandDocumentRoot(), documentType.getId(), processedVisit.getPatient().getId());
			log.info(String.format("Storing document with AN %s", accessionNumber));
			
			// Find already registered
			List<OnDemandDocumentRegistration> existingDocs = this.m_oddService.getOnDemandDocumentRegistrationsByAccessionNumber(accessionNumber);
			OnDemandDocumentRegistration registration = null;
			if(existingDocs.size() == 1) // update
			{
				registration = existingDocs.get(0);
			}
			else
			{
				registration = new OnDemandDocumentRegistration();
				registration.setPatient(processedVisit.getPatient());
				registration.setAccessionNumber(accessionNumber);
				registration.setType(documentType);
				registration.setTitle("Continuity of Care Document");
			}
			
			// Add the ODD encounter link!
			for(Encounter enc : processedVisit.getEncounters())
			{
				boolean duplicate = false;
				for(OnDemandDocumentEncounterLink elnk : registration.getEncounterLinks())
					duplicate |= elnk.getEncounter().getId().equals(enc.getId());
				if(duplicate) continue; // don't process
				
				OnDemandDocumentEncounterLink elnk = new OnDemandDocumentEncounterLink();
				elnk.setEncounter(enc);
				elnk.setRegistration(registration);
				registration.getEncounterLinks().add(elnk);
			}
			
			// Save the ODD registration
			this.m_oddService.saveOnDemandDocument(registration);
	
			// Register the pnr
			if(existingDocs.size() == 0) // Register new only TODO: Update this to support update
				XdsUtil.getInstance().registerDocumentSet(registration);
        }
        catch (Exception e) {
	        // TODO Auto-generated catch block
	        log.error("Could not register document with registry", e);
        }

	}
	
}
