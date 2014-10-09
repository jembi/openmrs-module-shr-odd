package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.Arrays;
import java.util.List;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryAct;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentActMood;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.activelist.ActiveListItem;
import org.openmrs.activelist.ActiveListType;
import org.openmrs.activelist.Problem;
import org.openmrs.activelist.ProblemModifier;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;

/**
 * Generator for Problem section
 */
public class ProblemSectionGenerator extends SectionGeneratorImpl {
	
	// The section code
	private final CE<String> m_sectionCode = new CE<String>("11450-4", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "PROBLEM LIST", null);
	private final Concept m_sectionConcept;
	
	/**
	 * Problem section generator ctor
	 */
	public ProblemSectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
	}
	
	/**
	 * Generate the section itself
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public Section generateSection() {

		// Are there problems on file for the patient?
		List<ActiveListItem> problems = Context.getActiveListService().getActiveListItems(this.m_registration.getPatient(), Problem.ACTIVE_LIST_TYPE);

		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_PROBLEM, CdaHandlerConstants.SCT_TEMPLATE_ACTIVE_PROBLEMS), 
			"section.problem.title",
			this.m_sectionCode
		);

		// Problem section must have level 3 content
		if(problems.size() > 0)
		{
			// Generate problem list
			for(ActiveListItem itm : problems)
			{
				Act problemAct = super.createAct(
					x_ActClassDocumentEntryAct.Act,
					x_DocumentActMood.Eventoccurrence,
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
					itm);
				
				Problem prob = (Problem)itm;
				
				// Negation indicator?
				if(prob.getModifier() != null)
					switch(prob.getModifier())
					{
						case HISTORY_OF:
							problemAct.setStatusCode(ActStatus.Completed);
						case RULE_OUT:
							problemAct.setNegationInd(BL.TRUE);
					}
				else
					problemAct.setStatusCode(ActStatus.Active);
				
				// Add an entry relationship of the problem
				Obs problemObs = itm.getStartObs();
				if(itm.getStopObs() != null)
					problemObs = itm.getStopObs();
				
				Observation problemObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION), 
					problemObs, 
					CdaHandlerConstants.CODE_SYSTEM_SNOMED);
				
				problemAct.getEntryRelationship().add(new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.FALSE, BL.TRUE, null, null, null, problemObservation));
				retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, problemAct));
			}

			retVal.setText(super.generateLevel3Text(retVal));
			
		}
		else if(this.getSectionObs().size() > 0) // unstructured?
		{
			super.generateLevel2Content(retVal);
		}
		else
		{
			retVal.setText(new SD(SD.createText("No content recorded")));
		}
		return retVal;
	}

	/**
	 * Get the obs group concept
	 * @see org.openmrs.module.shr.odd.generator.section.impl.SectionGeneratorImpl#getSectionObsGroupConcept()
	 */
	@Override
	protected Concept getSectionObsGroupConcept() {
		return this.m_sectionConcept;
	}
	
}
