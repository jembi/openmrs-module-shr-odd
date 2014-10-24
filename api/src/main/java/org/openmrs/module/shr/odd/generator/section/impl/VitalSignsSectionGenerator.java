package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Vital signs section generator
 */
public class VitalSignsSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("8716-3", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "VITAL SIGNS", null);
	private final Concept m_sectionConcept;
	private final CD<String> m_organizerCode = new CD<String>("46680005", CdaHandlerConstants.CODE_SYSTEM_SNOMED, "SNOMED CT", null, "Vital Signs", null);
	
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
	public Section generateSection() {
		
		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_VITAL_SIGNS, CdaHandlerConstants.SCT_TEMPLATE_CCD_VITAL_SIGNS), 
			"section.vitalSigns.title",
			this.m_sectionCode
		);
		
		// Now are we create a coded vital signs section or not?
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			// Yes: this is level 3, we want to generate at level 3
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_VITAL_SIGNS));
			
			// This means that we want to generate level 3 content. We're going to organize the vital signs into an organizer which 
			// is drawn from other organizers done at the same time (or encounter)
			
			// Collect by date observation was made and group
			// This is done because sometimes vital signs can be posted like
			// 	ORG
			//		OBS
			//		OBS
			//		OBS
			// Or some systems (like OSCAR) do this:
			//	ORG
			//		OBS
			//	ORG 
			//		OBS
			// What we really want is the former so we group by obs-date or the date the container organizer was created
			Map<Date, Set<Obs>> componentObsByDate = new HashMap<Date, Set<Obs>>();
			
			// Vital signs are contained in Vital Signs organizers however we want
			// to skip the organizer to create our own this means that we need to 
			// get the sub code type
			try {
				// Get all the observations within the encounter belonging 
				List<Concept> vitalSignTypes = this.m_conceptUtil.getOrCreateConcept(m_organizerCode).getSetMembers();
				if(vitalSignTypes.size() == 0)
					vitalSignTypes = this.m_conceptUtil.getOrCreateConcept(m_sectionCode).getSetMembers();
				// Get all obs in organizers (that were created properly)
				List<Obs> candidateObsInOrganizers = this.m_service.getObsGroupMembers(super.getAllObservationsOfType(this.m_conceptUtil.getConcept(m_organizerCode, null)), vitalSignTypes);
				// Get all obs in just the sections if needed
				candidateObsInOrganizers.addAll(this.m_service.getObsGroupMembers(super.getSectionObs(), vitalSignTypes));
				
	            for(Obs vitalSignsObs : candidateObsInOrganizers)
	            {
	            	
	            	if(vitalSignsObs.getVoided()) continue; /// skip
	            	
	            	Date organizerDate = vitalSignsObs.getObsDatetime();
	            	if(vitalSignsObs.getObsGroup() != null)
	            		organizerDate = vitalSignsObs.getObsGroup().getObsDatetime();

	            	// Get the current organizer observations
	            	Set<Obs> organizerComponents = componentObsByDate.get(organizerDate);
	            	if(organizerComponents == null)
	            	{
	            		organizerComponents = new HashSet<Obs>();
	            		componentObsByDate.put(organizerDate, organizerComponents);
	            	}
	            	
	            	if(!organizerComponents.contains(vitalSignsObs))
	            		organizerComponents.add(vitalSignsObs);
	            }
            }
            catch (DocumentImportException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
			
			// Now to create the organizers
			for(Map.Entry<Date, Set<Obs>> organizer : componentObsByDate.entrySet())
			{
				Organizer org = super.createOrganizer(
					x_ActClassDocumentEntryOrganizer.CLUSTER,
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_VITAL_SIGNS_ORGANIZER, CdaHandlerConstants.ENT_TEMPLATE_CCD_RESULT_ORGANIZER, CdaHandlerConstants.ENT_TEMPLATE_VITAL_SIGNS_ORGANIZER),
					this.m_organizerCode,
					null, 
					ActStatus.Completed,
					organizer.getKey());
				
				// Get the ID of the first organizer
				II proposedOrganizerId = null;
				for(Obs component : organizer.getValue())
				{
					
					if(component.getVoided()) continue;
					
					II containerId = null;
					if(component.getObsGroup() != null && component.getObsGroup().getAccessionNumber() != null) // Was in a group
						containerId = this.m_cdaDataUtil.parseIIFromString(component.getObsGroup().getAccessionNumber());
					else if(component.getObsGroup() != null)
						containerId = new II(this.m_cdaConfiguration.getObsRoot(), component.getObsGroup().getId().toString());
					
					// Set the organizer id to the component obs if applicable
					if(proposedOrganizerId == null)
						proposedOrganizerId = containerId;
					else if(proposedOrganizerId != null && !proposedOrganizerId.equals(containerId)) // Random as we don't have a mechanism to store this!
						proposedOrganizerId = new II(UUID.randomUUID());

					// Now create the obs
					Component4 organizerComponent = new Component4();
					organizerComponent.setClinicalStatement(
						super.createObs(
							Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_SIMPLE_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_CCD_RESULT_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_VITAL_SIGNS_OBSERVATION),
							component,
							CdaHandlerConstants.CODE_SYSTEM_LOINC)
					);	
					
					org.getComponent().add(organizerComponent);
				}
				
				// Set the proposed organizer ID
				org.setId(SET.createSET(proposedOrganizerId));
				
				// Now add to the section
				if(org.getComponent().size() > 0)
				{
					Entry entry = new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE);
					entry.setClinicalStatement(org);
					retVal.getEntry().add(entry);
				}
			}
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		else if(super.getSectionObs().size() > 0)
		{
			// No: We want to only report as level 2 using the text 
			super.generateLevel2Content(retVal);
		}
		else
		{
			retVal.setText(new SD(SD.createText("No content recorded")));

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
