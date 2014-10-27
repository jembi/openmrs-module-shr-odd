package org.openmrs.module.shr.odd.generator.section.impl;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.UUID;
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
import org.marc.everest.datatypes.ST;
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
import org.marc.everest.interfaces.IEnumeratedVocabulary;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Consumable;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Criterion;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ExternalAct;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ExternalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ManufacturedProduct;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Material;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Performer2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Precondition;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Procedure;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Product;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Reference;
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
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipExternalReference;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentProcedureMood;
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
import org.openmrs.VisitAttribute;
import org.openmrs.VisitAttributeType;
import org.openmrs.activelist.ActiveListItem;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;
import org.openmrs.module.shr.cdahandler.order.ProcedureOrder;
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

	private static final CD<String> s_drugTreatmentUnknownCode = new CD<String>("182904002", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Drug Treatment Unknown", null);
	
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
		retVal.setId(new II(UUID.randomUUID()));
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
		retVal.setTemplateId(this.getTemplateIdList(templateId));
		
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
		
		retVal.setTemplateId(this.getTemplateIdList(templateId));
		
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
	    		if(obs != null && obs.getObsDatePrecision() == 0)
	    			eft.getLow().setNullFlavor(NullFlavor.Unknown);
	    		else if(obs != null)
	    			eft.getLow().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    }
	    if(activeListItem.getStopObs() != null)
	    {
	    	eft.setHigh(this.m_cdaDataUtil.createTS(activeListItem.getEndDate()));
	    	if(activeListItem.getStopObs() != null)
	    	{
	    		// Correct the precision of the dates
	    		ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(activeListItem.getStopObs().getId());
	    		if(obs != null && obs.getObsDatePrecision() == 0)
	    			eft.getHigh().setNullFlavor(NullFlavor.Unknown);
	    		else if(obs != null)
	    			eft.getHigh().setDateValuePrecision(obs.getObsDatePrecision());
	    	}
	    	
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
	    retVal.setTemplateId(this.getTemplateIdList(templateId));
	    
		Reference original = this.createReferenceToDocument(sourceObs);
		if(original != null) retVal.getReference().add(original);

	    // Add identifier
		retVal.setId(this.getIdentifierList(sourceObs));
	    
	    // Add the code
	    retVal.setCode(this.m_oddMetadataUtil.getStandardizedCode(sourceObs.getConcept(), targetCodeCodeSystem, CD.class));
	    
	    // Is there a creation time?
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
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

	    
	    // Set the status, mood, and effective time
    	retVal.setStatusCode(this.getStatusCode(sourceObs));
    	retVal.setEffectiveTime(this.getEffectiveTime(sourceObs));
    	retVal.setMoodCode(this.getMoodCode(sourceObs, x_ActMoodDocumentObservation.class));

	    // Extended observation stuff
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());
	    if(extendedObs != null)
	    	this.setExtendedObservationProperties(retVal, extendedObs);
	    
	    // Look for negation
	    List<Obs> negation = this.m_service.getObsGroupMembers(sourceObs, Arrays.asList(Context.getConceptService().getConcept(CdaHandlerConstants.CONCEPT_ID_SIGN_SYMPTOM_PRESENT)));
	    if(negation.size() > 0 && negation.get(0).getValueCoded().getId().toString().equals(Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT)))
	    	retVal.setNegationInd(BL.TRUE);
	    	
	    try {
	        List<Obs> references = this.m_service.getObsGroupMembers(sourceObs, Arrays.asList(this.m_conceptUtil.getOrCreateRMIMConcept(CdaHandlerConstants.RMIM_CONCEPT_UUID_REFERENCE, new ST())));
	        if(references.size() > 0)
	        	for(Obs ref : references)
	        	{
	        		Reference refr = new Reference();
	        		ExternalDocument ed = new ExternalDocument();
	        		ed.setId(SET.createSET(this.m_cdaDataUtil.parseIIFromString(ref.getValueText())));
	        		refr.setTypeCode(x_ActRelationshipExternalReference.REFR);
	        		refr.setExternalActChoice(ed);
	        		retVal.getReference().add(refr);
	        	}
        }
        catch (DocumentImportException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
        }
	    
	    // Replacement?
	    if(sourceObs.getPreviousVersion() != null)
	    {
	    	Reference prevRef = new Reference(x_ActRelationshipExternalReference.RPLC);
	    	ExternalAct externalAct = new ExternalAct(new CD<String>("OBS"));
	    	externalAct.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getPreviousVersion().getId().toString())));
	    	if(sourceObs.getPreviousVersion().getAccessionNumber() != null)
	    		externalAct.getId().add(this.m_cdaDataUtil.parseIIFromString(sourceObs.getPreviousVersion().getAccessionNumber()));
	    	prevRef.setExternalActChoice(externalAct);
	    	retVal.getReference().add(prevRef);
	    }
	    	
	    return retVal;
	    
    }

	/**
	 * Get the identifier list
	 */
	protected SET<II> getIdentifierList(Obs sourceObs) {
		SET<II> retVal = new SET<II>();
	    if(sourceObs.getAccessionNumber() != null && !sourceObs.getAccessionNumber().isEmpty())
	    	retVal.add(this.m_cdaDataUtil.parseIIFromString(sourceObs.getAccessionNumber()));
	    retVal.add(new II(this.m_cdaConfiguration.getObsRoot(), sourceObs.getId().toString()));
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
    	
    }

	/**
	 * Get the mood code
	 */
	protected <T extends IEnumeratedVocabulary> CS<T> getMoodCode(Obs obs, Class<T> vocabulary) {
		 if(obs instanceof ExtendedObs)
		    {
		    	ExtendedObs extendedObs = (ExtendedObs)obs;
		    	CS<String> status = this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsMood(), x_DocumentProcedureMood.Eventoccurrence.getCodeSystem(), CS.class);
		    	if(status.isNull())
		    		return new CS<T>(FormatterUtil.fromWireFormat("EVN", vocabulary));
		    	else
		    		return new CS<T>(FormatterUtil.fromWireFormat(status.getCode(), vocabulary));
		    }
			 else
			 {
				 return new CS<T>(FormatterUtil.fromWireFormat("EVN", vocabulary));
			 }
    }

	/**
	 * Get the effective time
	 */
	protected IVL<TS> getEffectiveTime(Obs obs) {
		IVL<TS> retVal = new IVL<TS>();
		
		if(obs instanceof ExtendedObs)
		{
			ExtendedObs extendedObs = (ExtendedObs)obs;
	    	// status?
	    	if(extendedObs.getObsDatetime() != null &&
	    			extendedObs.getObsStartDate() == null &&
	    			extendedObs.getObsEndDate() == null)
	    		retVal.setValue(this.m_cdaDataUtil.createTS(extendedObs.getObsDatetime()));
	    	else
	    	{
	    		retVal.setValue(null);
		    	if(extendedObs.getObsStartDate() != null)
		    		retVal.setLow(this.m_cdaDataUtil.createTS(extendedObs.getObsStartDate()));
		    	if(extendedObs.getObsEndDate() != null)
		    		retVal.setHigh(this.m_cdaDataUtil.createTS(extendedObs.getObsEndDate()));
			}
	    	
	    	// Null ?
	    	if(extendedObs.getObsDatePrecision() == 0)
	    		retVal.setNullFlavor(NullFlavor.Unknown);
	    	
	    	// Set precision
	    	if(retVal.getValue() != null)
	    		retVal.getValue().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	if(retVal.getLow() != null)
	    		retVal.getLow().setDateValuePrecision(extendedObs.getObsDatePrecision());
	    	if(retVal.getHigh() != null)
	    		retVal.getHigh().setDateValuePrecision(extendedObs.getObsDatePrecision());
		}
		else
			retVal.setValue(this.m_cdaDataUtil.createTS(obs.getObsDatetime()));
		
		return retVal;
    }

	/**
	 * Get the status code of the object
	 */
	protected CS<ActStatus> getStatusCode(Obs obs) {
		 if(obs instanceof ExtendedObs)
	    {
	    	ExtendedObs extendedObs = (ExtendedObs)obs;
	    	CS<String> status = this.m_oddMetadataUtil.getStandardizedCode(extendedObs.getObsStatus(), ActStatus.Aborted.getCodeSystem(), CS.class);
	    	return new CS<ActStatus>(new ActStatus(status.getCode(), ActStatus.Completed.getCodeSystem()));
	    }
		 else
			 return new CS<ActStatus>(ActStatus.Completed);
    }

	/**
	 * Create an author node that points to correct information
	 */
	protected Author createAuthorPointer(BaseOpenmrsData sourceData) {
		Author retVal = new Author(ContextControl.OverridingPropagating);
		if(sourceData.getChangedBy() != null)
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateChanged()));
			Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(sourceData.getChangedBy().getPerson());
			Provider pvdr = providers.iterator().next();
			retVal.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(pvdr));
//			retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getChangedBy().getId().toString()))));
		}
		else
		{
			retVal.setTime(this.m_cdaDataUtil.createTS(sourceData.getDateCreated()));
			Collection<Provider> providers = Context.getProviderService().getProvidersByPerson(sourceData.getCreator().getPerson());
			Provider pvdr = providers.iterator().next();
			retVal.setAssignedAuthor(this.m_cdaDataUtil.createAuthorPerson(pvdr));
			//retVal.setAssignedAuthor(new AssignedAuthor(SET.createSET(new II(this.m_cdaConfiguration.getUserRoot(), sourceData.getCreator().getId().toString()))));
		}
		return retVal;
    }

	/**
	 * Generate the Level 3 content text
	 */
	protected SD generateLevel3Text(Section section)
	{
		SD retVal = new SD();
		Class<? extends ClinicalStatement> previousStatementType = null;
		StructDocElementNode context = null; 
		for(Entry ent : section.getEntry())
		{
			// Is this different than the previous?
			if(!ent.getClinicalStatement().getClass().equals(previousStatementType))
			{
				// Add existing context node before generating another
				if(context!=null)
					retVal.getContent().add(context);
				// Force the generation of new context
				context = null;
			}
			StructDocElementNode genNode = this.m_cdaTextUtil.generateText(ent.getClinicalStatement(), context, this.m_documentContext);
			
			// Set the context node
			if(context == null)
				context = genNode;
			previousStatementType = ent.getClinicalStatement().getClass();
			
			ent.setTypeCode(x_ActRelationshipEntry.DRIV);
		}
		if(context != null && !retVal.getContent().contains(context))
			retVal.getContent().add(context);
		return retVal;
	}

	/**
	 * Create a substance administration from a drug order
	 */
	public SubstanceAdministration createSubstanceAdministration(List<String> templateIds, DrugOrder data) {
	    SubstanceAdministration retVal = new SubstanceAdministration(x_DocumentSubstanceMood.Intent); // Intent (order) to administer
	    retVal.setTemplateId(this.getTemplateIdList(templateIds));
	    return retVal;
    }

	/**
	 * Create a substance administration from an observation
	 */
	public SubstanceAdministration createSubstanceAdministration(List<String> templateIds, Obs sourceObs) {
		
		SubstanceAdministration retVal = new SubstanceAdministration();

		Reference original = this.createReferenceToDocument(sourceObs);
		if(original != null) retVal.getReference().add(original);
		
		// Set the mood code
	    ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());

    	retVal.setMoodCode(x_DocumentSubstanceMood.Eventoccurrence);
	    
	    retVal.setTemplateId(this.getTemplateIdList(templateIds));

	    // Identifiers
	    retVal.setId(this.getIdentifierList(sourceObs));
	    
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));
	    
	    // This is the time that the medication was taken
    	IVL<TS> effectiveTimePeriod = new IVL<TS>();
    	SXCM<TS> effectiveTimeInstant = new SXCM<TS>();
    	ISetComponent<TS> frequencyExpression = null, 
    			effectiveTime = null;

    	retVal.setMoodCode(this.getMoodCode(sourceObs, x_DocumentSubstanceMood.class));
    	retVal.setStatusCode(this.getStatusCode(sourceObs));
    	
	    // Effective time and extended observation properties
	    if(extendedObs != null)
	    {
	    	if(extendedObs.getObsRepeatNumber() != null)
	    		retVal.setRepeatNumber(new INT(extendedObs.getObsRepeatNumber()));
	    	
	    	// Set times
	    	effectiveTimePeriod = this.getEffectiveTime(extendedObs);
	    	effectiveTimeInstant.setValue(effectiveTimePeriod.getValue());
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
	    			else if(retVal.getText() == null)
	    				retVal.setText(new ED(component.getValueText()));
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
	    			retVal.setId(this.getIdentifierList(sourceObs));
	    		    
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
					
					// Treatment is unknown?
					if(retVal.getCode().semanticEquals(s_drugTreatmentUnknownCode) == BL.TRUE)
						return null;
					break;
	    		default:
	    			// The codes that need to be determined at runtime
                    if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_ROUTE_OF_ADM))
                    	retVal.setRouteCode(this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_ROUTE_OF_ADMINISTRATION, CE.class));
                    else if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_REASON))
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

    	if(sourceObs.getComment() != null)
    		retVal.setText(new ED(sourceObs.getComment()));
	    return retVal;
    }

	/**
	 * Create a reference to a source document
	 */
	protected Reference createReferenceToDocument(Obs sourceObs) {
		
		Reference retVal = null;
		if(sourceObs.getEncounter().getVisit() != null)
		{
            try {
            	retVal = new Reference();
        		ExternalDocument ed = new ExternalDocument();
        		
            	VisitAttributeType vat = this.m_conceptUtil.getOrCreateVisitExternalIdAttributeType();
    			for(VisitAttribute attr : sourceObs.getEncounter().getVisit().getActiveAttributes())
    				if(attr.getAttributeType().equals(vat))
    					ed.setId(SET.createSET(this.m_cdaDataUtil.parseIIFromString(attr.getValue().toString())));

    			retVal.setTypeCode(x_ActRelationshipExternalReference.REFR);
    			retVal.setExternalActChoice(ed);

            }
            catch (DocumentImportException e) {
	            // TODO Auto-generated catch block
	            log.error("Error generated", e);
            }
		}
		
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
		product.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_PRODUCT), new II(CdaHandlerConstants.ENT_TEMPLATE_PRODUCT)));

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
		retVal.setTemplateId(this.getTemplateIdList(templateIds));
		
		retVal.getEffectiveTime().add(new IVL<TS>(new TS(), TS.now()));
		((IVL<TS>)retVal.getEffectiveTime().get(0)).getLow().setNullFlavor(NullFlavor.Unknown);
		
		retVal.setCode(s_drugTreatmentUnknownCode);
		
		retVal.setStatusCode(ActStatus.Completed);
		
		retVal.setId(SET.createSET(new II(UUID.randomUUID())));
//		retVal.getId().get(0).setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setDoseQuantity(new PQ());
		retVal.getDoseQuantity().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setAdministrationUnitCode(new CE<String>());
		retVal.getAdministrationUnitCode().setNullFlavor(NullFlavor.NotApplicable);
		
		retVal.setConsumable(new Consumable());
		retVal.getConsumable().setManufacturedProduct(new ManufacturedProduct());
		retVal.getConsumable().getManufacturedProduct().setTemplateId(this.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PRODUCT, CdaHandlerConstants.ENT_TEMPLATE_CCD_MEDICATION_PRODUCT)));
		retVal.getConsumable().getManufacturedProduct().setManufacturedDrugOrOtherMaterial(new Material());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().setCode(new CE<String>());
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode().setNullFlavor(NullFlavor.NotApplicable);
		retVal.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode().setOriginalText(new ED("Not Applicable"));
		
		retVal.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
		return retVal;
    }

	/**
	 * Create the procedure entry
	 */
	public Procedure createProcedure(List<String> templateIds, Obs sourceObs) {
		
		Procedure retVal = new Procedure();

	    // Identifiers
		retVal.setId(this.getIdentifierList(sourceObs));
	    
	    retVal.getAuthor().add(this.createAuthorPointer(sourceObs));

		Reference original = this.createReferenceToDocument(sourceObs);
		if(original != null) retVal.getReference().add(original);

		// Extended observations
		ExtendedObs extendedObs = Context.getService(CdaImportService.class).getExtendedObs(sourceObs.getId());
		
		// Set the mood code
		retVal.setTemplateId(this.getTemplateIdList(templateIds));
		retVal.setMoodCode(this.getMoodCode(sourceObs, x_DocumentProcedureMood.class));
		retVal.setStatusCode(this.getStatusCode(sourceObs));
		retVal.setEffectiveTime(this.getEffectiveTime(sourceObs));
		
		// Component obs
		for(Obs component : this.m_service.getObsGroupMembers(sourceObs))
		{
			switch(component.getConcept().getId())
			{
				case CdaHandlerConstants.CONCEPT_ID_PROCEDURE:
					retVal.setCode(this.m_oddMetadataUtil.getStandardizedCode(component.getConcept(), null, CD.class));
					break;
				case CdaHandlerConstants.CONCEPT_ID_PROCEDURE_DATE:
					if(extendedObs == null)
					{
						retVal.setEffectiveTime(this.m_cdaDataUtil.createTS(component.getValueDate()));
						retVal.getEffectiveTime().getValue().setDateValuePrecision(TS.DAY);
					}
					break;
				case CdaHandlerConstants.CONCEPT_ID_PROVIDER_NAME:
					// Get the provider
					Provider provider = Context.getProviderService().getProviderByIdentifier(component.getValueText());
					if(provider != null)
						retVal.getPerformer().add(new Performer2(this.m_cdaDataUtil.createAssignedEntity(provider)));
					break;
				case CdaHandlerConstants.CONCEPT_ID_PROCEDURE_HISTORY: // A sub-observation
	    		{
	    			Procedure statement = this.createProcedure(templateIds, component);
	    			EntryRelationship entryRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.HasComponent, BL.TRUE);
	    			entryRelation.setClinicalStatement(statement);
	    			retVal.getEntryRelationship().add(entryRelation);
	    			
	    			break;
	    		}
				default:
					if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_APPROACH_SITE))
					{
						if(retVal.getApproachSiteCode() == null)
							retVal.setApproachSiteCode(new SET<CD<String>>());
						retVal.getApproachSiteCode().add(this.m_oddMetadataUtil.getStandardizedCode(component.getValueCoded(), null, CD.class))
							;
					}
					else if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_TARGET_SITE))
					{
						if(retVal.getTargetSiteCode() == null)
							retVal.setTargetSiteCode(new SET<CD<String>>());
						retVal.getTargetSiteCode().add(this.m_oddMetadataUtil.getStandardizedCode(component.getConcept(), null, CD.class));
					}
					else if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_REASON))
					{
						EntryRelationship er = new EntryRelationship(x_ActRelationshipEntryRelationship.HasReason, BL.TRUE);
						er.setClinicalStatement(this.createInternalReference(component.getValueText()));
						retVal.getEntryRelationship().add(er);
					}
					else if(component.getConcept().getUuid().equals(CdaHandlerConstants.RMIM_CONCEPT_UUID_REFERENCE))
					{
						EntryRelationship er = new EntryRelationship(x_ActRelationshipEntryRelationship.HasComponent, BL.TRUE);
						er.setInversionInd(BL.TRUE);
						er.setClinicalStatement(this.createInternalReference(component.getValueText()));
						retVal.getEntryRelationship().add(er);
					}
					else
                    	throw new RuntimeException("Don't understand how to represent procedure component observation");

					break;
			}
		}
		

    	if(sourceObs.getComment() != null)
    		retVal.setText(new ED(sourceObs.getComment()));
	    return retVal;
		
    }

	/**
	 * Create an internal reference 
	 */
	private ClinicalStatement createInternalReference(String valueText) {
		Act retVal = new Act();
		
		retVal.setTemplateId(this.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_INTERNAL_REFERENCE)));
		II referencedObjectId = this.m_cdaDataUtil.parseIIFromString(valueText);
		retVal.setId(SET.createSET(referencedObjectId));
		retVal.setCode(new CD<String>());
		// TODO: Find out how to get this code?
		retVal.getCode().setNullFlavor(NullFlavor.NoInformation);
		
		return retVal;
    }

	/**
	 * Create procedure based on an order
	 */
	public Procedure createProcedure(List<String> asList, ProcedureOrder data) {

		// TODO Auto-generated method stub
	    return null;
    }
	
	/**
	 * Get the template id list
	 * Auto generated method comment
	 * 
	 * @param templateIds
	 * @return
	 */
	protected LIST<II> getTemplateIdList(List<String> templateIds)
	{
		LIST<II> retVal = null;
		if(templateIds.size() > 0)
		{
			retVal = new LIST<II>();
			for(String tplId : templateIds)
				retVal.add(new II(tplId));
		}
		return retVal;

	}

	/**
	 * Create a "no known problem" or "no known allergy" act
	 */
	protected Act createNoKnownProblemAct(List<String> templateIds, CD<String> code, CD<String> valueCode) {
		
		Act retVal = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence);
		retVal.setStatusCode(ActStatus.Completed);
		retVal.setTemplateId(this.getTemplateIdList(templateIds));
		retVal.setEffectiveTime(new TS(), TS.now());
		retVal.getEffectiveTime().getLow().setNullFlavor(NullFlavor.Unknown);
		retVal.setCode(new CD<String>());
		retVal.getCode().setNullFlavor(NullFlavor.NotApplicable);
		retVal.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());
		retVal.setId(SET.createSET(new II(UUID.randomUUID())));

		// Observation
		Observation probObs = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
		probObs.setStatusCode(ActStatus.Completed);
		probObs.setTemplateId(this.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION)));
		if(templateIds.contains(CdaHandlerConstants.ENT_TEMPLATE_ALLERGIES_AND_INTOLERANCES_CONCERN))
		{
			probObs.getTemplateId().add(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_ALERT_OBSERVATION));
			probObs.getTemplateId().add(new II(CdaHandlerConstants.ENT_TEMPLATE_ALLERGY_AND_INTOLERANCE_OBSERVATION));
		}
		probObs.setCode(code);
		probObs.setValue(valueCode);
		probObs.setEffectiveTime(new TS(), TS.now());
		probObs.getEffectiveTime().getLow().setNullFlavor(NullFlavor.Unknown);
		probObs.setId(SET.createSET(new II(UUID.randomUUID())));
		probObs.getAuthor().add(this.m_cdaDataUtil.getOpenSHRInstanceAuthor());

		retVal.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE, probObs));
		retVal.getEntryRelationship().get(0).setInversionInd(BL.FALSE);
		return retVal;
		
    }

	
	/**
	 * Create an external references act
	 */
	protected Act createExternalReferenceAct(Obs data) {
		 Act retVal = new Act(x_ActClassDocumentEntryAct.Act, x_DocumentActMood.Eventoccurrence);
		retVal.setTemplateId(this.getTemplateIdList(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_EXTERNAL_REFERENCES_ENTRY)));
		retVal.setId(SET.createSET(new II(UUID.randomUUID())));
		retVal.setCode(new CD<String>());
		retVal.getCode().setNullFlavor(NullFlavor.NotApplicable);
		
		for(Obs subObs : this.m_service.getObsGroupMembers(data))
		{
			Reference ref = new Reference();
			ref.setTypeCode(this.m_oddMetadataUtil.getStandardizedCode(subObs.getConcept(), x_ActRelationshipExternalReference.ELNK.getCodeSystem(), CS.class));
			ref.setExternalActChoice(new ExternalDocument());
			ref.getExternalActChoiceIfExternalDocument().setId(SET.createSET(this.m_cdaDataUtil.parseIIFromString(subObs.getValueText())));
			if(subObs.getComment() != null)
				ref.getExternalActChoiceIfExternalDocument().setText(new ED(subObs.getComment()));
			retVal.getReference().add(ref);
		}
		
		if(data.getComment() != null)
			retVal.setText(new ED(data.getComment()));
		
		return retVal;
	}

}
