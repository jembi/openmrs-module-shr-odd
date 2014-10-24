package org.openmrs.module.shr.odd.generator.document.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedCustodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component3;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Custodian;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.DocumentationOf;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer1;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ServiceEvent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.BindingRealm;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationFunction;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_BasicConfidentialityKind;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ServiceEventPerformer;
import org.openmrs.EncounterRole;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.generator.DocumentGenerator;
import org.openmrs.module.shr.odd.generator.SectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorFactory;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.util.CdaDataUtil;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;

/**
 * Represents an abstract implementation of a CdaGenerator 
 */
public abstract class DocumentGeneratorImpl implements DocumentGenerator {

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
		retVal.setTypeId(new II("2.16.840.1.113883.1.3", "POCD_HD000040"));
		retVal.setRealmCode(SET.createSET(new CS<BindingRealm>(BindingRealm.UniversalRealmOrContextUsedInEveryInstance)));
		
		// Identifier is the SHR root of the odd document ODD ID + Current Time (making the UUID of the ODD)
		TS idDate = TS.now();
		idDate.setDateValuePrecision(TS.SECONDNOTIMEZONE);
		String oddRoot = String.format("%s.%s", this.m_configuration.getOnDemandDocumentRoot(), oddRegistration.getType().getId()),
				oddExtension = String.format("%s-%s", oddRegistration.getPatient().getId(), idDate.toString());
		log.info(String.format("Preparing document %s^^^&%s&ISO", oddExtension, oddRoot));
		
		// Set core properties
		retVal.setId(oddRoot, oddExtension);
		retVal.setEffectiveTime(TS.now());
		
		// Set to Normal, anything above a normal will not be included in the extract
		retVal.setConfidentialityCode(new CE<x_BasicConfidentialityKind>(x_BasicConfidentialityKind.Normal));
		retVal.setLanguageCode(Context.getLocale().toLanguageTag()); // CONF-5
		
		// Custodian
		Custodian custodian = new Custodian();
		custodian.setAssignedCustodian(new AssignedCustodian());
		custodian.getAssignedCustodian().setRepresentedCustodianOrganization(this.m_cdaDatatypeUtil.getCustodianOrganization());
		retVal.setCustodian(custodian);

		// Add an author as this SHR device
		retVal.getAuthor().add(this.m_cdaDatatypeUtil.getOpenSHRInstanceAuthor());
		
		// Create documentation of
		// TODO: Do we only need one of these for all events that occur in the CDA or one for each?
		ServiceEvent event = new ServiceEvent(new CS<String>("PCPR")); // CCD CONF-3 & CONF-2
		Date earliestRecord = new Date(),
				lastRecord = new Date(0);
		
		// Assign data form the encounters
		for(OnDemandDocumentEncounterLink elnk : oddRegistration.getEncounterLinks())
		{
			if(elnk.getEncounter().getVisit().getStartDatetime().before(earliestRecord))
				earliestRecord = elnk.getEncounter().getVisit().getStartDatetime();
			if(elnk.getEncounter().getVisit().getStopDatetime().after(lastRecord))
				lastRecord = elnk.getEncounter().getVisit().getStopDatetime();
			
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
						retVal.getAuthor().add(aut);
					}
				else if(encounterProvider.getKey().getName().equals("LA")) // There technically are no "legal" attesters to the document here as it is an auto-generated document
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
		event.setEffectiveTime(new TS(earliestCal), new TS(latestCal)); // CCD CONF-4
		
		// Documentation of
		retVal.getDocumentationOf().add(new DocumentationOf(event));
		
		// Record target
		retVal.getRecordTarget().add(this.m_cdaDatatypeUtil.createRecordTarget(oddRegistration.getPatient()));
		
		// NOK (those within the time covered by this document)
		for(Relationship relatedPerson : Context.getPersonService().getRelationshipsByPerson(oddRegistration.getPatient()))
		{
			// Periodic hull
			if((relatedPerson.getEndDate() == null || earliestRecord.before(relatedPerson.getEndDate()) || earliestRecord.equals(relatedPerson.getEndDate())) &&
					(relatedPerson.getStartDate() == null || lastRecord.after(relatedPerson.getStartDate()) || lastRecord.equals(relatedPerson.getStartDate())))
					retVal.getParticipant().add(this.m_cdaDatatypeUtil.createRelatedPerson(relatedPerson, oddRegistration.getPatient()));
		}
		
		return retVal;
	}

	/**
	 * Add sections to the document
	 */
	public List<Component3> generateSections(OnDemandDocumentRegistration documentRegistration, ClinicalDocument generatedDocument, Class<? extends SectionGenerator>... generatorClazzes) {
		

		// Iterate through the generator classes and generate / add the section
		for(Class<? extends SectionGenerator> clazz : generatorClazzes)
		{
			SectionGenerator generator = SectionGeneratorFactory.createInstance(clazz);
			if(generator == null) continue; // cannot create this generator
			
			// Now generate the section
			generator.setRegistration(documentRegistration);
			generator.setGeneratedDocument(generatedDocument);
			Section generatedSection = generator.generateSection();

			if(generatedSection != null)
				generatedDocument.getComponent().getBodyChoiceIfStructuredBody().getComponent().add(
					new Component3(ActRelationshipHasComponent.HasComponent, BL.TRUE, generatedSection)
						);
		}

		return generatedDocument.getComponent().getBodyChoiceIfStructuredBody().getComponent();
    }
	
}
