package org.openmrs.module.shr.odd.exception;

/**
 * Represents an exception caused by the generation or registration of an ODD
 */
public class OnDemandDocumentException extends Exception {

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
    /**
     * Creates a new OnDemandDocumentException with the specified cause and message
     * @param message
     * @param e
     */
	public OnDemandDocumentException(String message, Exception cause) { super(message, cause); }
}
