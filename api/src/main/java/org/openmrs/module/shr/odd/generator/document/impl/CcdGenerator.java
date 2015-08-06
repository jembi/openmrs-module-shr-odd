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
import org.openmrs.module.shr.odd.generator.section.impl.BirthPlanSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.FamilyHistorySectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.FunctionalStatusSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.HistoryOfBloodTransfusionSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.HistoryOfPresentIllnessSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.HistoryOfSurgicalProceduresSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ImmunizationsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.MedicationsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.PlanOfCareSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ProblemSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ProceduresSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.PurposeOfUseSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.ResultsSectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.SocialHistorySectionGenerator;
import org.openmrs.module.shr.odd.generator.section.impl.VitalSignsSectionGenerator;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * A Cda Generator for a Continuity of Care Document (CCD)
 */
public class CcdGenerator extends DocumentGeneratorImpl {

	// Document code
	private final CE<String> m_documentCode = new CE<String>("34133-9", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "Summarization of Episode Note", null);
			
	/**
	 * Generate the CCD
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#generateDocument(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public ClinicalDocument generateDocument(OnDemandDocumentRegistration oddRegistration) {
		ClinicalDocument retVal = super.createHeader(oddRegistration);
		
		// CCD CONF-1
		retVal.setCode(m_documentCode);
		retVal.setTitle(retVal.getCode().getDisplayName());
		// CCD Template ID (CCD CONF-7 & CONF-8)
		retVal.setTemplateId(LIST.createLIST(
			new II(CdaHandlerConstants.DOC_TEMPLATE_CCD),
			new II(CdaHandlerConstants.DOC_TEMPLATE_CDA4CDT)
		));
		
		if(this.m_configuration.getUseCcdPlus())
			retVal.getTemplateId().add(new II(CdaHandlerConstants.DOC_TEMPLATE_CCD_PLUS));
		
		// CCD body must be structured
		retVal.setComponent(new Component2(ActRelationshipHasComponent.HasComponent, BL.TRUE));
		retVal.getComponent().setBodyChoice(new StructuredBody());
		
		// Now add the required sections
		// HACK: CCD+ Should be configurable in some way...
		if(this.m_configuration.getUseCcdPlus())
			super.generateSections(oddRegistration,
				retVal,
				PurposeOfUseSectionGenerator.class,
				AdvanceDirectivesSectionGenerator.class,
				PlanOfCareSectionGenerator.class,
				VitalSignsSectionGenerator.class,
				ProblemSectionGenerator.class,
				AllergiesSectionGenerator.class,
				FamilyHistorySectionGenerator.class,
				MedicationsSectionGenerator.class,
				ImmunizationsSectionGenerator.class,
				ProceduresSectionGenerator.class,
				SocialHistorySectionGenerator.class,
				ResultsSectionGenerator.class,
				AntepartumVisitFlowsheetSectionGenerator.class,
				AntenatalTestingAndSurveillanceSectionGenerator.class,
				HistoryOfSurgicalProceduresSectionGenerator.class,
				HistoryOfPresentIllnessSectionGenerator.class,
				BirthPlanSectionGenerator.class, 
				HistoryOfBloodTransfusionSectionGenerator.class,
				FunctionalStatusSectionGenerator.class
				//HistoryOfPastIllnessSectionGenerator.class,
				//PregnancyHistorySectionGenerator.class
			);
		else
			super.generateSections(oddRegistration,
				retVal,
				PurposeOfUseSectionGenerator.class,
				AdvanceDirectivesSectionGenerator.class,
				PlanOfCareSectionGenerator.class,
				VitalSignsSectionGenerator.class,
				ProblemSectionGenerator.class,
				AllergiesSectionGenerator.class,
				FamilyHistorySectionGenerator.class,
				MedicationsSectionGenerator.class,
				ImmunizationsSectionGenerator.class,
				ProceduresSectionGenerator.class,
				SocialHistorySectionGenerator.class,
				ResultsSectionGenerator.class
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
	    return this.m_documentCode;
    }


	/**
	 * Get the document format code
	 * @see org.openmrs.module.shr.odd.generator.DocumentGenerator#getFormatCode()
	 */
	@Override
	public CE<String> getFormatCode() {
		return new CE<String>("2.16.840.1.113883.10.20.1", "HITSP", "HITSP", null, "HL7 CCD Document", null);
	}
	
}
