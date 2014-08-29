package org.openmrs.module.shr.odd.generator.impl;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * A Cda Generator for a Continuity of Care Document (CCD)
 */
public class CcdGenerator extends CdaGeneratorImpl {
	
	@Override
	public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration) {
		// TODO Auto-generated method stub
		return super.createHeader(oddRegistration);
	}
	
}
