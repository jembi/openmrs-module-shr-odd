package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.UUID;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component3;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Procedure;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Reference;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipExternalReference;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Represents a section generator which can construct a Results section
 */
public class ResultsSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("30954-2", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Relevant Diagnostic tests/laboratory data", null);
	private final Concept m_sectionConcept;

	/**
	 * Ctor
	 * @throws DocumentImportException 
	 */
    public ResultsSectionGenerator() throws DocumentImportException {
        this.m_sectionConcept = this.m_conceptUtil.getOrCreateConcept(this.m_sectionCode);
    }
    
    /**
     * Coded Results section
     * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
     */
	@Override
	public Section generateSection() {
		Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_RESULTS, CdaHandlerConstants.SCT_TEMPLATE_RESULTS), "section.results.title", this.m_sectionCode);
		
		// Level 3?
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_RESULTS));

			// Now create entries
			for(Obs data : this.m_service.getObsGroupMembers(this.getSectionObs()))
			{
				if(data.getVoided()) continue;

				switch(data.getConcept().getConceptId())
				{
					case CdaHandlerConstants.CONCEPT_ID_PROCEDURE_HISTORY:
						Procedure proc = super.createProcedure(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROCEDURE_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_PROCEDURE_ENTRY), data);
						retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, proc));
						break;
					default:
						if(CdaHandlerConstants.RMIM_CONCEPT_UUID_REFERENCE.equals(data.getConcept().getUuid()))
						{
							Act act = super.createExternalReferenceAct(data);
							retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, act));
						}
						else // simple obs
						{
							Observation obs = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_RESULT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), data, null);
							retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, obs));
						}
						break;
				}
			}
			
			// Generate text
			retVal.setText(super.generateLevel3Text(retVal));
		}
		// Level 2?
		else if(this.getSectionObs().size() > 0)
			super.generateLevel2Content(retVal);
		// Other
		else
			return null;

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
