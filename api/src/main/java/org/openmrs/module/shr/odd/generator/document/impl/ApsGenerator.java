package org.openmrs.module.shr.odd.generator.document.impl;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component2;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.StructuredBody;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.odd.generator.section.impl.AdvanceDirectivesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.AllergiesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.AntenatalTestingAndSurveillanceSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.AntepartumVisitFlowsheetSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.EstimatedDeliveryDatesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.MedicationsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.PlanOfCareSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ProblemSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.SurgicalProceduresSectionGenerator;
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

		ClinicalDocument retVal = super.createHeader(oddRegistration);
		
		retVal.setCode(this.getDocumentTypeCode());
		retVal.setTitle(retVal.getCode().getDisplayName());
		retVal.setTemplateId(LIST.createLIST(
			new II(CdaHandlerConstants.DOC_TEMPLATE_MEDICAL_DOCUMENTS),
			new II(CdaHandlerConstants.DOC_TEMPLATE_MEDICAL_SUMMARY),
			new II(CdaHandlerConstants.DOC_TEMPLATE_ANTEPARTUM_SUMMARY)
		));
		
		// CCD body must be structured
		retVal.setComponent(new Component2(ActRelationshipHasComponent.HasComponent, BL.TRUE));
		retVal.getComponent().setBodyChoice(new StructuredBody());
		
		// Now add the required sections
		super.generateSections(oddRegistration,
			retVal,
			EstimatedDeliveryDatesSectionGenerator.class,
			AntepartumVisitFlowsheetSectionGenerator.class,
			AntenatalTestingAndSurveillanceSectionGenerator.class,
			AllergiesSectionGenerator.class,
			MedicationsSectionGenerator.class,
			PlanOfCareSectionGenerator.class,
			AdvanceDirectivesSectionGenerator.class,
			ProblemSectionGenerator.class,
			SurgicalProceduresSectionGenerator.class
		);
		
		// retVal.getComponent().getBodyChoiceIfStructuredBody().getComponent().addAll(sections);
		
		return retVal;
	}

	/**
	 * Get the document type code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getDocumentTypeCode()
	 */
	@Override
    public CE<String> getDocumentTypeCode() {
		return new CE<String>("57055-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "ANTEPARTUM SUMMARY NOTE", null);
    }

	/**
	 * Get the document format code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getFormatCode()
	 */
	@Override
	public CE<String> getFormatCode() {
		return new CE<String>("1.3.6.1.4.1.19376.1.5.3.1.1.11.2", "IHE PCC", "IHE PCC", null, "urn:ihe:pcc:aps:2007", null);
	}

}
