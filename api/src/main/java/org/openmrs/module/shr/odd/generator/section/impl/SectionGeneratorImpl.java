package org.openmrs.module.shr.odd.generator.section.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.formatters.xml.datatypes.r1.DatatypeFormatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Person;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.processor.util.DatatypeProcessorUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsMetadataUtil;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.generator.SectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.util.CdaDataUtil;
import org.openmrs.module.shr.odd.util.CdaTextUtil;
import org.openmrs.module.shr.odd.util.OddMetadataUtil;

/**
 * Abstract base class implementation for a section generator
 */
public abstract class SectionGeneratorImpl implements SectionGenerator {
	
	// Processor utilities
	protected final DatatypeProcessorUtil m_datatypeProcessorUtil = DatatypeProcessorUtil.getInstance();
	protected final OpenmrsMetadataUtil m_metadataUtil = OpenmrsMetadataUtil.getInstance();
	protected final OddMetadataUtil m_oddMetadataUtil = OddMetadataUtil.getInstance();
	protected final OpenmrsConceptUtil m_conceptUtil = OpenmrsConceptUtil.getInstance();
	protected final OnDemandDocumentService m_service = Context.getService(OnDemandDocumentService.class);
	protected final CdaDataUtil m_cdaDataUtil = CdaDataUtil.getInstance();
	protected final OnDemandDocumentConfiguration m_oddConfiguration = OnDemandDocumentConfiguration.getInstance();
	protected final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	protected final CdaTextUtil m_cdaTextUtil = CdaTextUtil.getInstance();
	
	// log
	protected final Log log = LogFactory.getLog(this.getClass());
	
	// Section concepts
	protected abstract Concept getSectionObsGroupConcept();
	
	// The odd registration context
	protected OnDemandDocumentRegistration m_registration;
	
	/**
     * @see org.openmrs.module.shr.odd.generator.SectionGenerator#setRegistration(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
     */
    @Override
    public void setRegistration(OnDemandDocumentRegistration context) {
    	this.m_registration = context;
    }

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
	public boolean allEncountersHaveDiscreteComponents() {
		
		// Get all obs matching the specified section concept(s) 
		List<Obs> sectionObservations = this.getSectionObs();
				
		// Loop through found observations and scan for contained obs or orders
		boolean hasComponentObservations = true;
		for(Obs obsGroup : sectionObservations)
		{
			List<Obs> groupMembers = this.m_service.getObsGroupMembers(obsGroup);
			// If the obs group has no group members, we need to look at orders which are a little more complex
			hasComponentObservations &= groupMembers != null && groupMembers.size() > 0;
		}
		return hasComponentObservations;
    }

	/**
	 * Get section level observations
	 */
	protected List<Obs> getSectionObs() {
		
	
		// To appease the search function
		List<Person> recordTarget = new ArrayList<Person>();
		recordTarget.add(this.m_registration.getPatient());
		
		// Get all obs matching the specified section concept(s) 
		return Context.getObsService().getObservations(
					recordTarget,
					this.getDocEncounters(), 
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
	 * Get encounters which are included in this document
	 */
	protected List<Encounter> getDocEncounters() {
		List<Encounter> docEncounters = new ArrayList<Encounter>();
		for(OnDemandDocumentEncounterLink encounterLink : this.m_registration.getEncounterLinks())
			if(!encounterLink.getVoided() && !encounterLink.getEncounter().getVoided())
				docEncounters.add(encounterLink.getEncounter());
		return docEncounters;
    }

	/**
	 * Merge the text components of each section obs together into a single obs 
	 */
	public Section generateLevel2Content(Section section) {

		SD text = new SD();
		
		// Render a list row per 
		List<Obs> sectionObs = this.getSectionObs();
		
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

	/**
	 * Get all obs in this set of allowed encounters having the 
	 * specified observation
	 */
	public List<Obs> getAllObservationsOfType(Concept concept) {

		// Get the section obs
		return Context.getObsService().getObservations(
			Arrays.asList((Person)this.m_registration.getPatient()), 
			this.getDocEncounters(), 
			Arrays.asList(concept), 
			null, 
			null, 
			null, 
			null, 
			null, 
			null, 
			null, 
			null, 
			false); // this.m_service.getObsGroupMembers(this.getSectionObs(), Arrays.asList(concept));
		
    }

	/**
	 * Creates an organizer from the specified content
	 */
	public Organizer createOrganizer(x_ActClassDocumentEntryOrganizer classCode, List<String> templateId, CD<String> code, II id, ActStatus status,
                                     Date effectiveTime) {
		Organizer retVal = new Organizer(classCode, status);
		retVal.setTemplateId(new LIST<II>());
		for(String tplId : templateId)
			retVal.getTemplateId().add(new II(tplId));
		
		// Other attributes
		if(id != null)
			retVal.setId(SET.createSET(id));
		
		// code and effective time
		retVal.setCode(code);
		retVal.setEffectiveTime(this.m_cdaDataUtil.createTS(effectiveTime));
		
		return retVal;
		
    }

	/**
	 * Create an observation
	 */
	public Observation createObs(List<String> templateId, Obs sourceObs, String targetCodeCodeSystem) {
	    Observation retVal = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
	    
	    // Set template id
	    retVal.setTemplateId(new LIST<II>());
	    for(String tplId : templateId)
	    	retVal.getTemplateId().add(new II(tplId));
	    
	    // Add identifier
	    retVal.setId(new SET<II>());
	    if(sourceObs.getAccessionNumber() != null)
	    	retVal.getId().add(this.m_cdaDataUtil.parseIIFromString(sourceObs.getAccessionNumber()));
	    retVal.getId().add(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getId().toString()));
	    
	    // Add the code
	    retVal.setCode(this.m_oddMetadataUtil.getStandardizedCode(sourceObs.getConcept(), targetCodeCodeSystem, CD.class));
	    
	    // Now add reference the status code
	    retVal.setEffectiveTime(this.m_cdaDataUtil.createTS(sourceObs.getObsDatetime()));
	    
	    // Is there a creation time?
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
	    // Status is completed
	    retVal.setStatusCode(ActStatus.Completed);
	    
	    // Value .. the tricky part
	    retVal.setValue(this.m_cdaDataUtil.getObservationValue(sourceObs));
	    
	    if(sourceObs.getComment() != null)
	        try {
	            retVal.setText(sourceObs.getComment());
            }
            catch (UnsupportedEncodingException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
	    // TODO: Sub-observations for interpretation, target site, method, etc.
	    return retVal;
	    
    }

	/**
	 * Create an author node that points to correct information
	 */
	private Author createAuthorPointer(BaseOpenmrsData sourceData) {
		Author retVal = new Author(ContextControl.OverridingPropagating);
		if(sourceData.getChangedBy() != null)
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateChanged()));
			retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getChangedBy().getId().toString()))));
		}
		else
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateCreated()));
			retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getCreator().getId().toString()))));
		}
		return retVal;
    }

	/**
	 * Generate the Level 3 content text
	 */
	protected SD generateLevel3Text(Section section)
	{
		SD retVal = new SD();
		StructDocElementNode contextNode = null;
		for(Entry ent : section.getEntry())
			contextNode = this.m_cdaTextUtil.generateText(ent.getClinicalStatement(), contextNode);
		retVal.getContent().add(contextNode);
		return retVal;
	}
	
}
