package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Reference;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Antepartum Visit Flowsheet Section Generator
 */
public class AntepartumVisitFlowsheetSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("57059-8", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Pregnancy Visit Summary", null);
	private final CD<String> m_batteryCode = new CD<String>("57061-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Antepartum Flowsheet Panel", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public AntepartumVisitFlowsheetSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode, null);
	}
	
	/**
	 * Generate the section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {
		
		Section retVal = super.createSection(Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_ANTEPARTUM_TEMPLATE_VISIT_SUMMARY_FLOWSHEET), "section.flowsheet.title", this.m_sectionCode);
		
		// Find the delivery date estimate observation

		// Only get the most recent
		Obs mostRecent = null;

		// Try to obtain the delivery date obs
		try {
        	List<Obs> prepregnancyWeightObservations = super.getAllObservationsOfType(this.m_conceptUtil.getOrCreateConcept(new CV<String>("8348-5", CdaHandlerConstants.CODE_SYSTEM_LOINC)));
    		for(Obs obs : prepregnancyWeightObservations)
    			if(mostRecent == null || obs.getObsDatetime().after(mostRecent.getObsDatetime()))
    				mostRecent = obs;
        }
        catch (DocumentImportException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
        }
		
		// Some observations may come from a pregnancy history or may 
		// Do we have any content?
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			// Most recent pregnancy weight
			if(mostRecent != null)
			{
				Observation pregnancyWeightObs = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), mostRecent, CdaHandlerConstants.CODE_SYSTEM_LOINC);
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.DRIV, BL.TRUE, pregnancyWeightObs));
			}
			
			// Now represent each flowsheet panel
            try {
            	List<Obs> flowsheetBatteries = super.m_service.getObsGroupMembers(this.getSectionObs(), Arrays.asList(this.m_conceptUtil.getOrCreateConcept(this.m_batteryCode)));
				for(Obs battery : flowsheetBatteries)
				{
					Organizer batteryOrganizer = super.createOrganizer(x_ActClassDocumentEntryOrganizer.BATTERY, 
						Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_ANTEPARTUM_FLOWSHEET_PANEL),
						this.m_batteryCode, 
						new II(this.m_cdaConfiguration.getObsRoot(), battery.getId().toString()), 
						ActStatus.Completed, 
						battery.getObsDatetime());
					
					// Set additional core attributes
					batteryOrganizer.setEffectiveTime(super.getEffectiveTime(battery));
					batteryOrganizer.setStatusCode(super.getStatusCode(battery));
					Reference original = super.createReferenceToDocument(battery);
					if(original != null) batteryOrganizer.getReference().add(original);
					
					// Sub observations in the battery 
					for(Obs batteryObs : super.m_service.getObsGroupMembers(battery))
					{
						Observation observation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION), batteryObs, CdaHandlerConstants.CODE_SYSTEM_LOINC);
						batteryOrganizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, observation));
					}
	
					retVal.getEntry().add(new Entry(x_ActRelationshipEntry.DRIV, BL.TRUE, batteryOrganizer));
				}
            }
            catch (DocumentImportException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		else if(super.getSectionObs().size() > 0)
			super.generateLevel2Content(retVal);
		else
			retVal.setText(new SD(SD.createText("No data present")));
		
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
