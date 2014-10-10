package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubstanceMood;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Generates the immunizations section
 */
public class ImmunizationsSectionGenerator extends SectionGeneratorImpl {
		// The section code
		private final CE<String> m_sectionCode = new CE<String>("11369-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF IMMUNIZATIONS", null);
		private final Concept m_sectionConcept;
		
		/**
		 * Problem section generator ctor
		 */
		public ImmunizationsSectionGenerator() throws DocumentImportException
		{
			this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
		}
		
		/**
		 * Generate the section
		 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
		 */
		@Override
		public Section generateSection() {
			
			Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_IMMUNIZATIONS, CdaHandlerConstants.SCT_TEMPLATE_IMMUNIZATIONS), 
				"section.immunizations.title", 
				this.m_sectionCode);
			
			
			// If all have discrete obs or orders 
			if(this.allEncountersHaveDiscreteComponentObs())
			{
				for(BaseOpenmrsData data : this.getAllObservationsOfType(Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_HISTORY)))
				{
					if(data.getVoided()) continue;
					SubstanceAdministration sbadm = super.createSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_IMMUNIZATIONS),(Obs)data);
					if(sbadm.getCode() == null)
						sbadm.setCode(new CD<String>("IMMUNIZ", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE, "ActCode", null, "Immunization", null));
					retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, sbadm));
				}
				retVal.setText(super.generateLevel3Text(retVal));
				
			}
			else
			{
				// Generate the no medications known entry
				SubstanceAdministration sbadm = super.createNoSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_IMMUNIZATIONS));
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, sbadm));
				retVal.setText(super.generateLevel3Text(retVal));
			}
			
			return retVal;
		}
	
	/**
	 * Get the section concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
	}
	
}
