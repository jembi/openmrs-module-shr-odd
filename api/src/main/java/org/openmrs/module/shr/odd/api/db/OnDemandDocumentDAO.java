package org.openmrs.module.shr.odd.api.db;

import java.util.List;

import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
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
	/**
	 * Get all obs members in the obs group
	 */
	public List<Obs> getObsGroupMembers(Obs containerObs);
	/**
	 * Get all obs members in the obs group
	 */
	public List<Obs> getObsGroupMembers(List<Obs> containerObs);
	/**
	 * Get all obs members in the obs group having the defined concept
	 */
	public List<Obs> getObsGroupMembers(List<Obs> containerObs, List<Concept> concept);
	/**
	 * Get the orders associated with the list of encounters
	 */
	public List<Order> getEncounterOrders(List<Encounter> encounters);
	/**
	 * Get encounter orders of the specified type
	 */
	public List<Order> getEncounterOrders(List<Encounter> encounters, Class<? extends Order> orderType);
	/**
	 * Get on-demand document registrations of a particular type for the specified patient
	 */
	public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient,
                                                                                        OnDemandDocumentType documentType,
                                                                                        boolean b);
	
}
