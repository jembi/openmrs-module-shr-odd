package org.openmrs.module.shr.odd.util;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.marc.everest.datatypes.AD;
import org.marc.everest.datatypes.ADXP;
import org.marc.everest.datatypes.ANY;
import org.marc.everest.datatypes.AddressPartType;
import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.EntityNamePartType;
import org.marc.everest.datatypes.EntityNameUse;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.INT;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.ON;
import org.marc.everest.datatypes.PN;
import org.marc.everest.datatypes.PQ;
import org.marc.everest.datatypes.PostalAddressUse;
import org.marc.everest.datatypes.REAL;
import org.marc.everest.datatypes.SC;
import org.marc.everest.datatypes.TEL;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.TelecommunicationsAddressUse;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.doc.StructDocTextNode;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CS;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedAuthor;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssignedEntity;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AssociatedEntity;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.AuthoringDevice;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.CustodianOrganization;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organization;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.OrganizationPartOf;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Participant1;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.PatientRole;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Person;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.RecordTarget;
import org.marc.everest.rmim.uv.cdar2.vocabulary.AdministrativeGender;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ContextControl;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ParticipationType;
import org.marc.everest.rmim.uv.cdar2.vocabulary.RoleClassAssociative;
import org.marc.everest.rmim.uv.cdar2.vocabulary.RoleClassPart;
import org.marc.everest.rmim.uv.cdar2.vocabulary.RoleStatus;
import org.marc.everest.xml.XMLStateStreamReader;
import org.openmrs.Concept;
import org.openmrs.ConceptDatatype;
import org.openmrs.ConceptNumeric;
import org.openmrs.ImplementationId;
import org.openmrs.Location;
import org.openmrs.LocationAttribute;
import org.openmrs.Obs;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.PersonAddress;
import org.openmrs.PersonAttribute;
import org.openmrs.PersonName;
import org.openmrs.Provider;
import org.openmrs.Relationship;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.processor.util.OpenmrsConceptUtil;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.util.OpenmrsConstants;

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
	private final OpenmrsConceptUtil m_conceptUtil = OpenmrsConceptUtil.getInstance();
	
	// NOK codes
	private static final List<String> s_nextOfKinRelations = Arrays.asList("MTH", "FTH", "GRMTH", "GRFTH", "SIB", "CHILD",
	    "AUNT", "UNCLE", "PGRMTH", "MGRMTH", "PGRFTH", "MGRFTH", "SON", "DAU", "BRO", "SIS", "DOMPART", "FAMMEMB");
	
	private final Pattern m_idPattern = Pattern.compile(m_oddConfiguration.getIdRegex(), Pattern.CASE_INSENSITIVE);

	protected Log log = LogFactory.getLog(getClass());
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
			if(retVal.getExtension().equals("null"))
				retVal.setExtension(null);
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
		retVal.setAddr(this.createAddressSet(pvdr.getPerson()));
		
		// Get names
		retVal.setAssignedPerson(new Person(this.createNameSet(pvdr.getPerson())));
		
		PersonAttribute orgAttribute = pvdr.getPerson().getAttribute(CdaHandlerConstants.ATTRIBUTE_NAME_ORGANIZATION);
		if(orgAttribute != null)
			retVal.setRepresentedOrganization(this.createOrganization((Location)orgAttribute.getHydratedObject()));

		return retVal;
		
    }

	/**
	 * Creates a set of telecoms
	 */
	public SET<TEL> createTelecomSet(org.openmrs.Person person) {
		
		SET<TEL> retVal = new SET<TEL>();
		if(person != null)
			for(PersonAttribute patt : person.getAttributes())
			{
				if(patt.getAttributeType().getName().equals(CdaHandlerConstants.ATTRIBUTE_NAME_TELECOM))
				{
					TEL tel = new TEL();
					if(patt.getValue().contains(":"))
					{
						String[] parts = {
								patt.getValue().substring(0, patt.getValue().indexOf(":")),
								patt.getValue().substring(patt.getValue().indexOf(":") + 1)
						};
						tel.setValue(parts[1].trim());
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
		
		if(retVal.size() == 0)
		{
			TEL nullTel = new TEL();
			nullTel.setNullFlavor(NullFlavor.NoInformation);
			retVal.add(nullTel);
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
			//midPart.setQualifier(SET.createSET(new CS<EntityNamePartQualifier>(EntityNamePartQualifier.Middle)));
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
		
		if(addr.getAddress3() != null)
			retVal.getPart().add(2, new ADXP(addr.getAddress3(), AddressPartType.AddressLine));
		if(addr.getAddress4() != null)
			retVal.getPart().add(3, new ADXP(addr.getAddress4(), AddressPartType.AddressLine));
		if(addr.getAddress5() != null)
			retVal.getPart().add(4, new ADXP(addr.getAddress5(), AddressPartType.AddressLine));

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
			new II(this.m_cdaConfiguration.getProviderRoot(), pvdr.getId().toString()),
			new II(this.m_cdaConfiguration.getUserRoot(), Context.getUserService().getUsersByPerson(pvdr.getPerson(), false).get(0).getId().toString())
		));

		
		// Set telecom
		if(pvdr.getPerson() != null)
		{
			retVal.setTelecom(this.createTelecomSet(pvdr.getPerson()));
			
			// Get the address
			retVal.setAddr(this.createAddressSet(pvdr.getPerson()));
			
			// Get names
			retVal.setAssignedAuthorChoice(new Person(this.createNameSet(pvdr.getPerson())));
			
			PersonAttribute orgAttribute = pvdr.getPerson().getAttribute(CdaHandlerConstants.ATTRIBUTE_NAME_ORGANIZATION);
			if(orgAttribute != null)
				retVal.setRepresentedOrganization(this.createOrganization((Location)orgAttribute.getHydratedObject()));
		}
		else
			retVal.setAssignedAuthorChoice(new AuthoringDevice(null, new SC(pvdr.getName()), null, null));
		return retVal;
    }

	/**
	 * Create organization from location
	 */
	public Organization createOrganization(Location location) {
		Organization retVal = new Organization();
		
		// Identifiers
		retVal.setId(new SET<II>());
		LocationAttribute externalId = this.m_metaDataUtil.getLocationAttribute(location, CdaHandlerConstants.ATTRIBUTE_NAME_EXTERNAL_ID);
		if(externalId != null)
			retVal.getId().add(this.parseIIFromString(externalId.getValue().toString()));
		retVal.getId().add(new II(this.m_cdaConfiguration.getLocationRoot(), location.getId().toString()));
		
		// Name , etc. ?
		if(location.getName() != null)
			retVal.setName(SET.createSET(new ON((EntityNameUse)null, Arrays.asList(new ENXP(location.getName())))));
		
		// Address
		retVal.setAddr(SET.createSET(this.createAddressSet(location)));

		// TODO:
		retVal.setTelecom(SET.createSET(new TEL()));
		retVal.getTelecom().get(0).setNullFlavor(NullFlavor.NoInformation);
		
		if(location.getParentLocation() != null)
		{
			retVal.setAsOrganizationPartOf(new OrganizationPartOf());
			retVal.getAsOrganizationPartOf().setClassCode(RoleClassPart.Part);
			retVal.getAsOrganizationPartOf().setId(new SET<II>());
			externalId = this.m_metaDataUtil.getLocationAttribute(location.getParentLocation(), CdaHandlerConstants.ATTRIBUTE_NAME_EXTERNAL_ID);
			if(externalId != null)
				retVal.getAsOrganizationPartOf().getId().add(this.parseIIFromString(externalId.getValue().toString()));
			retVal.getAsOrganizationPartOf().getId().add(new II(this.m_cdaConfiguration.getLocationRoot(), location.getParentLocation().getId().toString()));
			
			if(location.getParentLocation().getRetired())
				retVal.getAsOrganizationPartOf().setStatusCode(RoleStatus.Terminated);
			else
				retVal.getAsOrganizationPartOf().setStatusCode(RoleStatus.Active);
		}
		
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
			// Set Id
			retVal.getAssignedAuthor().setId(SET.createSET(new II(this.m_cdaConfiguration.getShrRoot(), implementation.getImplementationId())));
		}
		AuthoringDevice device = new AuthoringDevice();
		device.setSoftwareName(new SC("OpenSHR"));
		device.setManufacturerModelName(new SC(OpenmrsConstants.OPENMRS_VERSION));
		retVal.getAssignedAuthor().setAssignedAuthorChoice(device);
		
		// Get location of the device?
		Location shrLocation = Context.getLocationService().getDefaultLocation();
		if(shrLocation != null)
		{
			retVal.getAssignedAuthor().setAddr(SET.createSET(this.createAddressSet(shrLocation)));
			retVal.getAssignedAuthor().setTelecom(SET.createSET(new TEL()));
			retVal.getAssignedAuthor().getTelecom().get(0).setNullFlavor(NullFlavor.NoInformation);
		}
		return retVal;

    }
	

	/**
	 * Create address set
	 */
	private AD createAddressSet(Location location) {
		AD retVal = AD.fromSimpleAddress(null, location.getAddress1(), location.getAddress2(), location.getCityVillage(), location.getStateProvince(), location.getCountry(), location.getPostalCode());
		
		if(location.getAddress3() != null)
			retVal.getPart().add(2, new ADXP(location.getAddress3(), AddressPartType.AddressLine));
		if(location.getAddress4() != null)
			retVal.getPart().add(3, new ADXP(location.getAddress4(), AddressPartType.AddressLine));
		if(location.getAddress5() != null)
			retVal.getPart().add(4, new ADXP(location.getAddress5(), AddressPartType.AddressLine));

		if(retVal.getPart().size() == 0)
			retVal.setNullFlavor(NullFlavor.NoInformation);
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
			retVal.setName(new ON());
			retVal.setAddr(new AD());
			retVal.setTelecom(new TEL());
			retVal.getName().setNullFlavor(NullFlavor.NoInformation);
			retVal.getAddr().setNullFlavor(NullFlavor.NoInformation);
			retVal.getTelecom().setNullFlavor(NullFlavor.NoInformation);
		}
		else
		{
			retVal.setName(new ON());
			retVal.getName().getParts().add(new ENXP(shrLocation.getName()));
			// TODO: Get a root assigned for OpenMRS implementation IDs? Or make the id long enough for an OID
			LocationAttribute idAttribute = this.m_metaDataUtil.getLocationAttribute(shrLocation, CdaHandlerConstants.ATTRIBUTE_NAME_EXTERNAL_ID);
			if(idAttribute != null)
				retVal.setId(SET.createSET(
					this.parseIIFromString(idAttribute.getValue().toString()),
					new II(this.m_cdaConfiguration.getLocationRoot(), shrLocation.getId().toString())));
			else
				retVal.setId(SET.createSET(
					new II(this.m_cdaConfiguration.getLocationRoot(), shrLocation.getId().toString())));

			retVal.setAddr(this.createAddressSet(shrLocation));
			// TODO
			retVal.setTelecom(new TEL());
			retVal.getTelecom().setNullFlavor(NullFlavor.NoInformation);
		}
		return retVal;
	}

	/**
	 * Create the record target
	 */
	public RecordTarget createRecordTarget(Patient patient) {
		RecordTarget retVal = new RecordTarget(ContextControl.OverridingPropagating);
		PatientRole patientRole = new PatientRole();
		org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Patient hl7Patient = new org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Patient();
		
		retVal.setPatientRole(patientRole);
		patientRole.setPatient(hl7Patient);
		// Identifiers
		patientRole.setId(new SET<II>());
		for(PatientIdentifier pid : patient.getActiveIdentifiers())
		{
			II ii = new II(pid.getIdentifierType().getName(), pid.getIdentifier());
			try
			{
				if(!patientRole.getId().contains(ii))
					patientRole.getId().add(ii);
			}
			catch(Exception e)
			{
				log.error(e);
			}
		}
		
		II meId = new II(this.m_cdaConfiguration.getPatientRoot(), patient.getId().toString());
		if(!patientRole.getId().contains(meId))
			patientRole.getId().add(meId);
		
		// Address?
		patientRole.setAddr(this.createAddressSet(patient));
		
		// Telecom?
		patientRole.setTelecom(this.createTelecomSet(patient));
		
		// Marital status?
		PersonAttribute civilStatusCode = patient.getAttribute(CdaHandlerConstants.ATTRIBUTE_NAME_CIVIL_STATUS);
		if(civilStatusCode != null)
			hl7Patient.setMaritalStatusCode(this.m_metaDataUtil.getStandardizedCode((Concept)civilStatusCode.getHydratedObject(), CdaHandlerConstants.CODE_SYSTEM_MARITAL_STATUS, CE.class));
			
		// Names
		hl7Patient.setName(this.createNameSet(patient));
		
		// Gender and birth
		hl7Patient.setAdministrativeGenderCode(new AdministrativeGender(patient.getGender(), AdministrativeGender.Male.getCodeSystem()));
		hl7Patient.setBirthTime(this.createTS(patient.getBirthdate()));
		
		if(patient.getBirthdateEstimated())
			hl7Patient.getBirthTime().setDateValuePrecision(TS.YEAR);
		else
			hl7Patient.getBirthTime().setDateValuePrecision(TS.DAY);
		
		return retVal;
    }

	/**
	 * Create a TS from a date
	 */
	public TS createTS(Date date) {
		if(date == null)
		{
			TS retVal = new TS();
			retVal.setNullFlavor(NullFlavor.NoInformation);
			return retVal;
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		TS retVal = new TS(cal);
		return retVal;

    }

	/**
	 * Related person
	 */
	public Participant1 createRelatedPerson(Relationship relatedPerson, Patient recordTarget) {
		Participant1 retVal = new Participant1();
		
		retVal.setTypeCode(ParticipationType.IND);
		// Now we want to expose the related person
		retVal.setTime(this.createTS(relatedPerson.getStartDate()), this.createTS(relatedPerson.getEndDate()));
		if(retVal.getTime().getLow().isNull() && retVal.getTime().getHigh().isNull()) // collapse null flavor
			retVal.getTime().setNullFlavor(NullFlavor.NoInformation);
		
		
		// Now we want to expose the person
		retVal.setAssociatedEntity(new AssociatedEntity());
		retVal.getAssociatedEntity().setCode(this.parseCodeFromString(relatedPerson.getRelationshipType().getDescription(), CE.class));
		
		if(recordTarget == relatedPerson.getPersonA())
			retVal.getAssociatedEntity().getCode().setDisplayName(relatedPerson.getRelationshipType().getbIsToA());
		else
			retVal.getAssociatedEntity().getCode().setDisplayName(relatedPerson.getRelationshipType().getaIsToB());
		
		if(s_nextOfKinRelations.contains(retVal.getAssociatedEntity().getCode().getCode()))
			retVal.getAssociatedEntity().setClassCode(RoleClassAssociative.NextOfKin);
		else
			retVal.getAssociatedEntity().setClassCode(RoleClassAssociative.PersonalRelationship);
		
		// Now for the entity themselves
		org.openmrs.Person relatedToPerson = relatedPerson.getPersonA();
		if(relatedToPerson.getId().equals(recordTarget))
			relatedToPerson = relatedPerson.getPersonB();
		
		// Create telecoms
		retVal.getAssociatedEntity().setTelecom(this.createTelecomSet(relatedToPerson));
		
		// Set addresses
		retVal.getAssociatedEntity().setAddr(this.createAddressSet(relatedToPerson));
		
		// Set name
		retVal.getAssociatedEntity().setAssociatedPerson(new Person(this.createNameSet(relatedToPerson)));

		return retVal;
    }

	/**
	 * Create address set
	 */
	public SET<AD> createAddressSet(org.openmrs.Person person) {
		SET<AD> retVal = new SET<AD>();
		for(PersonAddress addr : person.getAddresses())
			retVal.add(this.createAD(addr));
		if(retVal.size() > 0)
			return retVal;
		AD nullAd = new AD();
		nullAd.setNullFlavor(NullFlavor.NoInformation);
		retVal.add(nullAd);
		return retVal;
	}

	/**
	 * Create address set
	 */
	public SET<PN> createNameSet(org.openmrs.Person person) {
		SET<PN> retVal = new SET<PN>();
		for(PersonName name : person.getNames())
			if(!name.getFamilyName().equals("*")) // HACK: Name is requiredso this is the hackery that I use to bypass it
				retVal.add(this.createPN(name));
		if(retVal.size() > 0)
			return retVal;
		return null;
	}

	/**
	 * Create text node from XML string data
	 */
	public StructDocNode createText(Obs obs) {

		//obs = Context.getObsService().getObs(obs.getId());
		if(obs.isComplex())
		{
			obs = Context.getObsService().getComplexObs(obs.getId(), null);
			byte[] data = (byte[]) obs.getComplexData().getData();
			XMLInputFactory fact = XMLInputFactory.newInstance();
			try
			{
				XMLStateStreamReader reader = new XMLStateStreamReader(fact.createXMLStreamReader(new ByteArrayInputStream(data)));
				
				// Go to an element
				while(!reader.isStartElement() && reader.hasNext())
					reader.next();
				
				StructDocElementNode elementNode = new StructDocElementNode();
				elementNode.readXml(reader);
				return elementNode;
			}
			catch(Exception e)
			{
				return new StructDocTextNode(new String(data));
			}
		}
		else
			return new StructDocTextNode(obs.getValueAsString(Context.getLocale()));
		
    }

	/**
	 * Scrub identifiers
	 */
	public StructDocNode scrubIDs(StructDocNode node) {
		if(node instanceof StructDocElementNode)
		{
			StructDocElementNode element = (StructDocElementNode)node;
			for(int i = 0; i < element.getChildren().size(); i++)
			{
				StructDocNode childNode = element.getChildren().get(i);
				if(childNode != null && "ID".equals(childNode.getName()))
					element.getChildren().remove(i);
				else if(childNode instanceof StructDocElementNode)
					this.scrubIDs(childNode);
			}
		}
		return node;
    }

	/**
	 * Create an observation VALUE from an Obs
	 */
	public ANY getObservationValue(Obs obs) {
		String conceptDatatypeUuid = obs.getConcept().getDatatype().getUuid();
		if(obs.getValueBoolean() != null)
			return new BL(obs.getValueAsBoolean());
		else if(obs.getValueCoded() != null)
			return this.m_metaDataUtil.getStandardizedCode(obs.getValueCoded(), null, CD.class);
		else if(obs.getValueComplex() != null)
		{
			obs = Context.getObsService().getComplexObs(obs.getId(), null);
			byte[] data = (byte[]) obs.getComplexData().getData();
			
			// Binary data?
			String mimeType = null,
					obsTitle = obs.getComplexData().getTitle();
			if(obsTitle.contains("--"))
			{
				if(obsTitle.contains("--"))
				{
					int sPos = obsTitle.indexOf("--") + 3,
							count = obsTitle.lastIndexOf(".") - sPos;
					mimeType = URLDecoder.decode(obsTitle.substring(sPos, count));
				}
			}
			
			ED retVal = new ED(data, mimeType);
			retVal.setData(data);
			return retVal;
			
		}
		else if(obs.getValueDate() != null)
		{
			
			TS retVal = this.createTS(obs.getValueDate());
			retVal.setDateValuePrecision(TS.DAY);
			return retVal;
		}
		else if(obs.getValueDatetime() != null)
			return this.createTS(obs.getValueDatetime());
		else if(ConceptDatatype.N_A_UUID.equals(conceptDatatypeUuid)) // This is most likely an indicator!
			return null; // TODO: indicators
		else if(obs.getValueNumeric() != null) // Numeric!
		{
			ConceptNumeric numConcept = Context.getConceptService().getConceptNumeric(obs.getConcept().getId());
			if(numConcept.getUnits() == "" || numConcept.getUnits() == null)
			{
				// Are there decimals?
				if(Math.ceil(obs.getValueNumeric()) == Math.floor(obs.getValueNumeric())) 
					return new INT(obs.getValueNumeric().intValue());
				else
					return new REAL(obs.getValueNumeric());
			}
			else
				return new PQ(BigDecimal.valueOf(obs.getValueNumeric()), this.m_conceptUtil.getUcumUnitCode(numConcept));
		}
		else if(obs.getValueText() != null)
		{
			// TEXT could be MO or RTO
			// TODO: Yeah, this could be sticky...
			return new ANY();
		}
		else
			return new ANY() {{ setNullFlavor(NullFlavor.Other); }};
		
    }

}
