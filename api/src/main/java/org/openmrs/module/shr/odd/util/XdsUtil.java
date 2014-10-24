package org.openmrs.module.shr.odd.util;

import java.util.Date;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.rim.AssociationType1;
import org.dcm4chee.xds2.infoset.rim.ClassificationType;
import org.dcm4chee.xds2.infoset.rim.ExternalIdentifierType;
import org.dcm4chee.xds2.infoset.rim.ExtrinsicObjectType;
import org.dcm4chee.xds2.infoset.rim.InternationalStringType;
import org.dcm4chee.xds2.infoset.rim.LocalizedStringType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectListType;
import org.dcm4chee.xds2.infoset.rim.RegistryObjectType;
import org.dcm4chee.xds2.infoset.rim.RegistryPackageType;
import org.dcm4chee.xds2.infoset.rim.SubmitObjectsRequest;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CV;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.configuration.OnDemandDocumentConfiguration;
import org.openmrs.module.shr.odd.contenthandler.OnDemandDocumentContentHandler;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.generator.DocumentGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.xdsbrepository.XDSbService;

/**
 * Xds Utility class
 */
public final class XdsUtil {
	
	
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static XdsUtil s_instance;
	private final OnDemandDocumentConfiguration m_configuration = OnDemandDocumentConfiguration.getInstance();
	private final CdaHandlerConfiguration m_cdaConfiguration = CdaHandlerConfiguration.getInstance();
	
	/**
	 * Private ctor
	 */
	private XdsUtil()
	{
		
	}
	
	/**
	 * Get instance of the XDS utility
	 */
	public static XdsUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new XdsUtil();
            }
		return s_instance;
	}
	
	/**
	 * Register odd with registry
	 * @throws JAXBException 
	 */
	public void registerDocumentSet(final OnDemandDocumentRegistration registration) throws Exception {
		XDSbService xdsService = Context.getService(XDSbService.class);
		OnDemandDocumentService oddService = Context.getService(OnDemandDocumentService.class);
		DocumentGenerator docGenerator = oddService.getDocumentGenerator(registration.getType());
		
		// TODO: Doesn't handle replacements
		
		// Create the extrinsic object data  
		SubmitObjectsRequest registryRequest = new SubmitObjectsRequest();
		registryRequest.setRegistryObjectList(new RegistryObjectListType());
		ExtrinsicObjectType oddRegistryObject = new ExtrinsicObjectType();
		// ODD
		oddRegistryObject.setId(String.format("Document%s", registration.getId().toString()));
		oddRegistryObject.setMimeType("text/xml");
//		oddRegistryObject.setObjectType(XDSConstants.UUID_XDSDocumentEntry);
		oddRegistryObject.setName(new InternationalStringType());
		oddRegistryObject.getName().getLocalizedString().add(new LocalizedStringType());
		oddRegistryObject.getName().getLocalizedString().get(0).setValue(registration.getTitle());
		
		// Add repository UUID
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID, this.m_configuration.getRepositoryUniqueId());
		
		// Get the earliest time something occurred and the latest
		Date lastEncounter = new Date(0),
				firstEncounter = new Date();
		for(OnDemandDocumentEncounterLink el : registration.getEncounterLinks())
		{
			if(el.getEncounter().getVisit().getStartDatetime().before(firstEncounter))
				firstEncounter = el.getEncounter().getVisit().getStartDatetime();
			if(el.getEncounter().getVisit().getStopDatetime().after(lastEncounter))
				lastEncounter = el.getEncounter().getVisit().getStopDatetime();
		}
		
		TS firstEncounterTs = CdaDataUtil.getInstance().createTS(firstEncounter),
				lastEncounterTs = CdaDataUtil.getInstance().createTS(lastEncounter),
				creationTimeTs = TS.now();
		
		firstEncounterTs.setDateValuePrecision(TS.MINUTENOTIMEZONE);
		lastEncounterTs.setDateValuePrecision(TS.MINUTENOTIMEZONE);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SERVICE_START_TIME, firstEncounterTs.getValue());
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SERVICE_STOP_TIME, lastEncounterTs.getValue());
		
		oddRegistryObject.setObjectType("urn:uuid:34268e47-fdf5-41a6-ba33-82133c465248");
		
		// Add source patient information
		TS patientDob = CdaDataUtil.getInstance().createTS(registration.getPatient().getBirthdate());
		patientDob.setDateValuePrecision(TS.DAY);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, this.formatId(this.m_cdaConfiguration.getPatientRoot(), registration.getPatient().getId().toString()));
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_SOURCE_PATIENT_INFO,
			String.format("PID-3|%s", this.formatId(this.m_cdaConfiguration.getPatientRoot(), registration.getPatient().getId().toString())),
			String.format("PID-5|%s^%s^^^", registration.getPatient().getFamilyName(), registration.getPatient().getGivenName()),
			String.format("PID-7|%s", patientDob.getValue()),
			String.format("PID-8|%s", registration.getPatient().getGender())
			);
		InfosetUtil.addOrOverwriteSlot(oddRegistryObject, XDSConstants.SLOT_NAME_LANGUAGE_CODE, Context.getLocale().toLanguageTag());
		
		// Unique identifier
		this.addExtenalIdentifier(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_uniqueId, registration.getAccessionNumber());
		this.addExtenalIdentifier(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_patientId, this.getPatientIdentifier(registration.getPatient()));
		
		// Set classifications
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_classCode, "code", "codingScheme");
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_confidentialityCode, "1.3.6.1.4.1.21367.2006.7.101", "Connect-a-thon confidentialityCodes");
		CV<String> formatCode = CdaDataUtil.getInstance().parseCodeFromString(registration.getType().getFormatCode(), CV.class);
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_formatCode, formatCode.getCode(), formatCode.getCodeSystem());
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_healthCareFacilityTypeCode, "Not Available", "Connect-a-thon healthcareFacilityTypeCodes");
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_practiceSettingCode, "Not Available", "Connect-a-thon practiceSettingCodes");
		this.addCodedValueClassification(oddRegistryObject, XDSConstants.UUID_XDSDocumentEntry_typeCode, docGenerator.getDocumentTypeCode().getCode(), docGenerator.getDocumentTypeCode().getCodeSystemName());
		
		// Create the submission set
		TS now = TS.now();
		now.setDateValuePrecision(TS.SECONDNOTIMEZONE);
		
		RegistryPackageType regPackage = new RegistryPackageType();
		regPackage.setId(String.format("SubmissionSet%s", registration.getId().toString()));
		InfosetUtil.addOrOverwriteSlot(regPackage, XDSConstants.SLOT_NAME_SUBMISSION_TIME, now.getValue());
		regPackage.setName(oddRegistryObject.getName());
		this.addCodedValueClassification(regPackage, XDSConstants.UUID_XDSSubmissionSet_contentTypeCode, docGenerator.getDocumentTypeCode().getCode(), docGenerator.getDocumentTypeCode().getCodeSystem());
		
		// Submission set external identifiers

		this.addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_uniqueId, registration.getAccessionNumber() + ".1." + now.getValue());
		this.addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_sourceId, registration.getAccessionNumber());
		this.addExtenalIdentifier(regPackage, XDSConstants.UUID_XDSSubmissionSet_patientId, this.getPatientIdentifier(registration.getPatient()));
		
		// Add the eo to the submission
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<ExtrinsicObjectType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0","ExtrinsicObject"),
					ExtrinsicObjectType.class,
					oddRegistryObject
				)
			);
		
		// Add the package to the submission
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<RegistryPackageType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0","RegistryPackage"),
					RegistryPackageType.class,
					regPackage
				)
			);
		
		// Add classification for the submission set
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<ClassificationType>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Classification"), 
					ClassificationType.class, 
					new ClassificationType() {{
						setId("cl01");
						setClassifiedObject(String.format("SubmissionSet%s", registration.getId().toString()));
						setClassificationNode(XDSConstants.UUID_XDSSubmissionSet);
					}}
				)
			);
		
		// Add an association
		AssociationType1 association = 	new AssociationType1();
		association.setId("as01");
		association.setAssociationType("HasMember");
		association.setSourceObject(String.format("SubmissionSet%s", registration.getId().toString()));
		association.setTargetObject(String.format("Document%s", registration.getId().toString()));
		InfosetUtil.addOrOverwriteSlot(association, XDSConstants.SLOT_NAME_SUBMISSIONSET_STATUS, "Original");
		registryRequest.getRegistryObjectList().getIdentifiable().add(
			new JAXBElement<AssociationType1>(
					new QName("urn:oasis:names:tc:ebxml-regrep:xsd:rim:3.0", "Association"), 
					AssociationType1.class, 
					association)
			);

		
		try {
	        xdsService.registerDocument(registration.getAccessionNumber(), OnDemandDocumentContentHandler.class, registryRequest);
        }
        catch (Exception e) {
	        throw new OnDemandDocumentException(e.getMessage(), e);
        }
    }

	/**
	 * Add external identifier
	 */
	private ExternalIdentifierType addExtenalIdentifier(final RegistryObjectType classifiedObj, final String uuid, final String id) throws JAXBException {
	
		ExternalIdentifierType retVal = new ExternalIdentifierType();
		retVal.setRegistryObject(classifiedObj.getId());
		retVal.setIdentificationScheme(uuid);
		retVal.setValue(id);
		retVal.setId(String.format("eid%s", classifiedObj.getExternalIdentifier().size()));
		classifiedObj.getExternalIdentifier().add(retVal);
		return retVal;
	}
	
	/**
	 * Create a codified value classification
	 * @throws JAXBException 
	 */
	private ClassificationType addCodedValueClassification(final RegistryObjectType classifiedObj, final String uuid, final String code, final String scheme) throws JAXBException {
	    ClassificationType retVal = new ClassificationType();
	    retVal.setClassifiedObject(classifiedObj.getId());
	    retVal.setClassificationScheme(uuid);
	    retVal.setNodeRepresentation(code);
	    
	    retVal.setId(String.format("cl%s",retVal.hashCode()));
	    InfosetUtil.addOrOverwriteSlot(retVal, "codingScheme", scheme);
	    
	    classifiedObj.getClassification().add(retVal);
	    
	    return retVal;
    }

	/**
	 * Format identifier for XDS meta-data
	 */
	private String formatId(String root, String extension)
	{
		return String.format("%s^^^&%s&ISO", extension, root);
	}
	
	/**
	 * Get the ECID identifier for the patient
	 */
	private String getPatientIdentifier(Patient patient) {
		for(PatientIdentifier pid : patient.getIdentifiers())
			if(pid.getIdentifierType().getName().equals(this.m_cdaConfiguration.getEcidRoot())) // prefer the ecid
				return this.formatId(pid.getIdentifierType().getName(), pid.getIdentifier());
		return String.format(this.m_cdaConfiguration.getPatientRoot(), patient.getId().toString());// use the local identifier as last effort!
    }
}
