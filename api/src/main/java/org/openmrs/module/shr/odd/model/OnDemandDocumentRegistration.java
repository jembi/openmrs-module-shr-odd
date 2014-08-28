package org.openmrs.module.shr.odd.model;

import java.util.HashSet;
import java.util.Set;

import org.openmrs.Auditable;
import org.openmrs.BaseOpenmrsData;
import org.openmrs.Patient;

/**
 * Represents the on-demand document id
 */
public class OnDemandDocumentRegistration extends BaseOpenmrsData implements Auditable {

	
	

	// The identifier of the registration
	private Integer oddId;
	// The patient to which the ODD applies
	private Patient patient;
	// The name of the class which generates the ODD
	private OnDemandDocumentType type;
	// Encounter links 
	private Set<OnDemandDocumentEncounterLink> encounterLinks = new HashSet<OnDemandDocumentEncounterLink>();
	// Accession number for the ODD
	private String accessionNumber;
	
	/**
	 * Gets the unique identifier for this registration
	 * @see org.openmrs.OpenmrsObject#getId()
	 */
	@Override
	public Integer getId() {
		return this.oddId;
	}
	
	/**
	 * Sets the id for this registration
	 * @see org.openmrs.OpenmrsObject#setId(java.lang.Integer)
	 */
	@Override
	public void setId(Integer value) {
		this.oddId = value;
	}

	
    /**
     * @return the patient
     */
    public Patient getPatient() {
    	return patient;
    }

	
    /**
     * @param patient the patient to set
     */
    public void setPatient(Patient patient) {
    	this.patient = patient;
    }

	
    /**
     * @return the type
     */
    public OnDemandDocumentType getType() {
    	return type;
    }

	
    /**
     * @param type the type to set
     */
    public void setType(OnDemandDocumentType type) {
    	this.type = type;
    }

	
    /**
     * @return the encounterLinks
     */
    public Set<OnDemandDocumentEncounterLink> getEncounterLinks() {
    	return encounterLinks;
    }

	
    /**
     * @param encounterLinks the encounterLinks to set
     */
    public void setEncounterLinks(Set<OnDemandDocumentEncounterLink> encounterLinks) {
    	this.encounterLinks = encounterLinks;
    }

	
    /**
     * @return the accessionNumber
     */
    public String getAccessionNumber() {
    	return accessionNumber;
    }

	
    /**
     * @param accessionNumber the accessionNumber to set
     */
    public void setAccessionNumber(String accessionNumber) {
    	this.accessionNumber = accessionNumber;
    }

}
