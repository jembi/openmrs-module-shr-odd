package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.generic.CE;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * Advance directives section generator
 */
public class AdvanceDirectivesSectionGenerator extends GenericLevel2SectionGenerator {
	
	/**
	 * Get the template identifiers
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTemplateIds()
	 */
	@Override
	protected List<String> getTemplateIds() {
		return Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_ADVNACE_DIRECTIVES, CdaHandlerConstants.SCT_TEMPLATE_ADVANCE_DIRECTIVES);
	}
	
	/**
	 * Get the title 
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "section.advanceDirectives.title";
	}
	
	/**
	 * Get the section code
	 */
	@Override
	protected CE<String> getSectionCode() {
		return new CE<String>("42348-3", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "ADVANCE DIRECTIVES", null);
	}
	
	
}
