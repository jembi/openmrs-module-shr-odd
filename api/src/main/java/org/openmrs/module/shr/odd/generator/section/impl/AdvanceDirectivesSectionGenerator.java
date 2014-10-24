package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Advance directives section generator
 */
public class AdvanceDirectivesSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
		private final CE<String> m_sectionCode = new CE<String>("42348-3", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "ADVANCE DIRECTIVES", null);
		private final CD<String> m_statusCode = new CD<String>("33999-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Status", null);
		private final Concept m_sectionConcept;

		/**
		 * Ctor
		 * @throws DocumentImportException 
		 */
	    public AdvanceDirectivesSectionGenerator() throws DocumentImportException {
	        this.m_sectionConcept = this.m_conceptUtil.getOrCreateConcept(this.m_sectionCode);
	    }
		
	/**
	 * Generate the section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
    public Section generateSection() {
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_ADVNACE_DIRECTIVES, CdaHandlerConstants.SCT_TEMPLATE_ADVANCE_DIRECTIVES), 
			"section.advanceDirectives.title",
				this.m_sectionCode);
		
		// We MUST assert blood transfusion
		List<Concept> requiredDirectives = Arrays.asList(
			Context.getConceptService().getConcept(1063)
		);
		
		// Level 3?
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_ADVANCE_DIRECTIVES));
			// Now create entries
			for(Obs data : this.m_service.getObsGroupMembers(this.getSectionObs()))
			{
				if(data.getVoided()) continue;
				requiredDirectives.remove(data.getConcept());
				Observation directive = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_ADVANCE_DIRECTIVE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ADVANCE_DIRECTIVE_OBSERVATION), data, CdaHandlerConstants.CODE_SYSTEM_SNOMED);
				
				// Status?
                try {
                	List<Obs> status = this.m_service.getObsGroupMembers(data, Arrays.asList(this.m_conceptUtil.getOrCreateConcept(m_statusCode)));
					if(status.size() == 1)
						directive.getEntryRelationship().add(this.createAdvanceDirectiveStatusObservation(status.get(0).getValueCoded()));
                }
                catch (DocumentImportException e) {
	                // TODO Auto-generated catch block
	                log.error("Error generated", e);
                }
				
				// HACK: This needs to change in the IHE Stuff
				if(data.getConcept().getId().equals(1063)) // Blood transfusion 
				{
					CD<String> hackedCode = new CD<String>("(xx-bld-transf-ok)", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood Tranfusion", null);
					hackedCode.setTranslation(SET.createSET(directive.getCode()));
					hackedCode.getTranslation().addAll(directive.getCode().getTranslation());
					directive.setCode(hackedCode);
				}
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, directive));
			}
			
		}
		// Level 2?
		else if(this.getSectionObs().size() > 0)
			super.generateLevel2Content(retVal);
		// Other
		else
			retVal.setText(new SD(SD.createText("No directives recorded")));

		// Any required directives?
		for(Concept directive : requiredDirectives)
		{
			Observation directiveObs = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
			directiveObs.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_ADVANCE_DIRECTIVE_OBSERVATION), new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_ADVANCE_DIRECTIVE_OBSERVATION)));
			directiveObs.setId(SET.createSET(new II(UUID.randomUUID())));
			directiveObs.setNegationInd(BL.TRUE);
			directiveObs.setValue(new BL());
			directiveObs.getValue().setNullFlavor(NullFlavor.NoInformation);
			directiveObs.setEffectiveTime(null, TS.now());
			directiveObs.setStatusCode(ActStatus.Completed);
			directiveObs.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
			directiveObs.setCode(new CD<String>("(xx-bld-transf-ok)", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Blood Tranfusion", null));
			directiveObs.getEntryRelationship().add(this.createAdvanceDirectiveStatusObservation(null));
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, directiveObs));
		}
		
		if(retVal.getEntry().size() > 0)
			retVal.setText(super.generateLevel3Text(retVal));

		return retVal;
    }

	/**
	 * Get the section obs group concept
	 */
	@Override
    protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
    }
	
	
	/**
	 * Advance directive status
	 * Auto generated method comment
	 * 
	 * @param status
	 * @return
	 */
	protected EntryRelationship createAdvanceDirectiveStatusObservation(Concept status)
	{
		Observation observation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
		EntryRelationship retVal = new EntryRelationship(x_ActRelationshipEntryRelationship.REFR, BL.TRUE, observation);
		observation.setTemplateId(super.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_ADVANCE_DIRECTIVE_STATUS, CdaHandlerConstants.ENT_TEMPLATE_CCD_STATUS_OBSERVATION)));
		observation.setCode(this.m_statusCode);
		observation.setStatusCode(ActStatus.Completed);
		observation.setValue(this.m_oddMetadataUtil.getStandardizedCode(status, CdaHandlerConstants.CODE_SYSTEM_SNOMED, CD.class));
		return retVal;
	}
	
}
