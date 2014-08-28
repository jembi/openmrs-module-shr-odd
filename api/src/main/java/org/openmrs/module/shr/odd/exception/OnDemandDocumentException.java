package org.openmrs.module.shr.odd.exception;

/**
 * Represents an exception caused by the generation or registration of an ODD
 */
public class OnDemandDocumentException extends RuntimeException {

	/**
     * Serialization id
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default ctor for the exception 
     */
    public OnDemandDocumentException() { }
    /**
     * Creates a new OnDemandDocumentException with the specified message
     * @param message A human readable message indicating why the exception was thrown
     */
    public OnDemandDocumentException(String message) { super(message); }
}
