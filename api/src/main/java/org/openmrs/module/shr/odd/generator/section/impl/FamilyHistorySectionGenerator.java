package org.openmrs.module.shr.odd.generator.section.impl;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.soap.Detail;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ENXP;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.INT;
import org.marc.everest.datatypes.NullFlavor;
import org.marc.everest.datatypes.PN;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.LIST;
import org.marc.everest.datatypes.generic.SET;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.RelatedSubject;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Subject;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubjectPerson;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActRelationshipHasComponent;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.AdministrativeGender;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActClassDocumentEntryOrganizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActMoodDocumentObservation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubject;
import org.openmrs.Concept;
import org.openmrs.Obs;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.everest.sdtc.SdtcSubjectPerson;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.obs.ExtendedObs;

/**
 * Family history section generator
 */
public class FamilyHistorySectionGenerator extends SectionGeneratorImpl {
	
	// Code and concepts
	private final CE<String> m_sectionCode = new CE<String>("10157-6", CdaHandlerConstants.CODE_SYSTEM_LOINC, CdaHandlerConstants.CODE_SYSTEM_NAME_LOINC, null, "History of family member diseases", null);
	private Concept m_sectionConcept = null;

	/**
	 * Family history section generator
	 */
	public FamilyHistorySectionGenerator() throws DocumentImportException
	{
		this.m_sectionConcept = this.m_conceptUtil.getConcept(this.m_sectionCode);
	}

	/**
	 * Generate the section
	 * @see org.openmrs.module.shr.odd.generator.SectionGenerator#generateSection()
	 */
	@Override
	public Section generateSection() {
		// Family history section
		Section retVal = super.createSection(
			Arrays.asList(CdaHandlerConstants.SCT_TEMPLATE_CCD_FAMILY_HISTORY, CdaHandlerConstants.SCT_TEMPLATE_FAMILY_HISTORY),
			"section.familyHistory.title", 
			this.m_sectionCode);
		
		if(super.allEncountersHaveDiscreteComponentObs())
		{
			// Create level 3
			retVal.getTemplateId().add(new II(CdaHandlerConstants.SCT_TEMPLATE_CODED_FAMILY_MEDICAL_HISTORY));
			List<Obs> encounterFamilyHistoryObs = super.getAllObservationsOfType(Context.getConceptService().getConcept(160593));

			// Group these observations by family member! eek
			Map<Concept, List<Obs>> organizedFamilyHistoryObs = new HashMap<Concept, List<Obs>>();
			Concept familyMemberRelationConcept = Context.getConceptService().getConcept(1560);
			for(Obs familyHistoryListObs : encounterFamilyHistoryObs)
			{
				if(familyHistoryListObs.getVoided()) continue;
				
				List<Obs> familyRelationObs = super.m_service.getObsGroupMembers(familyHistoryListObs, Arrays.asList(familyMemberRelationConcept));
				if(familyRelationObs.size() == 0) 
					continue;
				else
				{
					Concept relationship = familyRelationObs.get(0).getValueCoded();
					List<Obs> obsForRelationship = organizedFamilyHistoryObs.get(relationship);
					if(obsForRelationship == null)
					{
						obsForRelationship = new ArrayList<Obs>();
						organizedFamilyHistoryObs.put(relationship, obsForRelationship);
					}
					obsForRelationship.add(familyHistoryListObs);
				}
			}
			
			// Now create the structures in the CDA
			for(Map.Entry<Concept, List<Obs>> familyHistoryTuple : organizedFamilyHistoryObs.entrySet())
			{
				// TODO:
				// 2. Get the appropriate sub-obs from the current obs
				// 3. Construct family member information from the family history 
				// 4. Iterate through each value() obtaining the diagnosis and date creating an obs
				// 4a. Create an age obs
				Organizer organizer = super.createOrganizer(x_ActClassDocumentEntryOrganizer.CLUSTER, 
					Arrays.asList(CdaHandlerConstants.ENT_TEMPLATE_CCD_FAMILY_HISTORY_ORGANIZER, CdaHandlerConstants.ENT_TEMPLATE_FAMILY_HISTORY_ORGANIZER), 
					this.m_oddMetadataUtil.getStandardizedCode(familyHistoryTuple.getKey(), CdaHandlerConstants.CODE_SYSTEM_FAMILY_MEMBER, CD.class), 
					null, 
					ActStatus.Completed, 
					null);
				organizer.getAuthor().add(super.createAuthorPointer(familyHistoryTuple.getValue().get(0)));
				
				// Iterate through the tuples of family history data as these become components for the family history
				for(Obs historyItem : familyHistoryTuple.getValue())
				{
					
					if(historyItem.getVoided())
						continue;
					
					// Get all the sub-obs
					List<Obs> historyItemDetail = super.m_service.getObsGroupMembers(historyItem);

					// These are a little different so ...
					Observation historyObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
					organizer.getComponent().add(new Component4(ActRelationshipHasComponent.HasComponent, BL.TRUE, historyObservation));
					historyObservation.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_FAMILY_HISTORY_OBSERVATION), new II(CdaHandlerConstants.ENT_TEMPLATE_FAMILY_HISTORY_OBSERVATION)));
					historyObservation.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), historyItem.getId().toString())));
					if(historyItem.getAccessionNumber() != null)
						historyObservation.getId().add(this.m_cdaDataUtil.parseIIFromString(historyItem.getAccessionNumber()));
					historyObservation.setStatusCode(ActStatus.Completed);
					
					// Set the effective time
					ExtendedObs extendedHistoryObs = Context.getService(CdaImportService.class).getExtendedObs(historyItem.getId());
					if(extendedHistoryObs != null)
						super.setExtendedObservationProperties(historyObservation, extendedHistoryObs);
					
					// Comment?
					if(historyItem.getComment() != null)
	                    try {
	                        historyObservation.setText(historyItem.getComment());
                        }
                        catch (UnsupportedEncodingException e) {
	                        // TODO Auto-generated catch block
	                        log.error("Error generated", e);
                        }
					
					// Iterate through the observations and construct the section
					for(Obs detailItem : historyItemDetail)
					{

						// All of these are CIEL Codes so we'll drive off the id of concept
						switch(detailItem.getConcept().getId())
						{

							case 160750: // Name of family member
							
								if(organizer.getSubject() == null)
									organizer.setSubject(new Subject());
								if(organizer.getSubject().getRelatedSubject() == null)
									organizer.getSubject().setRelatedSubject(new RelatedSubject(x_DocumentSubject.PersonalRelationship));
								if(organizer.getSubject().getRelatedSubject().getSubject() == null)
									organizer.getSubject().getRelatedSubject().setSubject(this.createSubjectPerson());
								
								if(organizer.getSubject().getRelatedSubject().getSubject().getName() == null)
									organizer.getSubject().getRelatedSubject().getSubject().setName(SET.createSET(new PN(Arrays.asList(new ENXP(detailItem.getValueText())))));
								break;
							
							case 1560: // family member
								
								if(organizer.getSubject() == null)
									organizer.setSubject(new Subject());
								if(organizer.getSubject().getRelatedSubject() == null)
									organizer.getSubject().setRelatedSubject(new RelatedSubject(x_DocumentSubject.PersonalRelationship));
							
								organizer.getSubject().getRelatedSubject().setCode(this.m_oddMetadataUtil.getStandardizedCode(detailItem.getValueCoded(), CdaHandlerConstants.CODE_SYSTEM_FAMILY_MEMBER, CE.class));
								break;
							
							case 160617: // age a diagnosis
								
								// Add an age diagnosis
								Observation ageObs = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
								ageObs.setTemplateId(LIST.createLIST(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_AGE_OBSERVATION)));
								ageObs.setCode(new CD<String>("397659008", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Age", null));
								ageObs.setValue(new INT(detailItem.getValueNumeric().intValue()));
								ageObs.setStatusCode(ActStatus.Completed);
								EntryRelationship er = new EntryRelationship(x_ActRelationshipEntryRelationship.SUBJ, BL.TRUE);
								er.setInversionInd(BL.TRUE);
								er.setClinicalStatement(ageObs);
								historyObservation.getEntryRelationship().add(er);
								
								break;
								
							case 1729: // sign/symptom present
								
								break;
							
							case 160751: // dob of family member
								
								if(organizer.getSubject() == null)
									organizer.setSubject(new Subject());
								if(organizer.getSubject().getRelatedSubject() == null)
									organizer.getSubject().setRelatedSubject(new RelatedSubject(x_DocumentSubject.PersonalRelationship));
								if(organizer.getSubject().getRelatedSubject().getSubject() == null)
									organizer.getSubject().getRelatedSubject().setSubject(this.createSubjectPerson());
								
								
								organizer.getSubject().getRelatedSubject().getSubject().setBirthTime(this.m_cdaDataUtil.createTS(detailItem.getValueDate()));
								if(detailItem.getComment() != null)
								{
									try
									{
									organizer.getSubject().getRelatedSubject().getSubject().getBirthTime().setDateValuePrecision(Integer.parseInt(detailItem.getComment()));
									}
									catch(Exception e)
									{}
								}
								break;
								
							case 160752: // id of family member
								
								if(organizer.getSubject() == null)
									organizer.setSubject(new Subject());
								if(organizer.getSubject().getRelatedSubject() == null)
									organizer.getSubject().setRelatedSubject(new RelatedSubject(x_DocumentSubject.PersonalRelationship));
								if(organizer.getSubject().getRelatedSubject().getSubject() == null)
									organizer.getSubject().getRelatedSubject().setSubject(this.createSubjectPerson());
								
								SdtcSubjectPerson sdtcSubj = (SdtcSubjectPerson)organizer.getSubject().getRelatedSubject().getSubject();
								if(sdtcSubj.getId() == null)
									sdtcSubj.setId(new SET<II>());
								
								II id = this.m_cdaDataUtil.parseIIFromString(detailItem.getValueText());
								if(sdtcSubj.getId().contains(id))
									sdtcSubj.getId().add(id);
								
								break;
								
							case 160592:

								// Diagnosis of death!
								if(detailItem.getValueCoded() != null && detailItem.getValueCoded().getId().equals(160432))
								{
									historyObservation.getTemplateId().add(new II(CdaHandlerConstants.ENT_TEMPLATE_CCD_DEATH_OBSERVATION));
									Observation deathObservation = new Observation(x_ActMoodDocumentObservation.Eventoccurrence);
									deathObservation.setId(SET.createSET(new II(this.m_cdaConfiguration.getObsRoot(), detailItem.getId().toString())));
									deathObservation.setCode(new CD<String>("ASSERTION", CdaHandlerConstants.CODE_SYSTEM_ACT_CODE));
									deathObservation.setValue(new CD<String>("419099009", CdaHandlerConstants.CODE_SYSTEM_SNOMED, null, null, "Dead", null));
									deathObservation.setStatusCode(ActStatus.Completed);
									EntryRelationship deathRelation = new EntryRelationship(x_ActRelationshipEntryRelationship.CAUS, BL.TRUE, deathObservation);
									historyObservation.getEntryRelationship().add(deathRelation);
								}

								break;
								
							default: // some other obs value??
								
								historyObservation.setCode(this.m_oddMetadataUtil.getStandardizedCode(detailItem.getConcept(), null, CD.class));
								historyObservation.setValue(super.m_cdaDataUtil.getObservationValue(detailItem));
								break;
						}
					} // for
					
					// Subject 
					if(organizer.getSubject() == null)
						organizer.setSubject(new Subject());
					if(organizer.getSubject().getRelatedSubject() == null)
						organizer.getSubject().setRelatedSubject(new RelatedSubject(x_DocumentSubject.PersonalRelationship));
					if(organizer.getSubject().getRelatedSubject().getSubject() == null)
						organizer.getSubject().getRelatedSubject().setSubject(this.createSubjectPerson());
					
					if(organizer.getSubject().getRelatedSubject().getSubject().getName() == null)
					{
						organizer.getSubject().getRelatedSubject().getSubject().setName(SET.createSET(new PN()));
						organizer.getSubject().getRelatedSubject().getSubject().getName().get(0).setNullFlavor(NullFlavor.NoInformation);
					}
					
				} // for history item
				
				if(organizer.getComponent().size() > 0)
					retVal.getEntry().add(new Entry(x_ActRelationshipEntry.HasComponent, BL.TRUE, organizer));

			}
			
			retVal.setText(super.generateLevel3Text(retVal));
		}
		else if(super.getSectionObs().size() > 0)
			super.generateLevel2Content(retVal);
		else
			retVal.setText(new SD(SD.createText("No data present")));
		
		return retVal;
	}

	/**
	 * Create the subject person for an organizer
	 */
	private SdtcSubjectPerson createSubjectPerson() {
		SdtcSubjectPerson retVal = new SdtcSubjectPerson();
		retVal.setAdministrativeGenderCode(new CE<AdministrativeGender>());
		retVal.getAdministrativeGenderCode().setNullFlavor(NullFlavor.NoInformation);
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
