package org.openmrs.module.shr.odd.generator.document.impl;

import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.module.shr.contenthandler.api.CodedValue;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * A CDA generator for an Antepartum Summary
 */
public class ApsGenerator extends DocumentGeneratorImpl {
	
	/**
	 * Generate the document
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#generateDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Get the document type code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getDocumentTypeCode()
	 */
	@Override
    public CE<String> getDocumentTypeCode() {
	    // TODO Auto-generated method stub
	    return null;
    }

}
