package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Estimated delivery date section generator
 */
public class EstimatedDeliveryDatesSectionGenerator extends SectionGeneratorImpl {
	
	private final CE<String> m_sectionCode = new CE<String>("57060-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Estimated Date of Delivery", null);
	
	/**
	 * This is based on a variety of sources but will contain only one observation
	 * with the most recent estimated delivery date 
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {

		Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_ESTIMATED_DELIVERY_DATES), "section.deliveryDates.title", this.m_sectionCode);
		
		// Find the delivery date estimate observation

		// Only get the most recent
		Obs mostRecent = null;

		// Try to obtain the delivery date obs
		try {
        	List<Obs> deliveryDateObservations = super.getAllObservationsOfType(this.m_conceptUtil.getOrCreateConcept(new CV<String>("11778-8", CdaHandlerConstants.CODE_SYSTEM_LOINC)));
    		for(Obs obs : deliveryDateObservations)
    			if(mostRecent == null || obs.getObsDatetime().after(mostRecent.getObsDatetime()))
    				mostRecent = obs;
        }
        catch (DocumentImportException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
        }
		
		// Do we have any content?
		if(mostRecent != null)
		{
			Observation eddObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_DELIVERY_DATE_OBSERVATION), mostRecent, CdaHandlerConstants.CODE_SYSTEM_LOINC);
			
			if(eddObservation.getValue() instanceof TS)
				((TS)eddObservation.getValue()).setDateValuePrecision(TS.DAY);
			
			// Sub-observations?
			List<Obs> supportData = this.m_service.getObsGroupMembers(mostRecent);
			for(Obs support : supportData)
			{
				Observation supportObs = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), support, CdaHandlerConstants.CODE_SYSTEM_LOINC);

				if(supportObs.getValue() instanceof TS)
					((TS)supportObs.getValue()).setDateValuePrecision(TS.DAY);

				// Do the support obs have dirivation obs under them?
				List<Obs> derivedFromObs = this.m_service.getObsGroupMembers(support);
				for(Obs derivation : derivedFromObs)
				{
					Observation derivationObs = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), derivation, CdaHandlerConstants.CODE_SYSTEM_LOINC);

					if(derivationObs.getValue() instanceof TS)
						((TS)derivationObs.getValue()).setDateValuePrecision(TS.DAY);
					
					supportObs.getEntryRelationship().add(new EntryRelationship(new x_ActRelationshipEntryRelationship("DRIV", x_ActRelationshipEntryRelationship.CAUS.getCodeSystem()), null, derivationObs));
				}
				
				// Add support obs
				eddObservation.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SPRT, null, supportObs));
			}
			
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.DRIV, BL.TRUE, eddObservation));
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		else
			retVal.setText(new SD(SD.createText("No estimate available")));
		
		return retVal;
	}

	/**
	 * Get the observation group concept (not used)
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
