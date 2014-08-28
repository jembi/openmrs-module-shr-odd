package org.openmrs.module.shr.odd.api;

import java.util.List;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Patient;
import org.openmrs.api.OpenmrsService;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;

/**
 * The OnDemandDocumentService provides methods for retrieving and storing OnDemandDocument instances
 */
public interface OnDemandDocumentService extends OpenmrsService {
	
	/**
	 * Saves an OnDemandDocument to the datastore
	 */
	public OnDemandDocumentRegistration saveOnDemandDocument(OnDemandDocumentRegistration registrationEntry);

	/**
	 * Generates the on-demand document
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
	public ClinicalDocument generateOnDemandDocument(OnDemandDocumentRegistration registrationEntry) throws ClassNotFoundException, InstantiationException, IllegalAccessException;

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
	public OnDemandDocumentType getOnDemandDocumentTypeByUud(String uuid);

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
	
	
}
