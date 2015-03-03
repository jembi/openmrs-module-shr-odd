package org.openmrs.module.shr.odd.generator;

import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * Represents a class which is capable of generating a CDA
 */
public interface DocumentGenerator {
	
	/**
	 * Generates a document given the on-demand document registration entry
	 */
	public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration);
	
	/**
	 * Get the document type code
	 */
	public CE<String> getDocumentTypeCode();
}
