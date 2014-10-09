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
import org.openmrs.module.shr.cdahandler.processor.entry.impl.ihe.pcc.AntepartumFlowsheetBatteryEntryProcessor;
import org.openmrs.module.shr.contenthandler.api.CodedValue;
import org.openmrs.module.shr.odd.generator.section.impl.AdvanceDirectivesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.AllergiesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.AntepartumVisitFlowsheetSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.EstimatedDeliveryDatesSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.FamilyHistorySectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ImmunizationsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.MedicationsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.PlanOfCareSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ProblemSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ProceduresSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.PurposeOfUseSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.VitalSignsSectionGenerator;
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
			new II(CdaHandlerConstants.DOC_TEMPLATE_CCD),
			new II(CdaHandlerConstants.DOC_TEMPLATE_CDA4CDT),
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
			AllergiesSectionGenerator.class,
			MedicationsSectionGenerator.class,
			PlanOfCareSectionGenerator.class,
			AdvanceDirectivesSectionGenerator.class,
			ProblemSectionGenerator.class
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

}
