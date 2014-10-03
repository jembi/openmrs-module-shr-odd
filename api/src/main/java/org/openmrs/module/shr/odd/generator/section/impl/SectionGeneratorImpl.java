package org.openmrs.module.shr.odd.generator.section.impl;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.EN;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.INT;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.PQ;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.SetOperator;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.DomainTimingEvent;
import org.marc.everest.datatypes.generic.EIVL;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.PIVL;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.datatypes.generic.SXCM;
import org.marc.everest.datatypes.interfaces.ISetComponent;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.formatters.xml.datatypes.r1.DatatypeFormatter;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Consumable;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Criterion;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ManufacturedProduct;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Material;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Precondition;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Product;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Supply;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ObservationInterpretation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.RoleClassManufacturedProduct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubstanceMood;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Concept;
import org.openmrs.Drug;
import org.openmrs.DrugOrder;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.activelist.ActiveListItem;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.ConcernEntryProcessor;
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
import org.openmrs.util.OpenmrsConstants;

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

	protected static final String REGEX_IVL_PQ = "^\\{?([\\d.]*)?\\s(\\w*)?\\s?\\.*\\s?([\\d.]*)?\\s?([\\w]*?)?\\}?$";

	// log
	protected final Log log = LogFactory.getLog(this.getClass());
	
	// Section concepts
	protected abstract Concept getSectionObsGroupConcept();
	
	// The odd registration context
	protected OnDemandDocumentRegistration m_registration;
	protected ClinicalDocument m_documentContext;
	
	/**
     * @see org.openmrs.module.shr.odd.generator.SectionGenerator#setRegistration(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
     */
    @Override
    public void setRegistration(OnDemandDocumentRegistration context) {
    	this.m_registration = context;
    }

    /**
     * Sets the clinical document context 
     * @see org.openmrs.module.shr.odd.generator.SectionGenerator#setGeneratedDocument(org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument)
     */
    public void setGeneratedDocument(ClinicalDocument context)
    {
    	this.m_documentContext = context;
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
	public boolean allEncountersHaveDiscreteComponentObs() {
		
		// Get all obs matching the specified section concept(s) 
		List<Obs> sectionObservations = this.getSectionObs();
		// Loop through found observations and scan for contained obs or orders
		boolean hasComponentObservations = sectionObservations.size() > 0;
		for(Obs obsGroup : sectionObservations)
		{
			List<Obs> groupMembers = this.m_service.getObsGroupMembers(obsGroup);
			// If the obs group has no group members, we need to look at orders which are a little more complex
			hasComponentObservations &= groupMembers != null && groupMembers.size() > 0;
		}
		return hasComponentObservations;
    }
	
	/**
	 * Returns true if all the provided encounter parts have discrete components
	 * That is, that the ObsGroups representing the 
	 */
	public boolean allEncountersHaveDiscreteComponentObsOrOrders(Class<? extends Order> orderType) {
		
		// Get all obs matching the specified section concept(s)
		List<Encounter> docEncounters = this.getDocEncounters();
		boolean hasComponents = docEncounters.size() > 0;
		for(Encounter e : docEncounters)
		{
			// To appease the search function
			List<Person> recordTarget = new ArrayList<Person>();
			recordTarget.add(this.m_registration.getPatient());
			
			// Get all obs matching the specified section concept(s) 
			List<Obs> sectionObs = Context.getObsService().getObservations(
						recordTarget,
						Arrays.asList(e), 
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
			List<Order> encounterOrders = this.m_service.getEncounterOrders(Arrays.asList(e), orderType);
			
			// This encounter must have either orders of the specified type or
			hasComponents &= (encounterOrders.size() > 0 || this.m_service.getObsGroupMembers(sectionObs).size() > 0);
		}
		return hasComponents;
    }

	/**
	 * Get all orders in the sections
	 */
	private List<Order> getEncounterOrders() {
		return this.m_service.getEncounterOrders(this.getDocEncounters());
    }

	/**
	 * Get section level observations
	 */
	protected List<Obs> getSectionObs() {
		
	
		// To appease the search function
		List<Person> recordTarget = new ArrayList<Person>();
		recordTarget.add(this.m_registration.getPatient());
		Concept sectionConcept = this.getSectionObsGroupConcept();
		
		// Get all obs matching the specified section concept(s) 
		return Context.getObsService().getObservations(
					recordTarget,
					this.getDocEncounters(), 
					Arrays.asList(sectionConcept), 
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
			listItem.addElement("caption", String.format("From %s on %s (created by %s))", obs.getEncounter().getEncounterType().getName(), obs.getObsDatetime(), obs.getCreator().getPersonName()));
			
			StructDocNode childNode = this.m_cdaDataUtil.createText(obs);
			if(!(childNode instanceof StructDocElementNode))
				childNode = listItem.addElement("content", childNode);
			else
			{
				// Scrub the child node's ID because this is level 2 content and no entries reference
				childNode = this.m_cdaDataUtil.scrubIDs(childNode);
	
				// TODO: Add an ID in case we want to add this as a discrete act at a later time (to be determined with the SHR group)
				((StructDocElementNode)childNode).addAttribute("ID", String.format("obs%s", obs.getId().toString()));
				listItem.getChildren().add(childNode);
			}
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
	 * Create an Act
	 */
	public Act createAct(x_ActClassDocumentEntryAct classCode, x_DocumentActMood moodCode, List<String> templateId, ActiveListItem activeListItem) {
		Act retVal = new Act();
		retVal.setClassCode(classCode);
		retVal.setMoodCode(moodCode);
		
		retVal.setTemplateId(new LIST<II>());
		for(String tplId : templateId)
			retVal.getTemplateId().add(new II(tplId));
		
		  // Add identifier
	    retVal.setId(new SET<II>());
	    if(activeListItem.getStartObs() != null && activeListItem.getStartObs().getAccessionNumber() != null &&
	    		activeListItem.getStartObs().getAccessionNumber().isEmpty())
	    	retVal.getId().add(this.m_cdaDataUtil.parseIIFromString(activeListItem.getStartObs().getAccessionNumber()));
	    
	    retVal.getId().add(new II(this.m_cdaConfiguration.getProblemRoot(), activeListItem.getId().toString()));
	    // Add the code
	    retVal.setCode(new CD<String>());
	    retVal.getCode().setNullFlavor(NullFlavor.NotApplicable);
	    
	    // Now add reference the status code
	    IVL<TS> eft = new IVL<TS>();
	    if(activeListItem.getStartObs() != null)
	    {
    		eft.setLow(this.m_cdaDataUtil.createTS(activeListItem.getStartDate()));
	    	if(activeListItem.getStartObs() != null)
	    	{
	    		// Correct the precision of the dates
	    		ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(activeListItem.getStartObs().getId());
	    		if(obs != null)
	    			eft.getLow().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    }
	    else
	    {
	    	eft.setLow(new TS());
	    	eft.getLow().setNullFlavor(NullFlavor.Unknown);
	    }
	    if(activeListItem.getStopObs() != null)
	    {
	    	eft.setHigh(this.m_cdaDataUtil.createTS(activeListItem.getEndDate()));
	    	if(activeListItem.getStopObs() != null)
	    	{
	    		// Correct the precision of the dates
	    		ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(activeListItem.getStopObs().getId());
	    		if(obs != null)
	    			eft.getHigh().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    	
	    }
	    else
	    {
	    	eft.setHigh(new TS());
	    	eft.getHigh().setNullFlavor(NullFlavor.Unknown);
	    }

	    retVal.setEffectiveTime(eft);
	    
	    // Is there a creation time?
	    retVal.getAuthor().add(this.createAuthorPointer(activeListItem));
	    
	    retVal.setStatusCode(ConcernEntryProcessor.calculateCurrentStatus(activeListItem));;
	    
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
	    if(sourceObs.getAccessionNumber() != null && !sourceObs.getAccessionNumber().isEmpty())
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
	    
	    // Extended observation stuff
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());
	    if(extendedObs != null)
	    	this.setExtendedObservationProperties(retVal, extendedObs);
	    
	    // Look for negation
	    List<Obs> negation = this.m_service.getObsGroupMembers(sourceObs, Arrays.asList(Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_SIGN_SYMPTOM_PRESENT)));
	    if(negation.size() > 0 && negation.get(0).getValueCoded().getId().toString().equals(Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT)))
	    	retVal.setNegationInd(BL.TRUE);
	    	
	    	
	    return retVal;
	    
    }

	/**
	 * Set extended observation properties
	 */
	protected void setExtendedObservationProperties(Observation cdaObservation, ExtendedObs extendedObs) {

    	if(extendedObs.getObsInterpretation() != null)
    		cdaObservation.setInterpretationCode(SET.createSET((CE<ObservationInterpretation>)this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsInterpretation(), ObservationInterpretation.Abnormal.getCodeSystem(), CE.class)));
    	if(extendedObs.getObsRepeatNumber() != null)
    		cdaObservation.setRepeatNumber(new INT(extendedObs.getObsRepeatNumber()));
    	if(extendedObs.getObsMood() != null)
    		cdaObservation.setMoodCode(this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsMood(), x_ActMoodDocumentObservation.Definition.getCodeSystem(), CS.class));
    	
    	// Date stuff
    	if(cdaObservation.getEffectiveTime() == null)
    		cdaObservation.setEffectiveTime(new IVL<TS>());
    	
    	// status?
    	if(extendedObs.getObsStatus() != null)
    		cdaObservation.setStatusCode(this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsStatus(), ActStatus.Aborted.getCodeSystem(), CS.class));
    	
    	if(extendedObs.getObsDatetime() != null &&
    			extendedObs.getObsStartDate() == null &&
    			extendedObs.getObsEndDate() == null)
    		cdaObservation.getEffectiveTime().setValue(this.m_cdaDataUtil.createTS(extendedObs.getObsDatetime()));
    	else
    	{
    		cdaObservation.getEffectiveTime().setValue(null);
	    	if(extendedObs.getObsStartDate() != null)
	    		cdaObservation.getEffectiveTime().setLow(this.m_cdaDataUtil.createTS(extendedObs.getObsStartDate()));
	    	if(extendedObs.getObsEndDate() != null)
	    		cdaObservation.getEffectiveTime().setHigh(this.m_cdaDataUtil.createTS(extendedObs.getObsEndDate()));
		}
    	
    	// Null ?
    	if(extendedObs.getObsDatePrecision() == 0)
    		cdaObservation.getEffectiveTime().setNullFlavor(NullFlavor.Unknown);
    	
    	// Set precision
    	if(cdaObservation.getEffectiveTime().getValue() != null)
    		cdaObservation.getEffectiveTime().getValue().setDateValuePrecision(extendedObs.getObsDatePrecision());
    	if(cdaObservation.getEffectiveTime().getLow() != null)
    		cdaObservation.getEffectiveTime().getLow().setDateValuePrecision(extendedObs.getObsDatePrecision());
    	if(cdaObservation.getEffectiveTime().getHigh() != null)
    		cdaObservation.getEffectiveTime().getHigh().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	
    }

	/**
	 * Create an author node that points to correct information
	 */
	protected Author createAuthorPointer(BaseOpenmrsData sourceData) {
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
		{
			StructDocElementNode genNode = this.m_cdaTextUtil.generateText(ent.getClinicalStatement(), contextNode, this.m_documentContext);
			if(contextNode == null)
				contextNode = genNode; 
		}
		retVal.getContent().add(contextNode);
		return retVal;
	}

	/**
	 * Create a substance administration from a drug order
	 */
	public SubstanceAdministration createSubstanceAdministration(List<String> templateIds, DrugOrder data) {
	    SubstanceAdministration retVal = new SubstanceAdministration(x_DocumentSubstanceMood.Intent); // Intent (order) to administer
	    retVal.setTemplateId(new LIST<II>());
	    for(String templateId : templateIds)
	    {
	    	retVal.getTemplateId().add(new II(templateId));
	    }
	    return retVal;
    }

	/**
	 * Create a substance administration from an observation
	 */
	public SubstanceAdministration createSubstanceAdministration(List<String> templateIds, Obs sourceObs) {
		SubstanceAdministration retVal = new SubstanceAdministration();

		// Set the mood code
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());

    	retVal.setMoodCode(x_DocumentSubstanceMood.Eventoccurrence);
	    
	    // Template IDS
	    if(templateIds.size() > 0)
	    {
		    retVal.setTemplateId(new LIST<II>());
		    for(String templateId : templateIds)
		    {
		    	retVal.getTemplateId().add(new II(templateId));
		    }
	    }

	    // Identifiers
	    retVal.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getId().toString())));
	    if(sourceObs.getAccessionNumber() != null && !sourceObs.getAccessionNumber().isEmpty())
	    	retVal.getId().add(this.m_cdaDataUtil.parseIIFromString(sourceObs.getAccessionNumber()));
	    
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
	    // This is the time that the medication was taken
    	IVL<TS> effectiveTimePeriod = new IVL<TS>();
    	SXCM<TS> effectiveTimeInstant = new SXCM<TS>();
    	ISetComponent<TS> frequencyExpression = null, 
    			effectiveTime = null;
    	
	    // Effective time and extended observation properties
	    if(extendedObs != null)
	    {
	    	if(extendedObs.getObsRepeatNumber() != null)
	    		retVal.setRepeatNumber(new INT(extendedObs.getObsRepeatNumber()));
	    	if(extendedObs.getObsMood() != null)
	    	{
		    	CS<String> moodCode = this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsMood(), x_DocumentSubstanceMood.Eventoccurrence.getCodeSystem(), CS.class); 
		    	retVal.setMoodCode(new CS<x_DocumentSubstanceMood>(new x_DocumentSubstanceMood(moodCode.getCode(), x_DocumentSubstanceMood.Eventoccurrence.getCodeSystem())));
	    	}
	    	
	    	// status?
	    	if(extendedObs.getObsStatus() != null)
	    	{
	    		CS<String> status = this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsStatus(), ActStatus.Aborted.getCodeSystem(), CS.class);
	    		retVal.setStatusCode(new CS<ActStatus>(new ActStatus(status.getCode(), ActStatus.Cancelled.getCodeSystem())));
	    	}
	    	// The first effective time component
	    	if(extendedObs.getObsDatetime() != null &&
	    			extendedObs.getObsStartDate() == null &&
	    			extendedObs.getObsEndDate() == null)
	    	{
	    		effectiveTimePeriod.setValue(this.m_cdaDataUtil.createTS(extendedObs.getObsDatetime()));
	    		effectiveTimeInstant.setValue(this.m_cdaDataUtil.createTS(extendedObs.getObsDatetime()));
	    	}
	    	else
	    	{
	    		effectiveTimePeriod.setValue(null);
		    	if(extendedObs.getObsStartDate() != null)
		    		effectiveTimePeriod.setLow(this.m_cdaDataUtil.createTS(extendedObs.getObsStartDate()));
		    	if(extendedObs.getObsEndDate() != null)
		    		effectiveTimePeriod.setHigh(this.m_cdaDataUtil.createTS(extendedObs.getObsEndDate()));
			}
	    	
	    	// Null ?
	    	if(extendedObs.getObsDatePrecision() == 0)
	    		effectiveTimePeriod.setNullFlavor(NullFlavor.Unknown);
	    	
	    	// Set precision
	    	if(effectiveTimePeriod.getValue() != null)
	    	{
	    		effectiveTimePeriod.getValue().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    		effectiveTimeInstant.getValue().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	}
	    	if(effectiveTimePeriod.getLow() != null)
	    		effectiveTimePeriod.getLow().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	if(effectiveTimePeriod.getHigh() != null)
	    		effectiveTimePeriod.getHigh().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	
	    	//retVal.getEffectiveTime().add(effectiveTimePeriod);
	    }
	    else
	    {
	    	retVal.setStatusCode(ActStatus.Completed);
	    }
	    
	    // Now sub-observations
	    List<Obs> componentObs = this.m_service.getObsGroupMembers(sourceObs);
	    Collections.sort(componentObs, new Comparator<Obs>() {
			@Override
            public int compare(Obs o1, Obs o2) {
	           return o1.getId().compareTo(o2.getId());
            }
	    });
	    
	    int cSequence = 0;
	    
	    // Process the sub-observations
	    for(Obs component : componentObs)
	    {
	    	switch(component.getConcept().getId())
	    	{
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_TEXT:
	    			if(component.getValueText().startsWith("Instructions"))
	    			{
	    				Act instructionsAct = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence, new CD<String>("PINSTRUCT", CdaHandlerConstants.CODE_SYSTEM_IHE_ACT_CODE));
	    				instructionsAct.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_INSTRUCTIONS), new II(CdaHandlerConstants.ENT_TEMPLATE_MEDICATION_INSTRUCTIONS)));
	    				instructionsAct.setText(new ED(component.getValueText()));
	    			}
	    			else if(component.getValueText().startsWith("Pre-Condition"))
	    			{
	    				Precondition condition = new Precondition();
	    				condition.setCriterion(new Criterion());
	    				condition.getCriterion().setText(new ED(component.getValueText()));
	    				retVal.getPrecondition().add(condition);
	    			}
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_START_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimePeriod.setLow(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimePeriod.getLow().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STOP_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimePeriod.setHigh(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimePeriod.getHigh().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DATE:
	    			if(extendedObs == null)
	    			{
	    				effectiveTimeInstant.setValue(this.m_cdaDataUtil.createTS(component.getValueDate()));
	    				effectiveTimeInstant.getValue().setDateValuePrecision(TS.DAY);
	    			}
	    			break;
	    			
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DRUG:
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_DRUG:
	    			retVal.setConsumable(this.createConsumable(component.getValueDrug(), sourceObs.getConcept().getId()));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_IMMUNIZATION_SEQUENCE:
	    			Observation seriesObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
	    			seriesObservation.setStatusCode(ActStatus.Completed);
	    			seriesObservation.setCode(new CD<String>("30973-2", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Dose Number", null));
	    			seriesObservation.setValue(new INT(component.getValueNumeric().intValue()));
	    			retVal.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, seriesObservation));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_SIGN_SYMPTOM_PRESENT:
	    		    if(component.getValueCoded().getId().toString().equals(Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT)))
	    		    	retVal.setNegationInd(BL.TRUE);
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_QUANTITY: // Quantity is a numeric value and means we don't have / need units because of the form
	    			if(retVal.getDoseQuantity() == null)
	    				retVal.setDoseQuantity(new PQ(BigDecimal.valueOf(component.getValueNumeric()), null));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_SUPPLY:
	    		{
	    			
	    			EntryRelationship supplyRelationship = new EntryRelationship();
	    			Supply supply = new Supply();
	    			supply.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_SUPPLY_ACTIVITY), new II(CdaHandlerConstants.ENT_TEMPLATE_SUPPLY)));
	    			supplyRelationship.setClinicalStatement(supply);
	    			retVal.getEntryRelationship().add(supplyRelationship);
	    			
	    			// Obs for processing extended properties
	    			ExtendedObs supplyExtended = Context.getService(CdaImportService.class).getExtendedObs(component.getId());
	    			if(supplyExtended != null)
	    			{
	    				if(supplyExtended.getObsRepeatNumber() != null)
	    					supply.setRepeatNumber(new INT(supplyExtended.getObsRepeatNumber()));
	    		    	if(supplyExtended.getObsMood() != null)
	    		    		supply.setMoodCode(this.m_oddMetadataUtil.getStandardizedCode(supplyExtended.getObsMood(), x_ActMoodDocumentObservation.Definition.getCodeSystem(), CS.class));
	    			}
	    			
	    		    // Identifiers
	    		    retVal.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), component.getId().toString())));
	    		    if(component.getAccessionNumber() != null && !component.getAccessionNumber().isEmpty())
	    		    	retVal.getId().add(this.m_cdaDataUtil.parseIIFromString(component.getAccessionNumber()));
	    		    
	    			// Add author data
	    			supply.getAuthor().add(this.createAuthorPointer(component));
	    			
	    			// Process children
	    			List<Obs> supplyChildren = this.m_service.getObsGroupMembers(component);
	    			for(Obs supplyComponent : supplyChildren)
	    			{
	    				switch(supplyComponent.getConcept().getId())
	    				{
	    					case CdaHandlerConstants.CONCEPT_ID_DATE_OF_EVENT:
	    						if(supply.getPerformer().size() == 0)
	    							supply.getPerformer().add(new Performer2());
	    						supply.getPerformer().get(0).setTime(this.m_cdaDataUtil.createTS(supplyComponent.getValueDate()));
	    						supply.getPerformer().get(0).getTime().getValue().setDateValuePrecision(TS.DAY);
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_PROVIDER_NAME:
	    						if(supply.getPerformer().size() == 0)
	    							supply.getPerformer().add(new Performer2());
	    						// Get the provider
	    						Provider provider = Context.getProviderService().getProviderByIdentifier(supplyComponent.getValueText());
	    						if(provider != null)
	    							supply.getPerformer().get(0).setAssignedEntity(this.m_cdaDataUtil.createAssignedEntity(provider));
								break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DISPENSED:
	    						if(supply.getQuantity() == null)
	    							supply.setQuantity(new PQ(new BigDecimal(supplyComponent.getValueNumeric()), null));
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STRENGTH:
	    						IVL<PQ> quantity = this.parseDoseQuantity(supplyComponent.getValueText());
	    						if(quantity != null && quantity.getValue() != null)
	    							supply.setQuantity(quantity.getValue());
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_TREATMENT_NUMBER:
	    						supplyRelationship.setSequenceNumber(supplyComponent.getValueNumeric().intValue());
	    						break;
	    					case CdaHandlerConstants.CONCEPT_ID_MEDICATION_DRUG:
	    						Consumable cons = this.createConsumable(supplyComponent.getValueDrug(), sourceObs.getConcept().getId());
	    						supply.setProduct(new Product(cons.getManufacturedProduct()));
	    						break;
    						default:
    							throw new RuntimeException("Don't understand how to represent medication supply observation");
	    							
	    				}
	    			}
	    			break;
	    		}
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_FREQUENCY:
	    			switch(component.getValueCoded().getId())
	    			{
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE:
	    					frequencyExpression = null;
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("30"), "min"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_8_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("8"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_12_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("12"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_24_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("24"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_36_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("36"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_48_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("48"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_72_HOURS:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("72"), "h"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY:
	    					frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal("1"), "d"));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_AT_BEDTIME:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.HourOfSleep, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY_EVENING:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BetweenDinnerAndSleep, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_ONCE_DAILY_MORNING:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeBreakfast, null);
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_TWICE_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("12"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_THRICE_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("8"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_FOUR_TIMES_DAILY_AFTER_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.AfterMeal, new IVL<PQ>(new PQ(new BigDecimal("6"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_TWICE_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("12"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_THRICE_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("8"), "h")));
	    					break;
	    				case CdaHandlerConstants.MEDICATION_FREQUENCY_FOUR_TIMES_DAILY_BEFORE_MEALS:
	    					frequencyExpression = new EIVL<TS>(DomainTimingEvent.BeforeMeal, new IVL<PQ>(new PQ(new BigDecimal("6"), "h")));
	    					break;
    					default:
    						if(component.getValueCoded().getId() > CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS && component.getValueCoded().getId() < CdaHandlerConstants.MEDICATION_FREQUENCY_8_HOURS)
    							frequencyExpression = new PIVL<TS>(null, new PQ(new BigDecimal(component.getValueCoded().getId() - CdaHandlerConstants.MEDICATION_FREQUENCY_30_MINS), "h"));
    						else
    						{
    							EIVL<TS> other = new EIVL<TS>();
    							CV<String> domainTimingEvent = this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), DomainTimingEvent.AfterBreakfast.getCodeSystem(), CV.class);
    							other.setEvent(new CS<DomainTimingEvent>(FormatterUtil.fromWireFormat(domainTimingEvent.getCode(), DomainTimingEvent.class)));
    							frequencyExpression = other;
    						}
    						break;
	    			}
    				break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_STRENGTH: // use strength
	    			// Strength is measured as a IVL<PQ> which is serialized to a string when stored in OpenMRS
	    			// This string is in the following format:
	    			// 0[.##] XX - Exact dose
	    			// {0[.##] XX .. } - At least X dose
	    			// {0[.##] XX .. 0.[##] YY} - Between X and Y dose
	    			// { .. 0.[##] YY} - AT most Y dose
	    			retVal.setDoseQuantity(this.parseDoseQuantity(component.getValueText()));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY: // A sub-observation
	    		{
	    			SubstanceAdministration statement = this.createSubstanceAdministration(templateIds, component);
	    			EntryRelationship entryRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.HasComponent, BL.TRUE);
	    			entryRelation.setSequenceNumber(++cSequence);
	    			entryRelation.setClinicalStatement(statement);
	    			retVal.getEntryRelationship().add(entryRelation);
	    			break;
	    		}
	    		case CdaHandlerConstants.CONCEPT_ID_MEDICATION_FORM:
	    			retVal.setAdministrationUnitCode(this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_SNOMED, CE.class));
	    			break;
	    		case CdaHandlerConstants.CONCEPT_ID_PROCEDURE:
					retVal.setCode(this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), null, CD.class));
					break;
	    		default:
	    			// The codes that need to be determined at runtime
	    			try {
	                    if(component.getConcept().getId().equals(this.m_conceptUtil.getOrCreateRMIMConcept(CdaHandlerConstants.RMIM_CONCEPT_NAME_ROUTE_OF_ADM, null).getId()))
	                    	retVal.setRouteCode(this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_ROUTE_OF_ADMINISTRATION, CE.class));
	                    else if(component.getConcept().getId().equals(this.m_conceptUtil.getOrCreateRMIMConcept(CdaHandlerConstants.RMIM_CONCEPT_NAME_REASON, null)))
	                    {
	                    	EntryRelationship reasonRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.HasReason, BL.TRUE);
	                    	Act reasonAct = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence);
	                    	reasonAct.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_INTERNAL_REFERENCE)));
	                    	reasonAct.setId(SET.createSET(this.m_cdaDataUtil.parseIIFromString(component.getValueText())));
	                    	reasonRelation.setClinicalStatement(reasonAct);
	                    	retVal.getEntryRelationship().add(reasonRelation);
	                    }
	                    else
	                    	throw new RuntimeException("Don't understand how to represent medication component observation");
                    }
                    catch (DocumentImportException e) {
	                    // TODO Auto-generated catch block
	                    log.error("Error generated", e);
                    	throw new RuntimeException("Don't understand how to represent medication component observation",e);
                    }
	    	}

	    }
	    
    	// We have medication history  
    	if(sourceObs.getConcept().getId().equals(CdaHandlerConstants.CONCEPT_ID_MEDICATION_HISTORY))
    	{
    		retVal.getEffectiveTime().add(effectiveTimePeriod);
    		if(frequencyExpression != null)
    		{
    			((SXCM<TS>)frequencyExpression).setOperator(SetOperator.Intersect); 
    			retVal.getEffectiveTime().add(frequencyExpression);
    		}
    	}
    	else
    		retVal.getEffectiveTime().add(effectiveTimeInstant);

	    return retVal;
    }

	/**
	 * Parse dose quantity
	 */
	private IVL<PQ> parseDoseQuantity(String valueText) {
			Pattern regexPattern = Pattern.compile(REGEX_IVL_PQ);
			Matcher match = regexPattern.matcher(valueText);
			IVL<PQ> retVal = null;
			
			if(match.matches())
			{
				// Group 1 and 2 are the value and dose and 3 and 4 are another
				PQ group1 = null;
				if(match.group(1) != null && !match.group(1).isEmpty())
					group1 = new PQ(new BigDecimal(match.group(1)), match.group(2));
				if(match.groupCount() > 3 && match.group(3) != null && !match.group(3).isEmpty())
				{
					PQ group2 = new PQ(new BigDecimal(match.group(3)), match.group(4));
					// Range
					retVal = new IVL<PQ>(group1, group2);
				}
				else if(valueText.contains("{"))
					retVal = new IVL<PQ>(group1, null);
				else
					retVal = new IVL<PQ>(group1);
				
			}
			else
				throw new RuntimeException(String.format("Can't understand value %s", valueText));
	
			return retVal;
	}

	/**
	 * Create a consumable
	 */
	private Consumable createConsumable(Drug valueDrug, int containerConcent) {
		// Create the product
		Consumable consumable = new Consumable();
		ManufacturedProduct product = new ManufacturedProduct(RoleClassManufacturedProduct.ManufacturedProduct);
		product.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_PRODUCT), new II(CdaHandlerConstants.ENT_TEMPLATE_PRODCUT)));

		Material manufacturedMaterial = new Material();
		product.setManufacturedDrugOrOtherMaterial(manufacturedMaterial);
		// Drug code
		CE<String> drugCode = this.m_oddMetadataUtil.getStandardizedCode(valueDrug.getConcept(), CdaHandlerConstants.CODE_SYSTEM_RXNORM, CE.class);
		if(drugCode != null && drugCode.isNull())
		{
			CE<String> cvxCode = this.m_oddMetadataUtil.getStandardizedCode(valueDrug.getConcept(), CdaHandlerConstants.CODE_SYSTEM_CVX, CE.class);
			if(!cvxCode.isNull())
				manufacturedMaterial.setCode(cvxCode);
		}
		if(manufacturedMaterial.getCode() == null)
			manufacturedMaterial.setCode(drugCode);
		
		// Now get the drug from the concept
		if(valueDrug.getName() != null)
			manufacturedMaterial.setName(new EN(Arrays.asList(new ENXP(valueDrug.getName()))));

		consumable.setManufacturedProduct(product);
		return consumable;
    }

	/**
	 * Unknown drug treatment
	 */
	public SubstanceAdministration createNoSubstanceAdministration(List<String> templateIds) {
		SubstanceAdministration retVal = new SubstanceAdministration(x_DocumentSubstanceMood.Eventoccurrence);
		retVal.setTemplateId(new LIST<II>());
		for(String id : templateIds)
			retVal.getTemplateId().add(new II(id));
		
		retVal.getEffectiveTime().add(new IVL<TS>(new TS(), TS.now()));
		((IVL<TS>)retVal.getEffectiveTime().get(0)).getLow().setNullFlavor(NullFlavor.Unknown);
		
		retVal.setCode(new CD<String>("182904002", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Drug Treatment Unknown", null));
		
		retVal.setStatusCode(ActStatus.Completed);
		
		retVal.setId(SET.createSET(new II()));
		retVal.getId().get(0).setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setDoseQuantity(new PQ());
		retVal.getDoseQuantity().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setAdministrationUnitCode(new CE<String>());
		retVal.getAdministrationUnitCode().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setConsumable(new Consumable());
		retVal.getConsumable().setManufacturedProduct(new ManufacturedProduct());
		retVal.getConsumable().getManufacturedProduct().setManufacturedDrugOrOtherMaterial(new Material());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().setCode(new CE<String>());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode().setNullFlavor(NullFlavor.NotApplicable);
		
		return retVal;
    }
	
}
