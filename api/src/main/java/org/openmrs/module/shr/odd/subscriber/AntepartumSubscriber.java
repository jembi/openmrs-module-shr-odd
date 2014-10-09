package org.openmrs.module.shr.odd.subscriber;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.hssf.record.CalcCountRecord;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.api.CdaImportSubscriber;
import org.openmrs.module.shr.odd.OnDemandDocumentConstants;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.generator.document.impl.ApsGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.openmrs.module.shr.odd.util.XdsUtil;

/**
 * Represents a subscriber for Antepartum messages related to an 
 * instance of pregnancy
 */
public class AntepartumSubscriber implements CdaImportSubscriber {

	// Singleton stuff
	private final static Object s_lockObject = new Object();
	private static AntepartumSubscriber s_instance = null;
	
	// Service for import
	protected final OnDemandDocumentService m_service = Context.getService(OnDemandDocumentService.class);
	private final OnDemandDocumentType m_oddType = this.m_service.getOnDemandDocumentTypeByUuid(OnDemandDocumentConstants.ODD_TYPE_APS_UUID);
	private final OnDemandDocumentConfiguration m_configuration = OnDemandDocumentConfiguration.getInstance();
	
	private final int CONCEPT_ID_DATE_OF_CONFINEMENT_EST = 5596;
	
	// Get the log 
	protected final Log log = LogFactory.getLog(this.getClass());

	/**
	 * Ctor for anetpartum subscriber
	 */
	private AntepartumSubscriber() {
	
	}
	
	/**
	 * Get the singleton instance
	 */
	public static final AntepartumSubscriber getInstance() {

		if(s_instance == null)
			synchronized(s_lockObject)
			{
				if(s_instance == null)
					s_instance = new AntepartumSubscriber();
			}
		return s_instance;
	}
	
	/**
	 * Handles the registration of a new APS/APHP document and adds the current encounter to a current pregnancy record if applicable, otherwise
	 * creates a new ODD registration for the pregnancy if this pregnancy is beyond the expected delivery date. 
	 * @see org.openmrs.module.shr.cdahandler.api.CdaImportSubscriber#onDocumentImported(org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument, org.openmrs.Visit)
	 */
	@Override
    public void onDocumentImported(ClinicalDocument rawDocument, Visit processedVisit) {
	
		try
		{
			// Does this document have an estimated delivery date observation?
			OnDemandDocumentRegistration oddRegistration = this.getApplicableOddRegistration(processedVisit);
			
			// Encounters 
			for(Encounter enc : processedVisit.getEncounters())
			{
				boolean duplicate = false;
				for(OnDemandDocumentEncounterLink elnk : oddRegistration.getEncounterLinks())
					duplicate |= elnk.getEncounter().getId().equals(enc.getId());
				if(duplicate) continue; // don't process
				
				OnDemandDocumentEncounterLink elnk = new OnDemandDocumentEncounterLink();
				elnk.setEncounter(enc);
				elnk.setRegistration(oddRegistration);
				oddRegistration.getEncounterLinks().add(elnk);
			}
			
			boolean needsNotification = oddRegistration.getId() == null; // not persisted so we want to notify
			
			// Save the ODD registration
			this.m_service.saveOnDemandDocument(oddRegistration);
	
			// Register the pnr
			if(needsNotification) // Register new only TODO: Update this to support update
				XdsUtil.getInstance().registerDocumentSet(oddRegistration);
		}
		catch (Exception e) {
	        // TODO Auto-generated catch block
	        log.error("Could not register document with registry", e);
        }
			
    }

	/**
	 * Get an applicable ODD registration
	 */
	private OnDemandDocumentRegistration getApplicableOddRegistration(Visit processedVisit) {

		List<OnDemandDocumentRegistration> currentApsRegistrations = this.m_service.getOnDemandDocumentRegistrationsByPatient(processedVisit.getPatient(), this.m_oddType);
		String accessionNumber = String.format("%s.%s.%s.%s", this.m_configuration.getOnDemandDocumentRoot(), this.m_oddType.getId(), processedVisit.getPatient().getId(), currentApsRegistrations.size());
		if(currentApsRegistrations.size() == 0)
		{
			OnDemandDocumentRegistration retVal = new OnDemandDocumentRegistration();
			retVal.setPatient(processedVisit.getPatient());
			retVal.setType(this.m_oddType);
			retVal.setAccessionNumber(accessionNumber);
			retVal.setTitle(String.format("Antepartum Summary episode #%s", currentApsRegistrations.size() + 1));
			return retVal;
		}
		else // Find the appropriate one, whereby the visit time is before the most recent estimated delivery date
		{
			// get the estimated delivery date observation? 
			Set<Obs> confinementObservations = Context.getObsService().getObservations(processedVisit.getPatient(), Context.getConceptService().getConcept(CONCEPT_ID_DATE_OF_CONFINEMENT_EST), false);
			Obs lastConfinementObs = null;
			// Find that last confinement obs with a time before the end of this visit
			for(Obs candidateObs : confinementObservations)
			{
				Calendar lastPossibleConfinementDate = Calendar.getInstance();
				lastPossibleConfinementDate.setTime(candidateObs.getValueDate());
				lastPossibleConfinementDate.add(Calendar.MONTH, 1);
				
				if(processedVisit.getStopDatetime().before(lastPossibleConfinementDate.getTime()) &&
						(lastConfinementObs == null || lastConfinementObs.getValueDatetime().after(candidateObs.getValueDatetime())))
					lastConfinementObs = candidateObs;
			}
			
			// Is the last confinement obs (EDD) within a month of the data assigned here?
			if(lastConfinementObs != null)
			{
				for(OnDemandDocumentRegistration oddRegistration : currentApsRegistrations)
					for(OnDemandDocumentEncounterLink el : oddRegistration.getEncounterLinks())
						if(lastConfinementObs.getEncounter().getId().equals(el.getEncounter().getId()))
						return oddRegistration;

				// The encounter isn't part of a registration so create one
			}
			// TODO: Is confinement a reliable piece of data, will they all have that?
			OnDemandDocumentRegistration retVal = new OnDemandDocumentRegistration();
			retVal.setTitle(String.format("Antepartum Summary episode #%s", currentApsRegistrations.size() + 1));
			retVal.setPatient(processedVisit.getPatient());
			retVal.setType(this.m_oddType);
			retVal.setAccessionNumber(accessionNumber);
			return retVal;
		}
    }
	
}
