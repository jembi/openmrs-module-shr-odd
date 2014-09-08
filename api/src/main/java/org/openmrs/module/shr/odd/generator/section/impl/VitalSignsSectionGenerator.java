package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;

import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * Vital signs section generator
 */
public class VitalSignsSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("8716-3", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "VITAL SIGNS", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Constructor 
	 */
    public VitalSignsSectionGenerator() throws DocumentImportException {
        this.m_sectionConcept = this.m_conceptUtil.getOrCreateConcept(this.m_sectionCode);
    }
    
	/**
	 * Generate the vital signs section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public Section generateSection(OnDemandDocumentRegistration documentRegistration) {
		
		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_VITAL_SIGNS, CdaHandlerConstants.SCT_TEMPLATE_CCD_VITAL_SIGNS), 
			"section.vitalSigns.title",
			this.m_sectionCode
		);
		
		// Now are we create a coded vital signs section or not?
		if(super.allEncountersHaveDiscreteComponents(documentRegistration))
		{
			// Yes: this is level 3, we want to generate at level 3
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_VITAL_SIGNS));
			// No: We want to only report as level 2 using the text 
			super.generateLevel2Content(documentRegistration, retVal);
		}
		else
		{
			// No: We want to only report as level 2 using the text 
			super.generateLevel2Content(documentRegistration, retVal);
		}
		return retVal;
	}

	/**
	 * Get the section obs' concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
    protected Concept getSectionObsGroupConcept() {
	    return this.m_sectionConcept;
    }
	
}
