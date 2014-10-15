package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;


/**
 * Generic Level 2 section generator for surgical procedures
 */
public class SurgicalProceduresSectionGenerator extends GenericLevel2SectionGenerator {

	/**
	 * Get template identifiers
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTemplateIds()
	 */
	@Override
    protected List<String> getTemplateIds() {
		return Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_HISTORY_OF_SURGICAL_PROCEDURES);
    }

	/**
	 * Get the title
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTitle()
	 */
	@Override
    protected String getTitle() {
		return "section.surgicalProcedures.title";
    }

	/**
	 * Get the section code
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getSectionCode()
	 */
	@Override
    protected CE<String> getSectionCode() {
		return new CE<String>("10167-5", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF SURGERY", null);
    }

	
}
