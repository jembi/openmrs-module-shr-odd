package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.PN;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Participant2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ParticipantRole;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.PlayingEntity;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.EntityClassRoot;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationType;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.openmrs.*;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;

/**
 * A generator for the allergies or alerts section
 */
public class AllergiesSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("48765-2", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Allergies, adverse reactions, alerts", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public AllergiesSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode, null);
	}
	
	@Override
	public Section generateSection() {
		// Are there problems on file for the patient?
		List<Allergy> allergies = Context.getPatientService().getAllergies(this.m_registration.getPatient());

		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_ALERTS, CdaHandlerConstants.SCT_TEMPLATE_ALLERGIES), 
			"section.allergy.title",
			this.m_sectionCode
		);

		// We MUST assert an allergy to latex
		List<Concept> requiredAllergyAssertions = Arrays.asList(
			Context.getConceptService().getConcept(116367)
		);
		
		// Problem section must have level 3 content
		if(allergies.size() > 0)
		{

			
			// Generate problem list
			for(Allergy allergy : allergies)
			{
				Act problemAct = super.createAct(
					x_ActClassDocumentEntryAct.Act,
					x_DocumentActMood.Eventoccurrence,
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
					allergy);
			
				requiredAllergyAssertions.remove(allergy.getAllergen());

				// Add an entry relationship of the problem
				Obs problemObs = allergy.getStartObs();
				if(allergy.getStopObs() != null)
					problemObs = allergy.getStopObs();
				
				Observation problemObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_ALERT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ALLERGY_AND_INTOLERANCE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION), 
					problemObs, 
					CdaHandlerConstants.CODE_SYSTEM_SNOMED);
				
				super.correctCode((CD<?>)problemObservation.getValue(), CdaHandlerConstants.CODE_SYSTEM_RXNORM, CdaHandlerConstants.CODE_SYSTEM_SNOMED, CdaHandlerConstants.CODE_SYSTEM_ICD_10);
				
				// Now for allergy information
				String typeMnemonic = "", display = "";
				switch(allergy.getAllergenType())
				{
					case DRUG:
						typeMnemonic = "D";
						display = "Drug ";
						break;
					case ENVIRONMENT:
						typeMnemonic = "E";
						display = "Environmental ";
						break;
					case FOOD:
						typeMnemonic = "F";
						display = "Food ";
						break;
					default:
						typeMnemonic = "O";
						display = "Other ";
				}
				//TODO update, check what with AllergySeverity.INTOLERANCE
				// Complete the code and assign
				if(allergy.getSeverity().equals(AllergySeverity.INTOLERANCE))
				{
					typeMnemonic += "INT";
					display += "Intolerance";
				}
				else if(typeMnemonic.equals("O"))
				{
					typeMnemonic = "ALG";
					display += "Allergy";
				}
				else
				{
					typeMnemonic += "ALG";
					display += "Allergy";
				}
				problemObservation.setCode(new CD<String>(typeMnemonic, CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ObservationIntoleranceType", null, display, null));
				
				// The agent.. 
				problemObservation.getParticipant().add(new Participant2(ParticipationType.Consumable, ContextControl.OverridingPropagating));
				problemObservation.getParticipant().get(0).setParticipantRole(new ParticipantRole(new CS<String>("MANU")));
				PlayingEntity playingEntity = new PlayingEntity(EntityClassRoot.ManufacturedMaterial);
				
				playingEntity.setCode(this.m_oddMetadataUtil.getStandardizedCode(allergy.getAllergen().getCodedAllergen(), null, CE.class));
				super.correctCode(playingEntity.getCode(), CdaHandlerConstants.CODE_SYSTEM_RXNORM, CdaHandlerConstants.CODE_SYSTEM_SNOMED, CdaHandlerConstants.CODE_SYSTEM_ICD_10);

				playingEntity.setName(SET.createSET(new PN(Arrays.asList(new ENXP(allergy.getAllergen().toString())))));
				problemObservation.getParticipant().get(0).getParticipantRole().setPlayingEntityChoice(playingEntity);
				
				
				// Now the severity
				Concept severityConcept = allergy.getSeverity();
				String uuidSeverity = severityConcept.getUuid();
				String severityCode = null;
				if(uuidSeverity.equals(getGlobalProperty("allergy.concept.severity.mild")))
					severityCode = "L";
				else if(uuidSeverity.equals(getGlobalProperty("allergy.concept.severity.moderate")))
					severityCode = "M";
				else if(uuidSeverity.equals(getGlobalProperty("allergy.concept.severity.severe")))
					severityCode = "H";

				// Severity code
				if(severityCode != null)
				{
					Observation severityObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
					
					severityObservation.setId(SET.createSET(new II(UUID.randomUUID())));
					severityObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_SEVERITY_OBSERVATION), new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_SEVERITY_OBSERVATION)));
					severityObservation.setCode(new CD<String>("SEV", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ActCode", null, "Severity", null));
					severityObservation.setText(new ED(allergy.getSeverity().getName().getName()));
					severityObservation.setStatusCode(ActStatus.Completed);
					severityObservation.setValue(new CD<String>(severityCode, CdaHandlerConstants.CODE_SYSTEM_OBSERVATION_VALUE, "ObservationValue", null, null, allergy.getSeverity().getName().getName()));
					problemObservation.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, BL.TRUE, null, null, null, severityObservation));
				}
				
				// Now the manifestations (these are sub-observations)
				try {
	                for(Obs manifestationObs : this.m_service.getObsGroupMembers(problemObs, Arrays.asList(super.m_conceptUtil.getOrCreateConcept(new CV<String>("PROBLEM", CdaHandlerConstants.CODE_SYSTEM_IHE_ACT_CODE)))))
	                {
	                	ExtendedObs eobs = Context.getService(CdaImportService.class).getExtendedObs(manifestationObs.getId());
	                	if(eobs == null) continue;
	                	
	                	EntryRelationship manifestation = new EntryRelationship(x_ActRelationshipEntryRelationship.MFST, BL.TRUE);
	                	manifestation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_MANIFESTATION_RELATION)));
	                	Observation manifestationObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_REACTION_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION), eobs, CdaHandlerConstants.CODE_SYSTEM_IHE_ACT_CODE);
	                	if(manifestationObservation.getValue() instanceof CE)
	                		super.correctCode((CE)manifestationObservation.getValue(), CdaHandlerConstants.CODE_SYSTEM_ICD_10, CdaHandlerConstants.CODE_SYSTEM_SNOMED);

	                	manifestation.setClinicalStatement(manifestationObservation);
	                	problemObservation.getEntryRelationship().add(manifestation);
	                }
                }
                catch (DocumentImportException e) {
	                // TODO Auto-generated catch block
	                log.error("Error generated", e);
                }
				problemAct.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.FALSE, BL.TRUE, null, null, null, problemObservation));
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, problemAct));
				
			}
			
		}
		else if(this.getSectionObs().size() > 0) // unstructured?
		{
			requiredAllergyAssertions.clear();
			super.generateLevel2Content(retVal);
		}
		else
		{
			
			Act problemAct = super.createNoKnownProblemAct(
				Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
				new CD<String>("ALG", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, null, null, "Other Allergy", null),
				new CD<String>("160244002", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "No known allergies", null));
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, problemAct));
			super.generateLevel3Text(retVal);

		}
		
		// Now we add the mandatory assertions
		for(Concept allergen : requiredAllergyAssertions)
		{
			Act allergenAct = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence);
			allergenAct.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT)));
			allergenAct.setId(SET.createSET(new II(UUID.randomUUID())));
			allergenAct.setCode(new CD<String>());
			allergenAct.getCode().setNullFlavor(NullFlavor.NotApplicable);
			allergenAct.setStatusCode(ActStatus.Completed);
			allergenAct.setEffectiveTime(new TS(), TS.now());
			allergenAct.getEffectiveTime().getLow().setNullFlavor(NullFlavor.Unknown);

			// Set the author as the device!
			allergenAct.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
			
			// Add then observation
			Observation allergenObs = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
			allergenObs.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_ALERT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ALLERGY_AND_INTOLERANCE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION)));
			allergenObs.setNegationInd(BL.TRUE);
			allergenObs.setId(SET.createSET(new II(UUID.randomUUID())));
			allergenObs.setCode(new CD<String>("ALG", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ObservationIntoleranceType", null, "Other Allergy", null));
			allergenObs.setStatusCode(ActStatus.Completed);
			allergenObs.setEffectiveTime(new TS(), new TS());
			allergenObs.getEffectiveTime().getHigh().setNullFlavor(NullFlavor.Unknown);
			allergenObs.getEffectiveTime().getLow().setNullFlavor(NullFlavor.Unknown);
			allergenObs.setValue(this.m_oddMetadataUtil.getStandardizedCode(allergen, CdaHandlerConstants.CODE_SYSTEM_SNOMED, CD.class));
			allergenObs.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
			allergenAct.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, allergenObs));
			allergenAct.getEntryRelationship().get(0).setInversionInd(BL.FALSE);
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.DRIV, BL.TRUE, allergenAct));
			
		}
		
		// Generate level 3
		if(retVal.getEntry().size() > 0)
			retVal.setText(super.generateLevel3Text(retVal));

		return retVal;
	}
	
	/**
	 * Get the obs group concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
	}

	/**
	 * Get global property
	 *
	 */
	private String getGlobalProperty(String globalPropertyName) {
		return Context.getAdministrationService().getGlobalProperty(globalPropertyName);
	}
	
}
