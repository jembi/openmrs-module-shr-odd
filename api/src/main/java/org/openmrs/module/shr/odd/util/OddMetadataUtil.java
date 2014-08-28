package org.openmrs.module.shr.odd.util;

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
	 * Get or create an ODD type
	 */
	public OnDemandDocumentType getOddType(String typeUuid)
	{
		return this.m_oddService.getOnDemandDocumentTypeByUud(typeUuid);
	}
	
}
