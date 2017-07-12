package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.*;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
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
import org.openmrs.Condition;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.emrapi.conditionslist.ConditionService;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;

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
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode, null);
	}
	
	/**
	 * Generate the section itself
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection(org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration)
	 */
	@Override
	public Section generateSection() {

		// Are there problems on file for the patient?
		List<Condition> conditions = Context.getService(ConditionService.class).getActiveConditions(this.m_registration.getPatient());

		// Create generic section construct
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_PROBLEM, CdaHandlerConstants.SCT_TEMPLATE_ACTIVE_PROBLEMS), 
			"section.problem.title",
			this.m_sectionCode
		);

		// Problem section must have level 3 content
		if(conditions.size() > 0)
		{
			// Generate problem list
			for(Condition condition : conditions)
			{
				Act problemAct = super.createAct(
					x_ActClassDocumentEntryAct.Act,
					x_DocumentActMood.Eventoccurrence,
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
					condition);

				// Now add reference the status code
				IVL<TS> eft = new IVL<TS>();

				Obs startObs = findFirstProblemObs(condition);
				Obs stopObs = findLastProblemObs(condition);

				if(startObs != null){
					eft.setLow(this.m_cdaDataUtil.createTS(condition.getOnsetDate()));
					//Correct the precision of the dates
					ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(startObs.getId());
					if(obs != null && obs.getObsDatePrecision() == 0)
						eft.getLow().setNullFlavor(NullFlavor.Unknown);
					else if(obs != null)
						eft.getLow().setDateValuePrecision(obs.getObsDatePrecision());
				}
				if(stopObs != null){
					eft.setHigh(this.m_cdaDataUtil.createTS(condition.getEndDate()));
					// Correct the precision of the dates
					ExtendedObs obs = Context.getService(CdaImportService.class).getExtendedObs(stopObs.getId());
					if(obs != null && obs.getObsDatePrecision() == 0)
						eft.getHigh().setNullFlavor(NullFlavor.Unknown);
					else if(obs != null)
						eft.getHigh().setDateValuePrecision(obs.getObsDatePrecision());
				}
				problemAct.setEffectiveTime(eft);



				// Negation indicator?
				if(condition.getStatus() != null)
					switch(condition.getStatus())
					{
						case HISTORY_OF:
							problemAct.setStatusCode(ActStatus.Completed);
						case INACTIVE:
							problemAct.setNegationInd(BL.TRUE);
					}
				else
					problemAct.setStatusCode(ActStatus.Active);

				// Add an entry relationship of the problem
				
				Observation problemObservation = super.createObs(Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_OBSERVATION, CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_OBSERVATION),
						stopObs,
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
			
			Act problemAct = super.createNoKnownProblemAct(
				Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_PROBLEM_CONCERN, CdaHandlerConstants.ENT_TEMPLATE_CONCERN_ENTRY, CdaHandlerConstants.ENT_TEMPLATE_CCD_PROBLEM_ACT),
				new CD<String>("55607006", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Problem", null),
				new CD<String>("396782006", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Past Medical History Unknown", null));
			retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, problemAct));
			retVal.setText(super.generateLevel3Text(retVal));
		}

		return retVal;
	}

	private Obs findLastProblemObs(Condition condition){
		List<Obs> candidates= new ArrayList<>();
		List<Obs> obs = Context.getObsService().getObservationsByPerson(condition.getPatient());
		for(Obs currentObs : obs){
			if(currentObs.getValueCoded() != null && currentObs.getValueCoded().equals(condition.getConcept()))
				candidates.add(currentObs);
		}
		Collections.sort(candidates, Comparator.comparing(Obs::getObsDatetime));
		if(candidates.size()>0)
			return candidates.get(candidates.size()-1);
		return null;
	}

	private Obs findFirstProblemObs(Condition condition){
		List<Obs> candidates= new ArrayList<>();
		List<Obs> obs = Context.getObsService().getObservationsByPerson(condition.getPatient());
		for(Obs currentObs : obs){
			if(currentObs.getValueCoded() != null && currentObs.getValueCoded().equals(condition.getConcept()))
				candidates.add(currentObs);
		}
		Collections.sort(candidates, Comparator.comparing(Obs::getObsDatetime));
		if(candidates.size()>0)
			return candidates.get(0);
		return null;
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
