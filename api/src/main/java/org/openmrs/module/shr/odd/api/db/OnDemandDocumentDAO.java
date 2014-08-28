package org.openmrs.module.shr.odd.api.db;

import java.util.List;

import org.openmrs.Patient;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;

/**
 * The OnDemandDocument persistance DAO
 */
public interface OnDemandDocumentDAO {
	
	/**
	 * Save an on-demand document registration
	 */
	public OnDemandDocumentRegistration saveOnDemandDocumentRegistration(OnDemandDocumentRegistration document);
	/**
	 * Gets an on-demand document registration by its identifier
	 */
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationById(Integer id);
	/**
	 * Gets an on demand document registration by its uuid
	 */
	public OnDemandDocumentRegistration getOnDemandDocumentRegistrationByUuid(String uuid);
	/**
	 * Gets an on demand document registration by its accession number
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByAccessionNumber(String accessionNumber, boolean includeVoided);
	/**
	 * Gets an on-demand document registration by patient
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient, boolean includeVoided);
	/**
	 * Get an on demand document type by its identifier
	 */
	public OnDemandDocumentType getOnDemandDocumentTypeById(Integer id);
	/**
	 * Gets an on demand document type by its uuid
	 */
	public OnDemandDocumentType getOnDemandDocumentTypeByUuid(String uuid);
	/**
	 * Saves an on-demand document type
	 */
	public OnDemandDocumentType saveOnDemandDocumentType(OnDemandDocumentType documentType);
	/**
	 * Gets an OnDemandDocument<>Encounter link
	 */
	public List<OnDemandDocumentEncounterLink> getOnDemandDocumentEncounterLinks(OnDemandDocumentRegistration document, boolean includeVoided);
	/**
	 * Saves an on-demand document to encounter link
	 */
	public OnDemandDocumentEncounterLink saveOnDemandDocumentEncounterLink(OnDemandDocumentEncounterLink link);
	
}
