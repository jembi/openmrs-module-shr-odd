package org.openmrs.module.shr.odd.generator.document.impl;

import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component3;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.StructuredBody;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.odd.generator.section.impl.VitalSignsSectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * A Cda Generator for a Continuity of Care Document (CCD)
 */
public class CcdGenerator extends DocumentGeneratorImpl {

	/**
	 * Generate the CCD
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#generateDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration) {
		ClinicalDocument retVal = super.createHeader(oddRegistration);
		
		// CCD CONF-1
		retVal.setCode(new CE<String>("34133-9", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Summarization of Episode Note", null));
		
		// CCD Template ID (CCD CONF-7 & CONF-8)
		retVal.setTemplateId(LIST.createLIST(
			new II(CdaHandlerConstants.DOC_TEMPLATE_CCD)
		));
		
		// CCD body must be structured
		retVal.setComponent(new Component2(ActRelationshipHasComponent.HasComponent, BL.TRUE));
		retVal.getComponent().setBodyChoice(new StructuredBody());
		
		// Now add the required sections
		List<Component3> sections = super.generateSections(oddRegistration, 
			VitalSignsSectionGenerator.class
		);
		retVal.getComponent().getBodyChoiceIfStructuredBody().getComponent().addAll(sections);
		
		return retVal;
	}
	
}
