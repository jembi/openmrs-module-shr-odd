package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.PN;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.IVL;
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
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.activelist.ActiveListItem;
import org.openmrs.activelist.Allergy;
import org.openmrs.activelist.AllergySeverity;
import org.openmrs.activelist.Problem;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * A generator for the allergies or alerts section
 */
public class AllergiesSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("48765-2", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "PROBLEM LIST", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public AllergiesSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
	}
	
	@Override
	public Section generateSection() {
		// Are there problems on file for the patient?
		List<ActiveListItem> allergies = Context.getActiveListService().getActiveListItems(this.m_registration.getPatient(), Allergy.ACTIVE_LIST_TYPE);

		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_ALERTS, CdaHandlerConstants.SCT_TEMPLATE_ALLERGIES), 
			"section.allergy.title",
			this.m_sectionCode
		);

		// Problem section must have level 3 content
		if(allergies.size() > 0)
		{
			// Generate problem list
			for(ActiveListItem itm : allergies)
			{
				Act problemAct = super.createAct(
					x_ActClassDocumentEntryAct.Act,
					x_DocumentActMood.Eventoccurrence,
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
					itm);
				
				Allergy allergy = (Allergy)itm;
				
				// Add an entry relationship of the problem
				Obs problemObs = itm.getStartObs();
				if(itm.getStopObs() != null)
					problemObs = itm.getStopObs();
				
				Observation problemObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_ALERT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ALLERGY_AND_INTOLERANCE_OBSERVATION), 
					problemObs, 
					CdaHandlerConstants.CODE_SYSTEM_SNOMED);
				
				// Now for allergy information
				String typeMnemonic = "", display = "";
				switch(allergy.getAllergyType())
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
				playingEntity.setCode(this.m_oddMetadataUtil.getStandardizedCode(allergy.getAllergen(), null, CE.class));
				playingEntity.setName(SET.createSET(new PN(Arrays.asList(new ENXP(allergy.getAllergen().getName().getName())))));
				problemObservation.getParticipant().get(0).getParticipantRole().setPlayingEntityChoice(playingEntity);
				
				
				// Now the severity
				String severityCode = null;
				switch(allergy.getSeverity())
				{
					case SEVERE:
						severityCode = "H";
						break;
					case MODERATE:
						severityCode = "M";
						break;
					case MILD:
						severityCode = "L";
						break;
				}
				if(severityCode != null)
				{
					Observation severityObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
					severityObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_SEVERITY_OBSERVATION)));
					severityObservation.setCode(new CD<String>("SEV", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ActCode", null, "Severity", null));
					severityObservation.setText(new ED(allergy.getSeverity().name()));
					severityObservation.setStatusCode(ActStatus.Completed);
					severityObservation.setValue(new CD<String>(severityCode, CdaHandlerConstants.CODE_SYSTEM_OBSERVATION_VALUE, "ObservationValue", null, null, allergy.getSeverity().name()));
					problemObservation.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, BL.TRUE, null, null, null, severityObservation));
				}
				
				// Now the manifestation
				if(allergy.getReaction() != null)
				{
					EntryRelationship manifestation = new EntryRelationship(x_ActRelationshipEntryRelationship.MFST, BL.TRUE);
					manifestation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_MANIFESTATION_RELATION)));
					Observation manifestationObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
					manifestationObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_REACTION_OBSERVATION)));
					manifestationObservation.setCode(new CD<String>("PROBLEM", CdaHandlerConstants.CODE_SYSTEM_IHE_ACT_CODE));
					manifestationObservation.setStatusCode(ActStatus.Completed);
					manifestationObservation.setEffectiveTime(new IVL<TS>());
					manifestationObservation.getEffectiveTime().setNullFlavor(NullFlavor.NoInformation);
					manifestationObservation.setValue(this.m_oddMetadataUtil.getStandardizedCode(allergy.getReaction(), null, CD.class));
					manifestation.setClinicalStatement(manifestationObservation);
					problemObservation.getEntryRelationship().add(manifestation);
				}
				problemAct.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.FALSE, BL.TRUE, null, null, null, problemObservation));
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, problemAct));
				
			}

			retVal.setText(super.generateLevel3Text(retVal));
			
		}
		else if(this.getSectionObs().size() > 0) // unstructured?
		{
			super.generateLevel2Content(retVal);
		}
		else
		{
			retVal.setText(new SD(SD.createText("No content recorded")));
		}
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
	
}
