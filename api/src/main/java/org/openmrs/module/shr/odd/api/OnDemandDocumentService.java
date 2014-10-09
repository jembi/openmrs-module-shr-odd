package org.openmrs.module.shr.odd.api;

import java.util.List;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.generator.DocumentGenerator;
import org.openmrs.module.shr.odd.generator.document.impl.ApsGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.springframework.transaction.annotation.Transactional;

/**
 * The OnDemandDocumentService provides methods for retrieving and storing OnDemandDocument instances
 */
@Transactional
public interface OnDemandDocumentService extends OpenmrsService {
	
	/**
	 * Saves an OnDemandDocument to the datastore
	 */
	public OnDemandDocumentRegistration saveOnDemandDocument(OnDemandDocumentRegistration registrationEntry);

	/**
	 * Generates the on-demand document
	 * @throws OnDemandDocumentException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ClinicalDocument generateOnDemandDocument(OnDemandDocumentRegistration registrationEntry) throws OnDemandDocumentException;

	/**
	 * Gets the generator for the specified type of document
	 */
	public DocumentGenerator getDocumentGenerator(OnDemandDocumentType type);
	
	/**
	 * Returns true if the on-demand document is already registered
	 */
	public boolean isOnDemandDocumentRegistered(String uuid);

	/**
	 * Gets the on-demand document registration entry by the ID
	 */
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationById(Integer id);

	/**
	 * Gets the on-demand document registration entry by the UUID
	 */
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationByUuid(String uuid);

	/**
	 * Gets the on-demand document registrations by patient
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient);
	
	/**
	 * Gets the on-demand document type by its UUID
	 */
	public OnDemandDocumentType getOnDemandDocumentTypeByUuid(String uuid);

	/**
	 * Saves the on-demand document type
	 */
	public OnDemandDocumentType saveOnDemandDocumentType(OnDemandDocumentType documentType);

	/**
	 * Gets the encounters which can be used to generate data for the ODD
	 */
	public List<OnDemandDocumentEncounterLink> getOnDemandDocumentEncounters(OnDemandDocumentRegistration oddRegistration);

	/**
	 * Get an on-demand document registration number by accession number
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByAccessionNumber(String accessionNumber);

	/**
	 * For some reason the getGroupMembers() doesn't correctly work
	 */
	public List<Obs> getObsGroupMembers(Obs group);
	
	/**
	 * For some reason the getGroupMembers() doesn't correctly work
	 */
	public List<Obs> getObsGroupMembers(Obs group, List<Concept> concept);
	
	/**
	 * For some reason the getGroupMembers() doesn't correctly work
	 */
	public List<Obs> getObsGroupMembers(List<Obs> group, List<Concept> concept);

	/**
	 * Get all obs group members within the specified group of obs
	 */
	public List<Obs> getObsGroupMembers(List<Obs> sectionObs);

	/**
	 * Get all orders from the specified encounters
	 */
	public List<Order> getEncounterOrders(List<Encounter> docEncounters);
	/**
	 * Get orders in the encounter of the specified type
	 */
	public List<Order> getEncounterOrders(List<Encounter> asList, Class<? extends Order> orderType);

	/**
	 * Get on-demand document registrations by patient and ODD type
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient,
                                                                                        OnDemandDocumentType oddType);
}
