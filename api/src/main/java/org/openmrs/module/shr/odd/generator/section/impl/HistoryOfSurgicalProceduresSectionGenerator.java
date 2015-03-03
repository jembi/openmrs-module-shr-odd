package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.generic.CE;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * History of Surgical Procedures Section Generator
 */
public class HistoryOfSurgicalProceduresSectionGenerator extends GenericLevel2SectionGenerator {
	/**
	 * Get the template identifiers
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTemplateIds()
	 */
	@Override
    protected List<String> getTemplateIds() {
		return Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_HISTORY_OF_SURGICAL_PROCEDURES);
    }

	/**
	 * Get the title of the birth plan
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTitle()
	 */
	@Override
    protected String getTitle() {
		return "section.historyOfSurgery.title";
    }
	
	/**
	 * Get the section code
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getSectionCode()
	 */
	@Override
    protected CE<String> getSectionCode() {
		return new CE<String>("10167-5", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF SURGICAL PROCEDURES", null);
    }	
}
