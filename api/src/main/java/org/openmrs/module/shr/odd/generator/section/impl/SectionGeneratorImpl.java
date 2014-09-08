package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.formatters.xml.datatypes.r1.DatatypeFormatter;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.impl.test.util.SectionCreatorUtil;
import org.openmrs.module.shr.cdahandler.processor.util.DatatypeProcessorUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsMetadataUtil;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.generator.SectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.util.CdaDataUtil;

/**
 * Abstract base class implementation for a section generator
 */
public abstract class SectionGeneratorImpl implements SectionGenerator {
	
	// Processor utilities
	protected final DatatypeProcessorUtil m_datatypeProcessorUtil = DatatypeProcessorUtil.getInstance();
	protected final OpenmrsMetadataUtil m_metadataUtil = OpenmrsMetadataUtil.getInstance();
	protected final OpenmrsConceptUtil m_conceptUtil = OpenmrsConceptUtil.getInstance();
	protected final OnDemandDocumentService m_service = Context.getService(OnDemandDocumentService.class);
	protected final CdaDataUtil m_cdaDataUtil = CdaDataUtil.getInstance();
	
	// log
	protected final Log log = LogFactory.getLog(this.getClass());
	
	// Section concepts
	protected abstract Concept getSectionObsGroupConcept();
	
	/**
	 * Create the specified section scaffolding
	 */
	public Section createSection(List<String> templateIds, String titleMessageKey, CE<String> code) {
		Section retVal = new Section();
		
		// TODO: ID
		
		// Localization for title
		ResourceBundle strings = ResourceBundle.getBundle("messages");
		retVal.setTitle(strings.getString(String.format("shr-odd.%s", titleMessageKey)));
		retVal.setText(new SD());
		retVal.setCode(code);
		
		// Set templates
		LIST<II> sectionTemplate = new LIST<II>();
		for(String template : templateIds)
			sectionTemplate.add(new II(template));
		retVal.setTemplateId(sectionTemplate);
		return retVal;
    }

	/**
	 * Returns true if all the provided encounter parts have discrete components
	 * That is, that the ObsGroups representing the 
	 */
	public boolean allEncountersHaveDiscreteComponents(OnDemandDocumentRegistration documentRegistration) {
		
		// Get all obs matching the specified section concept(s) 
		List<Obs> sectionObservations = this.getSectionObs(documentRegistration);
				
		// Loop through found observations and scan for contained obs or orders
		boolean hasComponentObservations = true;
		for(Obs obsGroup : sectionObservations)
		{
			// If the obs group has no group members, we need to look at orders which are a little more complex
			hasComponentObservations &= obsGroup.hasGroupMembers();
		}
		return hasComponentObservations;
    }

	/**
	 * Get section level observations
	 */
	private List<Obs> getSectionObs(final OnDemandDocumentRegistration documentRegistration) {
		
		// Get encounters for the registration
		List<Encounter> docEncounters = new ArrayList<Encounter>();
		for(OnDemandDocumentEncounterLink encounterLink : documentRegistration.getEncounterLinks())
			if(!encounterLink.getVoided() && !encounterLink.getEncounter().getVoided())
				docEncounters.add(encounterLink.getEncounter());
		
		// To appease the search function
		List<Person> recordTarget = new ArrayList<Person>() {{ add(documentRegistration.getPatient()); }};
		

		// Get all obs matching the specified section concept(s) 
		return Context.getObsService().getObservations(
					recordTarget,
					docEncounters, 
					Arrays.asList(this.getSectionObsGroupConcept()), 
					null,
					null, 
					null, 
					null, 
					null, 
					null, 
					null, 
					null, 
					false);    
		}

	/**
	 * Merge the text components of each section obs together into a single obs 
	 */
	public Section generateLevel2Content(OnDemandDocumentRegistration documentRegistration, Section section) {

		SD text = new SD();
		
		// Render a list row per 
		List<Obs> sectionObs = this.getSectionObs(documentRegistration);
		
		// List node
		StructDocElementNode listNode = text.createElement("list", DatatypeFormatter.NS_HL7);
		for(Obs obs : sectionObs)
		{
			StructDocElementNode listItem = listNode.addElement("item");
			listItem.addElement("caption", String.format("From %s on %s (created by %s))", obs.getEncounter().getEncounterType().getName(), obs.getObsDatetime(), obs.getCreator().getUsername()));
			
			StructDocNode childNode = this.m_cdaDataUtil.createText(obs);
			if(!(childNode instanceof StructDocElementNode))
				childNode = listItem.addElement("content", childNode);
			// Scrub the child node's ID because this is level 2 content and no entries reference
			childNode = this.m_cdaDataUtil.scrubIDs(childNode);

			// TODO: Add an ID in case we want to add this as a discrete act at a later time (to be determined with the SHR group)
			((StructDocElementNode)childNode).addAttribute("ID", String.format("obs%s", obs.getId().toString()));
			listItem.getChildren().add(childNode);
		}
		
		text.getContent().add(listNode);
		section.setText(text);
		
		return section;
    }
}
