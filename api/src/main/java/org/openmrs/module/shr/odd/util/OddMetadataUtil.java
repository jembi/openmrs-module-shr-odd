package org.openmrs.module.shr.odd.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.SET;
import org.openmrs.Concept;
import org.openmrs.ConceptMap;
import org.openmrs.ConceptMapType;
import org.openmrs.ConceptReferenceTerm;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Provider;
import org.openmrs.ProviderAttribute;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;

/**
 * The On-Demand document metadata util
 */
public final class OddMetadataUtil {
	
	protected Log log = LogFactory.getLog(getClass());

	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static OddMetadataUtil s_instance;
	
	// Get the ODD service
	private final OnDemandDocumentService m_oddService = Context.getService(OnDemandDocumentService.class);
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	private final OpenmrsConceptUtil m_conceptUtil = OpenmrsConceptUtil.getInstance();
	
	
	/**
	 * Private ctor
	 */
	private OddMetadataUtil()
	{
		
	}
	
	/**
	 * Get instance of the ODD meta-data utility
	 */
	public static OddMetadataUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new OddMetadataUtil();
            }
		return s_instance;
	}
	
	/**
	 * Get the specified visit attribute
	 */
	public VisitAttribute getVisitAttribute(Visit visit, String attributeName)
	{
		for(VisitAttribute att : visit.getActiveAttributes())
			if(att.getAttributeType().getName().equals(attributeName))
				return att;
		return null;
	}
	
	/**
	 * Get or create an ODD type
	 */
	public OnDemandDocumentType getOddType(String typeUuid)
	{
		return this.m_oddService.getOnDemandDocumentTypeByUuid(typeUuid);
	}

	/**
	 * Get a location attribute
	 */
	public LocationAttribute getLocationAttribute(Location location, String attributeName) {
		for(LocationAttribute att : location.getActiveAttributes())
			if(att.getAttributeType().getName().equals(attributeName))
				return att;
		return null;
    }

	/**
	 * Get the provider attribute
	 */
	public ProviderAttribute getProviderAttribute(Provider pvdr, String attributeName) {
		for(ProviderAttribute att : pvdr.getActiveAttributes())
			if(att.getAttributeType().getName().equals(attributeName))
				return att;
		return null;
    }

	public <T extends CS> T getStandardizedCode(Concept value, String targetCodeSystem, Class<T> clazz) {
	    
		try {
	        T retVal = null;
	        
	        if(value == null)
	        {
	        	retVal = clazz.newInstance();
	        	retVal.setNullFlavor(NullFlavor.NoInformation);
	        	return retVal;
	        }
	        // First, we need to find the reference term that represents the most applicable
	        Queue<ConceptReferenceTerm> preferredCodes = new ArrayDeque<ConceptReferenceTerm>(),
	        		equivalentCodes = new ArrayDeque<ConceptReferenceTerm>();
	        
	        // Mappings
	        String targetCodeSystemName = this.m_conceptUtil.mapOidToConceptSourceName(targetCodeSystem);
	        for(ConceptMap mapping : value.getConceptMappings())
	        {
	        	if(mapping.getConceptMapType().getName().equalsIgnoreCase("SAME-AS"))
	        	{
	        		ConceptReferenceTerm candidateTerm = mapping.getConceptReferenceTerm();
	        		if(targetCodeSystem == null ||
	        				targetCodeSystemName.equals(candidateTerm.getConceptSource().getName()) ||
	        				targetCodeSystem.equals(candidateTerm.getConceptSource().getHl7Code()))
	        			preferredCodes.add(candidateTerm);
	        		else
	        			equivalentCodes.add(candidateTerm);
	        	}
	        }
	        
	        // Now that we have a term, let's see if we can select a preferred term
	        ConceptReferenceTerm preferredTerm = preferredCodes.poll();
	        if(preferredTerm == null) // No preferred terms!
	        {
	        	retVal = clazz.newInstance();
	        	retVal.setNullFlavor(NullFlavor.Other);
	        }
	        else
	        	retVal = this.createCode(preferredTerm, value, clazz);
        	
	        // Are there other preferred terms
        	if(retVal instanceof CE)
        	{
        		SET<CD<?>> translations = new SET<CD<?>>();
		        while(preferredCodes.peek() != null)
		        {
		        	preferredTerm = preferredCodes.poll();
		        	translations.add(this.createCode(preferredTerm, value, CD.class));
		        }

		        // Fallback to others if we're going for broke
		        if(retVal.isNull()) 
		        	while(equivalentCodes.peek() != null)
		        	{
		        		preferredTerm = equivalentCodes.poll();
		        		translations.add(this.createCode(preferredTerm, value, CD.class));
		        	}
		        
		        // Add translations if any
		        if(!translations.isEmpty())
		        	((CE) retVal).setTranslation(translations);
        	}
        	
        	return retVal;
        }
        catch (Exception e) {
        	log.error("Error creating code", e);
        	return null;
        }
	    
    }

	/**
	 * Create the actual code data from the referenceTerm
	 */
	private <T extends CS> T createCode(ConceptReferenceTerm referenceTerm, Concept originalConcept, Class<T> clazz) {
		try
		{
			T retVal = clazz.newInstance();
	    	retVal.setCode(referenceTerm.getCode());
	    	if(retVal instanceof CV)
	    	{
	    		((CV<?>)retVal).setDisplayName(referenceTerm.getDescription());
	    		if(originalConcept.getPreferredName(Context.getLocale()) != null)
	    			((CV<?>)retVal).setOriginalText(new ED(originalConcept.getPreferredName(Context.getLocale()).getName(), Context.getLocale().toLanguageTag()));
	    		((CV<?>)retVal).setCodeSystemName(referenceTerm.getConceptSource().getName());
	    		((CV<?>)retVal).setCodeSystem(this.m_conceptUtil.mapConceptSourceNameToOid(referenceTerm.getConceptSource().getName()));
	    	}
	    	
	    	return retVal;
		}
		catch(Exception e)
		{
			log.error("Error creating code", e);
			return null;
		}
    }
	
}
