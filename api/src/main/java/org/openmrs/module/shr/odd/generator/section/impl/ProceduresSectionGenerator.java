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
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Procedure;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.order.ProcedureOrder;

/**
 * Section generator for results section
 */
public class ProceduresSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("47519-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "HISTORY OF PROCEDURES", null);
	private final Concept m_sectionConcept;

	/**
	 * Ctor
	 * @throws DocumentImportException 
	 */
    public ProceduresSectionGenerator() throws DocumentImportException {
        this.m_sectionConcept = this.m_conceptUtil.getOrCreateConcept(this.m_sectionCode);
    }
	
	@Override
	public Section generateSection() {
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_PROCEDURES), 
				"section.procedures.title", 
				this.m_sectionCode);
		

		// This is different as there is no direct child of procedures in the IHE templates
		// So this is a new section (that we've never imported) so we'll report procedures
		// that we find in the OpenMRS Datastore
		// We're going to sort the records by date
		List<BaseOpenmrsData> records = new ArrayList<BaseOpenmrsData>();
		// Get all records in the encounters that are obs
		records.addAll(super.getAllObservationsOfType(Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_PROCEDURE_HISTORY))); // procedure history
		records.addAll(super.m_service.getEncounterOrders(super.getDocEncounters(), ProcedureOrder.class));

		if(records.size() > 0)
		{
			
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
				
				Procedure procedure = null;
				if(data instanceof ProcedureOrder)
					procedure = super.createProcedure(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROCEDURE_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_PROCEDURE_ENTRY), (ProcedureOrder)data);
				else if(data instanceof Obs)
					procedure = super.createProcedure(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROCEDURE_ACTIVITY, CdaHandlerConstants.ENT_TEMPLATE_PROCEDURE_ENTRY),(Obs)data);
				
				if(procedure != null)
					retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, procedure));
			}
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		else
			retVal.setText(new SD(SD.createText("No procedures reported")));
		
		return retVal;
	}
	
	/**
	 * Get the observation group concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
	}
	
}
