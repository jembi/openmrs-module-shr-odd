package org.openmrs.module.shr.odd.util;

import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Person;
import org.openmrs.PersonAttribute;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;

/**
 * The On-Demand document metadata util
 */
public final class OddMetadataUtil {
	
	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static OddMetadataUtil s_instance;
	
	// Get the ODD service
	private final OnDemandDocumentService m_oddService = Context.getService(OnDemandDocumentService.class);
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	
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
		return this.m_oddService.getOnDemandDocumentTypeByUud(typeUuid);
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
	
}
