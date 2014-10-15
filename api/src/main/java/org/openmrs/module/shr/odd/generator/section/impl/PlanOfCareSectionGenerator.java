package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.generic.CE;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * Get the plan of care section generator (currently level 2)
 */
public class PlanOfCareSectionGenerator extends GenericLevel2SectionGenerator {
	
	/**
	 * Plan of care
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTemplateIds()
	 */
	@Override
	protected List<String> getTemplateIds() {
		return Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_PLAN_OF_CARE, CdaHandlerConstants.SCT_TEMPLATE_CARE_PLAN);
	}

	/**
	 * Get the title of the section
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getTitle()
	 */
	@Override
	protected String getTitle() {
		return "section.planOfCare.title";
	}
	
	/**
	 * Get the section code
	 * @see org.openmrs.module.shr.odd.generator.section.impl.GenericLevel2SectionGenerator#getSectionCode()
	 */
	@Override
	protected CE<String> getSectionCode() {
		return new CE<String>("18776-5", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "TREATMENT PLAN", null);
	}
	
}
