package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.List;

import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Generic level 2 section generator
 */
public abstract class GenericLevel2SectionGenerator extends SectionGeneratorImpl {

	// The section concept
	private Concept m_sectionConcept;
	
	/**
	 * Default CTOR
	 */
	public GenericLevel2SectionGenerator()
	{
		try {
	        this.m_sectionConcept = this.m_conceptUtil.getOrCreateConcept(this.getSectionCode());
        }
        catch (DocumentImportException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
        }
	}
	
	/**
	 * Get template ids
	 */
	protected abstract List<String> getTemplateIds();
	/**
	 * Get the title
	 */
	protected abstract String getTitle();
	
	/**
	 * Get the section code
	 */
	protected abstract CE<String> getSectionCode();
	
	/**
	 * Generate the section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {
		Section retVal = super.createSection(this.getTemplateIds(), this.getTitle(), this.getSectionCode());
		
		if(this.getSectionObs().size() > 0)
			this.generateLevel2Content(retVal);
		else
			retVal.setText(new SD(SD.createText("No content available")));
		
		return retVal;
	}
	
	/**
     * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
     */
    @Override
    protected Concept getSectionObsGroupConcept() {
	    return this.m_sectionConcept;
    }
	
	
	
}
