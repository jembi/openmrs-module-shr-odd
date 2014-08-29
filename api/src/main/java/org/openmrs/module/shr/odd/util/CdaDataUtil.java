package org.openmrs.module.shr.odd.util;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openmrs.EncounterRole;
import org.openmrs.ImplementationId;
import org.openmrs.Location;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Visit;
import org.openmrs.VisitAttribute;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.openmrs.util.OpenmrsConstants;
import org.marc.everest.datatypes.*;
import org.marc.everest.datatypes.generic.*;
import org.marc.everest.exceptions.FormatterException;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedEntity;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AuthoringDevice;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.CustodianOrganization;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Person;

/**
 * The On-Demand document metadata util
 */
public final class CdaDataUtil {
	
	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static CdaDataUtil s_instance;
	
	// Cda Handler Configuration
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	private final OnDemandDocumentConfiguration m_oddConfiguration = OnDemandDocumentConfiguration.getInstance();
	private final OddMetadataUtil m_metaDataUtil = OddMetadataUtil.getInstance();
	
	private final Pattern m_idPattern = Pattern.compile(m_oddConfiguration.getIdRegex(), Pattern.CASE_INSENSITIVE);
	/**
	 * Private ctor
	 */
	private CdaDataUtil()
	{
		
	}
	
	/**
	 * Get instance of the ODD meta-data utility
	 */
	public static CdaDataUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new CdaDataUtil();
            }
		return s_instance;
	}
	

	/**
	 * Parse an II from a string
	 */
	public II parseIIFromString(String iiString)
	{
		II retVal = new II();
		Matcher matcher = this.m_idPattern.matcher(iiString);
		if(matcher.matches())
		{
			retVal.setExtension(matcher.group(1));
			retVal.setRoot(matcher.group(2));
		}
		return retVal;
	}
	

	/**
	 * Parse an II from a string
	 */
	public <T extends CS<?>> T parseCodeFromString(String codeString, Class<T> clazz)
	{
		Matcher matcher = this.m_idPattern.matcher(codeString);
		if(matcher.matches())
		{
            try {
	            T retVal = clazz.newInstance();
				retVal.setCode(matcher.group(1));
				if(retVal instanceof CV)
					((CV<?>)retVal).setCodeSystem(matcher.group(2));
				return retVal;
            }
            catch (Exception e) {
            	return null;
            }
		}
		else
		{
			T retVal;
            try {
	            retVal = clazz.newInstance();
				retVal.setNullFlavor(NullFlavor.Other);
				return retVal;
            }
            catch (Exception e) {
            	return null;
            }
		}
			
	}
	
	
	/**
	 * Create an assigned entity from the specified provider
	 */
	@SuppressWarnings("unchecked")
    public AssignedEntity createAssignedEntity(Provider pvdr) {
		AssignedEntity retVal = new AssignedEntity();
		
		// Get the ID
		retVal.setId(SET.createSET(
			this.parseIIFromString(pvdr.getIdentifier()),
			new II(this.m_cdaConfiguration.getProviderRoot(), pvdr.getId().toString())
		));

		// Telecoms
		retVal.setTelecom(this.createTelecomSet(pvdr.getPerson()));
		// Get the address 
		retVal.setAddr(new SET<AD>());
		for(PersonAddress addr : pvdr.getPerson().getAddresses())
			retVal.getAddr().add(this.createAD(addr));
		
		// Get names
		retVal.setAssignedPerson(new Person());
		retVal.getAssignedPerson().setName(new SET<PN>());
		for(PersonName name : pvdr.getPerson().getNames())
			retVal.getAssignedPerson().getName().add(this.createPN(name));
		
		return retVal;
    }

	/**
	 * Creates a set of telecoms
	 */
	private SET<TEL> createTelecomSet(org.openmrs.Person person) {
		SET<TEL> retVal = new SET<TEL>();
		for(PersonAttribute patt : person.getAttributes())
		{
			if(patt.getAttributeType().getName().equals(CdaHandlerConstants.ATTRIBUTE_NAME_TELECOM))
			{
				TEL tel = new TEL();
				if(patt.getValue().contains(":"))
				{
					String[] parts = patt.getValue().split(":");
					tel.setValue(parts[1]);
					try {
	                    tel.setUse((SET<CS<TelecommunicationsAddressUse>>) FormatterUtil.fromWireFormat(parts[0], AssignedEntity.class.getMethod("getTelecom", null).getGenericReturnType(), false));
                    }
                    catch (Exception e) {
	                    // Safe to ignore?
                    }
				}
				else
					tel.setValue(patt.getValue());
				if(patt.getVoided())
					tel.setUse(TelecommunicationsAddressUse.BadAddress);
				retVal.add(tel);
			}
		}
		return retVal;

    }

	/**
	 * Create a PN
	 */
	public PN createPN(PersonName name) {
		// Names
		PN retVal = new PN();

		if(name.getPreferred())
			retVal.setUse(SET.createSET(new CS<EntityNameUse>(EntityNameUse.Legal)));
		else
			retVal.setUse(SET.createSET(new CS<EntityNameUse>(EntityNameUse.Search)));
		
		// Middle name
		if(name.getMiddleName() != null)
		{
			ENXP midPart = new ENXP(name.getMiddleName(), EntityNamePartType.Given);
			midPart.setQualifier(SET.createSET(new CS<EntityNamePartQualifier>(EntityNamePartQualifier.Middle)));
			retVal.getParts().add(midPart);
		}

		// Family 
		if(name.getFamilyName() != null)
			retVal.getParts().add(new ENXP(name.getFamilyName(), EntityNamePartType.Family));
		
		// Family 
		if(name.getFamilyName2() != null)
			retVal.getParts().add(new ENXP(name.getFamilyName2(), EntityNamePartType.Family));
		
		// First
		if(name.getGivenName() != null)
			retVal.getParts().add(new ENXP(name.getGivenName(), EntityNamePartType.Given));

		// Middle name
		if(name.getMiddleName() != null)
		{
			ENXP midPart = new ENXP(name.getMiddleName(), EntityNamePartType.Given);
			midPart.setQualifier(SET.createSET(new CS<EntityNamePartQualifier>(EntityNamePartQualifier.Middle)));
			retVal.getParts().add(midPart);
		}

		return retVal;
		
    }

	/**
	 * Create an AD
	 */
	public AD createAD(PersonAddress addr) {
		
		AD retVal = AD.fromSimpleAddress(null, addr.getAddress1(), addr.getAddress2(), addr.getCityVillage(), addr.getStateProvince(), addr.getCountry(), addr.getPostalCode());
		if(addr.getVoided())
			retVal.setUse(SET.createSET(new CS<PostalAddressUse>(PostalAddressUse.BadAddress)));
		return retVal;
		
		
    }

	/**
	 * Creates an assigned author
	 */
	public AssignedAuthor createAuthorPerson(Provider pvdr) {
		AssignedAuthor retVal = new AssignedAuthor();
		
		// Get the ID
		retVal.setId(SET.createSET(
			this.parseIIFromString(pvdr.getIdentifier()),
			new II(this.m_cdaConfiguration.getProviderRoot(), pvdr.getId().toString())
		));

		// Set telecom
		retVal.setTelecom(this.createTelecomSet(pvdr.getPerson()));
		
		// Get the address 
		retVal.setAddr(new SET<AD>());
		for(PersonAddress addr : pvdr.getPerson().getAddresses())
			retVal.getAddr().add(this.createAD(addr));
		
		// Get names
		retVal.setAssignedAuthorChoice(new Person());
		retVal.getAssignedAuthorChoiceIfAssignedPerson().setName(new SET<PN>());
		for(PersonName name : pvdr.getPerson().getNames())
			retVal.getAssignedAuthorChoiceIfAssignedPerson().getName().add(this.createPN(name));
		
		return retVal;
    }

	/**
	 * Get the OpenMRS instance author
	 */
	public Author getOpenSHRInstanceAuthor() {
		Author retVal = new Author();
		retVal.setTime(TS.now());
		ImplementationId implementation = Context.getAdministrationService().getImplementationId();

		retVal.setAssignedAuthor(new AssignedAuthor());
		
		// Set name
		if(implementation == null)
		{
			II deviceId = new II();
			deviceId.setNullFlavor(NullFlavor.NoInformation);
			retVal.getAssignedAuthor().setId(SET.createSET(deviceId));
		}
		else
		{
			AuthoringDevice device = new AuthoringDevice();
			device.setSoftwareName(new SC("OpenSHR"));
			device.setManufacturerModelName(new SC(OpenmrsConstants.OPENMRS_VERSION));
			// Set Id
			// TODO: Get root for OpenMRS root oids
			retVal.getAssignedAuthor().setId(SET.createSET(new II(this.m_cdaConfiguration.getShrRoot(), implementation.getImplementationId())));
		}

		return retVal;

    }
	

	/**
	 * Get the custodian information
	 */
	public CustodianOrganization getCustodianOrganization()
	{
		CustodianOrganization retVal = new CustodianOrganization();
		Location shrLocation = Context.getLocationService().getDefaultLocation();
		
		// Set name
		if(shrLocation == null)
		{
			II deviceId = new II();
			deviceId.setNullFlavor(NullFlavor.NoInformation);
			retVal.setId(SET.createSET(deviceId));
		}
		else
		{
			retVal.setName(new ON());
			retVal.getName().getParts().add(new ENXP(shrLocation.getName()));
			// TODO: Get a root assigned for OpenMRS implementation IDs? Or make the id long enough for an OID
			II id = this.parseIIFromString(this.m_metaDataUtil.getLocationAttribute(shrLocation, CdaHandlerConstants.ATTRIBUTE_NAME_EXTERNAL_ID).getValue().toString());
			retVal.setId(SET.createSET(
				id,
				new II(this.m_cdaConfiguration.getLocationRoot(), shrLocation.getId().toString())));
		}
		return retVal;
	}
}
