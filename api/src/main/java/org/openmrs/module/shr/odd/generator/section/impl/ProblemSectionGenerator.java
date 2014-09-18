package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;

import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Generator for Problem section
 */
public class ProblemSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("11450-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "PROBLEM LIST", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public ProblemSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
	}
	
	/**
	 * Generate the section itself
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public Section generateSection() {

		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_ACTIVE_PROBLEMS, CdaHandlerConstants.SCT_TEMPLATE_CCD_PROBLEM), 
			"section.problem.title",
			this.m_sectionCode
		);

		// Problem section must have level 3 content
		if(super.allEncountersHaveDiscreteComponents())
		{
			// No: We want to only report as level 2 using the text 
			super.generateLevel2Content(retVal);
		}
		else
		{
			// This is invalid but as they say GIGO
			super.generateLevel2Content(retVal);
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
