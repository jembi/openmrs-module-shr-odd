package org.openmrs.module.shr.odd.api.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.Concept;
import org.openmrs.Encounter;
import org.openmrs.Obs;
import org.openmrs.Order;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.api.impl.BaseOpenmrsService;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.api.db.OnDemandDocumentDAO;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.generator.DocumentGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentEncounterLink;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.model.OnDemandDocumentType;
import org.springframework.transaction.annotation.Transactional;

/**
 * The On-demand document service implementation
 */
@Transactional
public class OnDemandDocumentServiceImpl extends BaseOpenmrsService implements OnDemandDocumentService {
	
	// Dao
	private OnDemandDocumentDAO dao;

	/**
	 * Save an on-demand document
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#saveOnDemandDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
    public OnDemandDocumentRegistration saveOnDemandDocument(OnDemandDocumentRegistration registrationEntry) {
		registrationEntry.setDateChanged(new Date());
		registrationEntry.setChangedBy(Context.getAuthenticatedUser());
		return this.dao.saveOnDemandDocumentRegistration(registrationEntry);
    }

	/**
	 * Generate an on-demand document
	 * @throws OnDemandDocumentException 
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#generateOnDemandDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	@Transactional(readOnly = true)
    public ClinicalDocument generateOnDemandDocument(OnDemandDocumentRegistration registrationEntry) throws OnDemandDocumentException {
		
		// Validate
		if(registrationEntry == null)
			throw new IllegalArgumentException("registrationEntry must not be null");
		else if(registrationEntry.getType() == null)
			throw new IllegalStateException("registrationEntry must carry type");
		
        // Now instantiate and generate
        DocumentGenerator generatorInstance = this.getDocumentGenerator(registrationEntry.getType());
        
        if(generatorInstance == null)
        	throw new OnDemandDocumentException("Could not create document generator");
        return generatorInstance.generateDocument(registrationEntry);
		
    }

	/**
	 * Determine if the ODD is registered
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#isOnDemandDocumentRegistered(java.lang.String)
	 */
	@Override
    public boolean isOnDemandDocumentRegistered(String accessionNumber) {
		return this.dao.getOnDemandDocumentRegistrationsByAccessionNumber(accessionNumber, true).size() != 0;
    }

	/**
	 * Get the on demand document registration by id
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentRegistrationById(java.lang.Integer)
	 */
	@Override
	@Transactional(readOnly = true)
    public OnDemandDocumentRegistration getOnDemandDocumentRegistrationById(Integer id) {
		return this.dao.getOnDemandDocumentRegistrationById(id);
    }

	/**
	 * Get on demand document registration by uuid
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentRegistrationByUuid(java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true)
    public OnDemandDocumentRegistration getOnDemandDocumentRegistrationByUuid(String uuid) {
		return this.dao.getOnDemandDocumentRegistrationByUuid(uuid);
    }

	/**
	 * Get on demand document registration by accession number
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentRegistrationByAccessionNumber(java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true)
    public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByAccessionNumber(String accessionNumber) {
		return this.dao.getOnDemandDocumentRegistrationsByAccessionNumber(accessionNumber, false);
    }

	/**
	 * Get on demand document registrations by patient
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentRegistrationsByPatient(org.openmrs.Patient)
	 */
	@Override
	@Transactional(readOnly = true)
    public List<OnDemandDocumentRegistration> getOnDemandDocumentRegistrationsByPatient(Patient patient) {
		return this.dao.getOnDemandDocumentRegistrationsByPatient(patient, false);
    }

	/**
	 * Get an on-demand document registration type by uuid
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentTypeByUuid(java.lang.String)
	 */
	@Override
	@Transactional(readOnly = true)
    public OnDemandDocumentType getOnDemandDocumentTypeByUuid(String uuid) {
		return this.dao.getOnDemandDocumentTypeByUuid(uuid);
    }

	/**
	 * Save an on-demand document type
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#saveOnDemandDocumentType(org.openmrs.module.shr.odd.model.OnDemandDocumentType)
	 */
	@Override
    public OnDemandDocumentType saveOnDemandDocumentType(OnDemandDocumentType documentType) {
		documentType.setDateChanged(new Date());
		documentType.setChangedBy(Context.getAuthenticatedUser());
		return this.dao.saveOnDemandDocumentType(documentType);
    }

	/**
	 * Get encounters related to an on-demand document registration
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getOnDemandDocumentEncounters(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	@Transactional(readOnly = true)
    public List<OnDemandDocumentEncounterLink> getOnDemandDocumentEncounters(OnDemandDocumentRegistration oddRegistration) {
		return this.dao.getOnDemandDocumentEncounterLinks(oddRegistration, false);
    }

	
    /**
     * @param dao the dao to set
     */
    public void setDao(OnDemandDocumentDAO dao) {
    	this.dao = dao;
    }

    /**
     * Get obs group members
     * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getObsGroupMembers(org.openmrs.Obs)
     */
	@Override
	@Transactional(readOnly = true)
    public List<Obs> getObsGroupMembers(Obs group) {
		return this.dao.getObsGroupMembers(group);
    }

	/**
	 * Get obs group members
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getObsGroupMembers(org.openmrs.Obs, java.util.List)
	 */
	@Override
	@Transactional(readOnly = true)
    public List<Obs> getObsGroupMembers(Obs group, List<Concept> concept) {
	    return this.dao.getObsGroupMembers(Arrays.asList(group), concept);
    }
	
	/**
	 * Get obs group members
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getObsGroupMembers(org.openmrs.Obs, java.util.List)
	 */
	@Override
	@Transactional(readOnly = true)
    public List<Obs> getObsGroupMembers(List<Obs> group, List<Concept> concept) {
	    return this.dao.getObsGroupMembers(group, concept);
    }

	/**
	 * Get the document generator
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getDocumentGenerator(org.openmrs.module.shr.odd.model.OnDemandDocumentType)
	 */
	@Override
    public DocumentGenerator getDocumentGenerator(OnDemandDocumentType type) {
        
        try {
        	Class<? extends DocumentGenerator> clazz= (Class<? extends DocumentGenerator>)Context.loadClass(type.getJavaClassName());
	        // Now instantiate and generate
	        return clazz.newInstance();
        }
        catch (Exception e) {
        	return null;
        }

    }

	/**
	 * Get the obs group members of any of the provided obs
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getObsGroupMembers(java.util.List)
	 */
	@Override
    public List<Obs> getObsGroupMembers(List<Obs> sectionObs) {
		return this.dao.getObsGroupMembers(sectionObs);
    }

	/**
	 * Get all orders for the listed encounters
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getEncounterOrders(java.util.List)
	 */
	@Override
    public List<Order> getEncounterOrders(List<Encounter> docEncounters) {
		return this.dao.getEncounterOrders(docEncounters);
    }

	/**
	 * Get all orders of the specified type in the specified encounters
	 * @see org.openmrs.module.shr.odd.api.OnDemandDocumentService#getEncounterOrders(java.util.List, java.lang.Class)
	 */
	@Override
    public List<Order> getEncounterOrders(List<Encounter> docEncounters, Class<? extends Order> orderType) {
		return this.dao.getEncounterOrders(docEncounters, orderType);
    }
	
	
}
