package org.openmrs.module.shr.odd.generator.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedCustodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Authenticator;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Custodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.DocumentationOf;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer1;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.RelatedDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ServiceEvent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationFunction;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipDocument;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_BasicConfidentialityKind;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ServiceEventPerformer;
import org.openmrs.EncounterRole;
import org.openmrs.Provider;
import org.openmrs.VisitAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsDataUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsMetadataUtil;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.generator.CdaGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.util.CdaDataUtil;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;

/**
 * Represents an abstract implementation of a CdaGenerator 
 */
public abstract class CdaGeneratorImpl implements CdaGenerator {

	// odd configuration
	protected final OnDemandDocumentConfiguration m_configuration = OnDemandDocumentConfiguration.getInstance();
	protected final OddMetadataUtil m_oddMetadataUtil = OddMetadataUtil.getInstance();
	protected final CdaDataUtil m_cdaDatatypeUtil = CdaDataUtil.getInstance();
	
	protected final Log log = LogFactory.getLog(this.getClass());
	
	/**
	 * Create the common header elements for the clinical document
	 */
	protected ClinicalDocument createHeader(OnDemandDocumentRegistration oddRegistration)
	{

		ClinicalDocument retVal = new ClinicalDocument();
		
		// Identifier is the SHR root of the odd document ODD ID + Current Time (making the UUID of the ODD)
		String oddRoot = String.format("%s.%s", this.m_configuration.getOnDemandDocumentRoot(), oddRegistration.getType().getId()),
				oddExtension = String.format("%s-%s", oddRegistration.getPatient().getId(), TS.now().toString());
		log.info(String.format("Preparing document %s^^^&%s&ISO", oddExtension, oddRoot));
		
		// Set core properties
		retVal.setId(oddRoot, oddExtension);
		retVal.setEffectiveTime(TS.now());
		
		// Set to Normal, anything above a normal will not be included in the extract
		retVal.setConfidentialityCode(new CE<x_BasicConfidentialityKind>(x_BasicConfidentialityKind.Normal));
		retVal.setLanguageCode(Context.getLocale().toString());
		
		// Custodian
		Custodian custodian = new Custodian();
		custodian.setAssignedCustodian(new AssignedCustodian());
		custodian.getAssignedCustodian().setRepresentedCustodianOrganization(this.m_cdaDatatypeUtil.getCustodianOrganization());
		retVal.setCustodian(custodian);

		// Add an author as this SHR device
		retVal.getAuthor().add(this.m_cdaDatatypeUtil.getOpenSHRInstanceAuthor());
		
		// Create documentation of
		// TODO: Do we only need one of these for all events that occur in the CDA or one for each?
		ServiceEvent event = new ServiceEvent();
		Date earliestRecord = new Date(),
				lastRecord = new Date(0);
		
		for(OnDemandDocumentEncounterLink elnk : oddRegistration.getEncounterLinks())
		{
			if(elnk.getEncounter().getEncounterDatetime().before(earliestRecord))
				earliestRecord = elnk.getEncounter().getEncounterDatetime();
			if(elnk.getEncounter().getEncounterDatetime().after(lastRecord))
				lastRecord = elnk.getEncounter().getEncounterDatetime();
			
			
			// Now add participants
			for(Entry<EncounterRole, Set<Provider>> encounterProvider : elnk.getEncounter().getProvidersByRoles().entrySet())
			{
				
				if(encounterProvider.getKey().getName().equals("AUT"))
					for(Provider pvdr : encounterProvider.getValue())
					{
						Author aut = new Author(ContextControl.OverridingPropagating);
						aut.setTime(new TS());
						aut.getTime().setNullFlavor(NullFlavor.NoInformation);
						aut.setAssignedAuthor(this.m_cdaDatatypeUtil.createAuthorPerson(pvdr));
					}
				else if(encounterProvider.getKey().getName().equals("LA"))
					;
				else
					for(Provider pvdr : encounterProvider.getValue())
					{
						Performer1 performer = new Performer1(x_ServiceEventPerformer.PRF, this.m_cdaDatatypeUtil.createAssignedEntity(pvdr));
						performer.setFunctionCode((CE<ParticipationFunction>)this.m_cdaDatatypeUtil.parseCodeFromString(encounterProvider.getKey().getDescription(), CE.class));
						event.getPerformer().add(performer);
					}
			}

		}
		
		// Set the effective time of records
		Calendar earliestCal = Calendar.getInstance(),
				latestCal = Calendar.getInstance();
		earliestCal.setTime(earliestRecord);
		latestCal.setTime(lastRecord);
		event.setEffectiveTime(new TS(earliestCal), new TS(latestCal));
		
		// Documentation of
		retVal.getDocumentationOf().add(new DocumentationOf(event));
		
		return retVal;
	}
	
}
