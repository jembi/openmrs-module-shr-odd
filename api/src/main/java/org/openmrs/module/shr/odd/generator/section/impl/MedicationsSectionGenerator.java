package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.DrugOrder;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Represents a section generator that can generate a medications list from Obs and Orders in OpenMRS 
 */
public class MedicationsSectionGenerator extends SectionGeneratorImpl {
	
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("10160-0", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF MEDICATION USE", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public MedicationsSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
	}
	/**
	 * Generate the medications section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {
		Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_MEDICATIONS, CdaHandlerConstants.SCT_TEMPLATE_MEDICATIONS), 
			"section.medications.title", 
			this.m_sectionCode);
		
		
		// If all have discrete obs or orders 
		if(this.allEncountersHaveDiscreteComponentObsOrOrders(DrugOrder.class))
		{
			// We're going to sort the records by date
			List<BaseOpenmrsData> records = new ArrayList<BaseOpenmrsData>();
			// Get all records in the encounters that are obs
			records.addAll(super.getAllObservationsOfType(Context.getConceptService().getConcept(160741))); // medication history
			records.addAll(super.m_service.getEncounterOrders(super.getDocEncounters(), DrugOrder.class));
			
			// Sort by date
			Collections.sort(records, new Comparator<BaseOpenmrsData>() {
				/**
				 * Sort by date changed so most recent are last
				 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
				 */
				@Override
                public int compare(BaseOpenmrsData o1, BaseOpenmrsData o2) {
					Date date1 = o1.getDateCreated(),
							date2 = o2.getDateCreated();
					if(o1.getDateChanged() != null)
						date1 = o1.getDateChanged();
					if(o2.getDateChanged() != null)
						date2 = o2.getDateChanged();
					return date1.compareTo(date2);
                }
			});
			
			// Now create entries
			for(BaseOpenmrsData data : records)
			{
				if(data.getVoided()) continue;
				
				SubstanceAdministration sbadm = null;
				if(data instanceof DrugOrder)
					sbadm = super.createSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS), (DrugOrder)data);
				else if(data instanceof Obs)
					sbadm = super.createSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS),(Obs)data);
				
				if(sbadm != null)
					retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, sbadm));
			}
			
		}


		if(retVal.getEntry().size() == 0)
		{
			// Generate the no medications known entry
			SubstanceAdministration sbadm = super.createNoSubstanceAdministration(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS, CdaHandlerConstants.ENT_TEMPLATE_MEDICATIONS_NORMAL_DOSING));
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, sbadm));
		}

		if(retVal.getEntry().size() > 0)
			retVal.setText(super.generateLevel3Text(retVal));

		return retVal;
	}
	
	/**
	 * Get the observation group concept(s) for medications
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
	}
	
}
