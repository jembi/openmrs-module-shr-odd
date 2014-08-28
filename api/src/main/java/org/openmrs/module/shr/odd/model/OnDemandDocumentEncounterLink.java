package org.openmrs.module.shr.odd.model;

import org.openmrs.BaseOpenmrsData;
import org.openmrs.Encounter;


/**
 * Associates an encounter with a document registration
 */
public class OnDemandDocumentEncounterLink extends BaseOpenmrsData
{
	
	// Identifier of the association
	private Integer linkId;
	// The odd registration to which this association belongs
	private OnDemandDocumentRegistration registration;
	// Gets the encounter 
	private Encounter encounter;
	
	/**
	 * Gets the id of the ODD encounter association
	 * @see org.openmrs.OpenmrsObject#getId()
	 */
	@Override
    public Integer getId() {
        return this.linkId;
    }

	/**
	 * Sets the id of the ODD encounter association
	 * @see org.openmrs.OpenmrsObject#setId(java.lang.Integer)
	 */
	@Override
    public void setId(Integer id) {
		this.linkId = id;
    }

	/**
	 * Gets the registration to which the association applies
	 * @return
	 */
    public OnDemandDocumentRegistration getRegistration() {
    	return registration;
    }

    /**
     * Sets the registration to which the association applies
     */
    public void setRegistration(OnDemandDocumentRegistration registration) {
    	this.registration = registration;
    }

	
    /** 
     * @return the encounter
     */
    public Encounter getEncounter() {
    	return encounter;
    }

	
    /**
     * @param encounter the encounter to set
     */
    public void setEncounter(Encounter encounter) {
    	this.encounter = encounter;
    }
	
    
	
}