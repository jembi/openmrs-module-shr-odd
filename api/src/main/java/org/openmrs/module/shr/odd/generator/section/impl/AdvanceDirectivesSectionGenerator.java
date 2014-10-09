package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Procedure;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.order.ProcedureOrder;

/**
 * Advance directives section generator
 */
public class AdvanceDirectivesSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
		private final CE<String> m_sectionCode = new CE<String>("42348-3", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "ADVANCE DIRECTIVES", null);
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
		

		// Level 3?
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_ADVANCE_DIRECTIVES));
			// Now create entries
			for(Obs data : this.m_service.getObsGroupMembers(this.getSectionObs()))
			{
				if(data.getVoided()) continue;
				
				Observation directive = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_ADVANCE_DIRECTIVE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_ADVANCE_DIRECTIVE_OBSERVATION), data, CdaHandlerConstants.CODE_SYSTEM_SNOMED);
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, directive));
			}
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		// Level 2?
		else if(this.getSectionObs().size() > 0)
			super.generateLevel2Content(retVal);
		// Other
		else
			retVal.setText(new SD(SD.createText("No directives recorded")));
		
		return retVal;
    }

	@Override
    protected Concept getSectionObsGroupConcept() {
	    // TODO Auto-generated method stub
	    return null;
    }
	
	
}
