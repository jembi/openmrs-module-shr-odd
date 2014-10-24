package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.ResourceBundle;

import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;

/**
 * Purpose of use section generator
 */
public class PurposeOfUseSectionGenerator extends SectionGeneratorImpl {
	

		
	/**
	 * Generate the section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {
		Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_PURPOSE), "section.purpose.title", new CE<String>("48764-5", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Summary Purpose", null));
		ResourceBundle strings = ResourceBundle.getBundle("messages");
		retVal.setText(new SD(new StructDocElementNode("paragraph", strings.getString("shr-odd.section.purpose.text"))));
		return retVal;
	}
	
	/**
	 * Get the section obs group concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
