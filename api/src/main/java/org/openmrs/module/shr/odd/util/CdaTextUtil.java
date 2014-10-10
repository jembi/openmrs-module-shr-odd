package org.openmrs.module.shr.odd.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.marc.everest.datatypes.BL;
import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.II;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TEL;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.doc.StructDocTextNode;
import org.marc.everest.datatypes.generic.CD;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.EIVL;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.datatypes.generic.PIVL;
import org.marc.everest.datatypes.generic.SXCM;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.SubstanceAdministration;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ActStatus;
import org.marc.everest.rmim.uv.cdar2.vocabulary.DrugEntity;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ObservationInterpretation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntry;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_ActRelationshipEntryRelationship;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubject;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubstanceMood;
import org.openmrs.api.db.AdministrationDAO;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;


/**
 * CDA Text utility for building text
 */
public final class CdaTextUtil {
	// Singleton stuff
	private static final Object s_lockObject = new Object();
	private static CdaTextUtil s_instance;

	/**
	 * Private ctor
	 */
	private CdaTextUtil()
	{
		
	}
	
	/**
	 * Get instance of the ODD CDA text generation utility
	 */
	public static CdaTextUtil getInstance() {
		if(s_instance == null)
			synchronized (s_lockObject) {
				if(s_instance == null)
					s_instance = new CdaTextUtil();
            }
		return s_instance;
	}
	
	/**
	 * Generate text node for the clinical statement
	 */
	public StructDocElementNode generateText(ClinicalStatement statement, StructDocElementNode context, ClinicalDocument document)
	{
		if(statement.isPOCD_MT000040UVOrganizer())
			return this.generateText((Organizer)statement, context, document);
		else if(statement.isPOCD_MT000040UVObservation())
			return this.generateText((Observation)statement, context, document);
		else if(statement.isPOCD_MT000040UVAct())
			return this.generateText((Act)statement, context, document);
		else if(statement.isPOCD_MT000040UVSubstanceAdministration())
			return this.generateText((SubstanceAdministration)statement, context, document);
		else
			return new StructDocElementNode("content", statement.toString());
	}

	/**
	 * Substance administration
	 * 
	 * <tr>
	 * 	<td colspan="2">Procedure (Code)</td>
	 * 	<td>Date</td>
	 *  <td>Frequency</td>
	 *  <td>Drug</td>
	 *  <td>Dose</td>
	 *  <td>Form</td>
	 *  <td>Status</td>
	 *  <td>Notes</td>
	 * </tr>
	 */
	public StructDocElementNode generateText(SubstanceAdministration administration, StructDocElementNode context, ClinicalDocument document)
	{
		StructDocElementNode retVal = new StructDocElementNode("tr");
		
		
		String id = String.format(String.format("obs%s", UUID.randomUUID()));
		id = id.substring(0, id.indexOf("-"));
		
		StructDocElementNode procedureTd = null;
		if(administration.getCode() != null)
		{
			procedureTd = this.createCodeTextCell(administration.getCode());
			retVal.getChildren().add(procedureTd);
		}
		else
			procedureTd = retVal.addElement("td", "Drug Administration");
		procedureTd.addAttribute("colspan", "2");
		if(!administration.getMoodCode().getCode().equals(x_DocumentSubstanceMood.Eventoccurrence))
			procedureTd.addText(String.format(" (%s)", administration.getMoodCode().getCode()));
		if(BL.TRUE.equals(administration.getNegationInd()))
			procedureTd.addText(" (Not Performed)");
		if(administration.getEffectiveTime().size() > 0)
			retVal.addElement("td").getChildren().addAll(this.generateIvlDisplay((SXCM<TS>)administration.getEffectiveTime().get(0)));
		else
			retVal.addElement("td", "Unknown");
		
		if(administration.getEffectiveTime().size() > 1)
		{
			StructDocElementNode freqTd = retVal.addElement("td");
			SXCM<TS> time = (SXCM<TS>)administration.getEffectiveTime().get(1);
			if(time instanceof PIVL)
			{
				freqTd.addText("Every ");
				freqTd.addText(((PIVL)time).getPeriod().toString());
			}
			else if(time instanceof EIVL)
			{
				EIVL<TS> eivl = (EIVL<TS>)time;
				if(eivl.getOffset().getValue() != null)
					freqTd.addText(eivl.getOffset().getValue().toString());
				freqTd.addText(" ");
				freqTd.addText(eivl.getEvent().getCode().name());
			}
			else if(time instanceof SXCM)
			{
				freqTd.addText("Once On ");
				freqTd.getChildren().add(this.createTimeDisplay(time.getValue()));
			}

		}
		else
			retVal.addElement("td", "One time");
		
		// Drug
		if(administration.getConsumable() != null &&
				administration.getConsumable().getManufacturedProduct() != null &&
				administration.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterial() != null)
		{
			if(administration.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedLabeledDrug() != null)
				retVal.addElement("td", 
					SD.createText("Labeled drug "), 
					SD.createText(administration.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedLabeledDrug().getName().toString()));
			else
				retVal.getChildren().add(this.createCodeTextCell(administration.getConsumable().getManufacturedProduct().getManufacturedDrugOrOtherMaterialIfManufacturedMaterial().getCode()));
		}
		else
			retVal.addElement("td", "None Identified");
		
		// Dose
		if(administration.getDoseQuantity() != null && !administration.getDoseQuantity().isNull())
			retVal.addElement("td",administration.getDoseQuantity().toString());
		else
			retVal.addElement("td", "?");
		
		// Form
		if(administration.getAdministrationUnitCode() != null && !administration.getAdministrationUnitCode().isNull())
			retVal.getChildren().add(this.createCodeTextCell(administration.getAdministrationUnitCode()));
		else
			retVal.addElement("td");
		
		// Status event?
		retVal.addElement("td", administration.getStatusCode().getCode().toString());
		
		// Notes
		if(administration.getText() != null)
			retVal.addElement("td", administration.getText().toString());
		else
			retVal.addElement("td");
		
		// Set the text
		administration.setText(new ED(new TEL(String.format("#%s", id))));
		
		// There is no context so the retval becomes the list
		if(context == null)
		{
			context = new StructDocElementNode();
			StructDocElementNode tbody = this.createSubstanceAdministrationTable(context);
			tbody.getChildren().add(retVal);
			retVal = (StructDocElementNode)context.getChildren().get(0);
			context = tbody; 
		}
		else if(context.getName().equals("list"))
		{
			context = context.addElement("item");
			context = this.createSubstanceAdministrationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("item"))
		{
			context = this.createSubstanceAdministrationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("td"))
		{
			context = context.addElement("list").addElement("item");
			context = this.createSubstanceAdministrationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("table")) // need to find the tbody
		{
			for(StructDocNode node : context.getChildren())
				if(node.getName().equals("tbody"))
					node.getChildren().add(retVal);
		}
		else
			context.getChildren().add(retVal);
		
		// Now, is there sub-observations
		if(administration.getEntryRelationship().size() > 0)
		{
			StructDocElementNode relationshipRow = context.addElement("tr");
			relationshipRow.addElement("td");
			relationshipRow.addElement("td", "Related Doses");
			StructDocElementNode relationshipContent = relationshipRow.addElement("td");
			relationshipContent.addAttribute("colspan", "6");
			StructDocElementNode subContext = this.createObservationTable(relationshipContent.addElement("list").addElement("item"));
			for(EntryRelationship er : administration.getEntryRelationship())
				this.generateText(er.getClinicalStatement(), subContext, document);
		}
		
		return retVal;
				
	}
	
	/**
	 * Create a code text cell
	 */
	private StructDocElementNode createCodeTextCell(CV<String> code) {
		StructDocElementNode retVal = null;
		if(code == null || code.isNull())
			retVal = new StructDocElementNode("td","N/A");
		else if(code.getOriginalText() != null)
		{
			String id = String.format("txt%s", UUID.randomUUID());
			id = id.substring(0, id.indexOf("-"));
			StructDocElementNode cellNode = new StructDocElementNode("td");
			cellNode.addText(code.getOriginalText().toString());
			cellNode.addAttribute("ID", id);
			code.setOriginalText(new ED());
			code.getOriginalText().setReference(new TEL(String.format("#%s", id)));
			retVal = cellNode;
		}
		else
			retVal = new StructDocElementNode("td", code.getDisplayName());
		retVal.setNamespaceUri("urn:hl7-org:v3");
		return retVal;
    }

	/**
	 * Create substance administration table header
	 */
	private StructDocElementNode createSubstanceAdministrationTable(StructDocElementNode context) {
		// For each component place in a table row!
				StructDocElementNode table = context.addElement("table"),
						thead = table.addElement("thead"),
						theadRow = thead.addElement("tr"), 
						tbody = table.addElement("tbody");
				
				theadRow.addElement("th", "Administration Procedure").addAttribute("colspan", "2");
				theadRow.addElement("th", "Date/Time");
				theadRow.addElement("th", "Frequency");
				theadRow.addElement("th", "Drug");
				theadRow.addElement("th", "Dose");
				theadRow.addElement("th", "Form");
				theadRow.addElement("th", "Status");
				theadRow.addElement("th", "Notes");
				
				return tbody;
    }

	/**
	 * Text for Acts
	 */
	public StructDocElementNode generateText(Act act, StructDocElementNode context, ClinicalDocument document)
	{
		for(EntryRelationship er : act.getEntryRelationship())
		{
			// HACK: Generate content with act status
			ActStatus originalStatus = act.getStatusCode().getCode();
			if(er.getClinicalStatementIfObservation() != null)
			{
				originalStatus = er.getClinicalStatementIfObservation().getStatusCode().getCode();
				er.getClinicalStatementIfObservation().setStatusCode(act.getStatusCode());
			}
			
			StructDocElementNode node = this.generateText(er.getClinicalStatement(), context, document);


			// Reset the status code
			if(er.getClinicalStatementIfObservation() != null)
				er.getClinicalStatementIfObservation().setStatusCode(originalStatus);

			return node;
		}
		return context.addElement("content", act.getCode().getDisplayName());
	}
	
	/**
	 * Create text for an organizer or template
	 * 
	 * <list>
	 * 	<item>
	 * 		<caption>Organizer.code - Organizer.effectiveTime</caption>
	 * 	</item>
	 * </list>
	 */
	public StructDocElementNode generateText(Organizer organizer, StructDocElementNode context, ClinicalDocument document)
	{
		StructDocElementNode retVal = new StructDocElementNode("item");
		StructDocElementNode caption = retVal.addElement("caption");
		if(organizer.getCode() == null)
			caption.addText("Organizer ");
		else
			caption.addText(organizer.getCode().getDisplayName());
		
		// Effective time
		if(organizer.getEffectiveTime() != null)
		{
			caption.addText(" (effective: ");
			caption.getChildren().addAll(this.generateIvlDisplay(organizer.getEffectiveTime()));
			caption.addText(")");
		}
		
		// Now the related to?
		if(organizer.getSubject() != null)
		{
			caption.addText(" (information pertains to: ");
			if(organizer.getSubject().getRelatedSubject() != null)
			{
				
				// Name / dob / gender?
				if(organizer.getSubject().getRelatedSubject().getSubject() != null)
				{
					if(organizer.getSubject().getRelatedSubject().getSubject().getName() != null)
						caption.addText(" " + organizer.getSubject().getRelatedSubject().getSubject().getName().get(0).toString());
					if(organizer.getSubject().getRelatedSubject().getSubject().getAdministrativeGenderCode() != null)
						caption.addText(" " + organizer.getSubject().getRelatedSubject().getSubject().getAdministrativeGenderCode().getCode());
					if(organizer.getSubject().getRelatedSubject().getSubject().getBirthTime() != null)
					{
						SimpleDateFormat fmt = new SimpleDateFormat(this.getDateFormat(organizer.getSubject().getRelatedSubject().getSubject().getBirthTime()));
						caption.addText(" DOB: " + fmt.format(organizer.getSubject().getRelatedSubject().getSubject().getBirthTime().getDateValue().getTime()));
					}
				}
				if(organizer.getSubject().getRelatedSubject().getClassCode().getCode().equals(x_DocumentSubject.PersonalRelationship))
					caption.addText("[Relative] ");
				else
					caption.addText("[Person] ");
				
				if(organizer.getSubject() != null && 
						organizer.getSubject().getRelatedSubject() != null &&
								organizer.getSubject().getRelatedSubject().getCode() != null)
					caption.addText(String.format(" [%s] ", organizer.getSubject().getRelatedSubject().getCode().getDisplayName()));
			}
			caption.addText(")");
		}
		StructDocElementNode tbody = this.createObservationTable(retVal);
		
		// Component
		for(Component4 comp : organizer.getComponent())
			this.generateText(comp.getClinicalStatement(), tbody, document);
		
		// There is no context so the retval becomes the list
		if(context == null)
		{
			context = new StructDocElementNode("list");
			context.getChildren().add(retVal);
			retVal = context;
		}
		else if(!context.getName().equals("list"))
			context.addElement("list").getChildren().add(retVal);
		else
			context.getChildren().add(retVal);
		
		return retVal;
	}

	/**
	 * Create a row for an observation 
	 * 
	 * <tr ID="obsXXXXX">
	 * 	<td>CODE DISPLAY (OBS)</td>
	 *  <td>EFFECTIVE TIME</td>
	 *  <td>VALUE</td>
	 *  <td>INTERPRETATION</td>
	 *  <td>EXISTING TEXT AS COMMENT</td>
	 * </re>
	 */
	public StructDocElementNode generateText(Observation observation, StructDocElementNode context, ClinicalDocument document)
	{
		StructDocElementNode retVal = new StructDocElementNode("tr");

		String id = String.format("obs%s", UUID.randomUUID());
		id = id.substring(0, id.indexOf("-"));

		retVal.addAttribute("ID", id);
		
		// TODO: entry relationships
		if(observation.getCode() != null && !observation.getCode().isNull())
			retVal.getChildren().add(this.createCodeTextCell(observation.getCode()));
		else
			retVal.addElement("td", "Unknown");
		
		
		// Time
		if(observation.getEffectiveTime() != null && !observation.getEffectiveTime().isNull())
		{
			StructDocElementNode efftTime = retVal.addElement("td");
			efftTime.getChildren().addAll(this.generateIvlDisplay(observation.getEffectiveTime()));
		}
		else
			retVal.addElement("td", "Unknown");
		
		// Value
		StructDocElementNode valueTd = null;
		if(observation.getValue() instanceof CV)
		{
			valueTd = this.createCodeTextCell((CV)observation.getValue());
			retVal.getChildren().add(valueTd);
		}
		else if(observation.getValue() instanceof ED)
		{
			TEL reference = ((ED)observation.getValue()).getReference();
			if(reference != null)
				valueTd = retVal.addElement("td", new StructDocElementNode("renderMultiMedia").addAttribute("referencedObject", reference.getValue()));
			else
				valueTd = retVal.addElement("td");
			valueTd.addText("Binary Data");
		}
		else if(observation.getValue() != null && !observation.getValue().isNull())
			valueTd = retVal.addElement("td", observation.getValue().toString());
		else if(observation.getValue() != null)
			valueTd = retVal.addElement("td", observation.getValue().getNullFlavor().getCode().toString());
		else
			valueTd = retVal.addElement("td", "Unknown / Not Present");
		
		if(BL.TRUE.equals(observation.getNegationInd()))
		{
			valueTd.addElement("br");
			valueTd.addText("(Negated)");
		}
		
		// Author
		StructDocElementNode authorCol = retVal.addElement("td");
		for(Author aut : observation.getAuthor())
		{
			authorCol.getChildren().add(this.generateAuthorDisplay(aut.getAssignedAuthor().getId().get(0), document));
			authorCol.addElement("br");
		}
		
		// Interpretation
		if(observation.getInterpretationCode() != null && !observation.getInterpretationCode().isNull())
		{
			StructDocElementNode content = retVal.addElement("td");
			for(CE<ObservationInterpretation> interp : observation.getInterpretationCode())
				content.addText(interp.getDisplayName() + " ");
		}
		else
			retVal.addElement("td", "");
		
		retVal.addElement("td", observation.getStatusCode().toString());
		
		// Notes
		if(observation.getText() != null)
		{
			retVal.addElement("td", observation.getText().toString());
		}
		else
			retVal.addElement("td","");
		
		// Overwrite text
		observation.setText(new ED());
		observation.getText().setReference(new TEL(String.format("#%s", id)));
		
		// There is no context so the retval becomes the list
		if(context == null)
		{
			context = new StructDocElementNode();
			StructDocElementNode tbody = this.createObservationTable(context);
			tbody.getChildren().add(retVal);
			retVal = (StructDocElementNode)context.getChildren().get(0);
			context = tbody; 
		}
		else if(context.getName().equals("list"))
		{
			context = context.addElement("item");
			context = this.createObservationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("item"))
		{
			context = this.createObservationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("td"))
		{
			context = context.addElement("list").addElement("item");
			context = this.createObservationTable(context);
			context.getChildren().add(retVal);
		}
		else if(context.getName().equals("table")) // need to find the tbody
		{
			for(StructDocNode node : context.getChildren())
				if(node.getName().equals("tbody"))
				{
					context = (StructDocElementNode)node;
					node.getChildren().add(retVal);
				}
		}
		else
			context.getChildren().add(retVal);
		
		// Now, is there sub-observations
		if(observation.getEntryRelationship().size() > 0)
		{
			StructDocElementNode relationshipRow = context.addElement("tr");
			relationshipRow.addElement("td");
			StructDocElementNode relationshipContent = relationshipRow.addElement("td");
			relationshipContent.addAttribute("colspan", "6");
			StructDocElementNode relationshipItem = relationshipContent.addElement("list").addElement("item");
			relationshipItem.addElement("caption", "Additional Information");
			StructDocElementNode subContext = this.createObservationTable(relationshipItem);
			for(EntryRelationship er : observation.getEntryRelationship())
			{
				String captionText = "Has Component:";
				
				// Text for the caption
				if(BL.TRUE.equals(er.getInversionInd()))
				{
					if(x_ActRelationshipEntryRelationship.CAUS.equals(er.getTypeCode().getCode()))
						captionText = "Caused by:";
					else if(x_ActRelationshipEntryRelationship.SPRT.equals(er.getTypeCode().getCode()))
						captionText = "Supported By:";
					else if(x_ActRelationshipEntryRelationship.SUBJ.equals(er.getTypeCode().getCode()))
						captionText = "Subject of:";
				}
				else
				{
					if(x_ActRelationshipEntryRelationship.CAUS.equals(er.getTypeCode().getCode()))
						captionText = "Causes:";
					else if(x_ActRelationshipEntryRelationship.SPRT.equals(er.getTypeCode().getCode()))
						captionText = "Supports:";
					else if(x_ActRelationshipEntryRelationship.MFST.equals(er.getTypeCode().getCode()))
						captionText = "Manifests:";
					else if(x_ActRelationshipEntryRelationship.SUBJ.equals(er.getTypeCode().getCode()))
						captionText = "Subjects:";
				}
				
				StructDocElementNode node = subContext.addElement("tr").addElement("td");
				node.addAttribute("colspan", "7");
				node = node.addElement("content");
				node.addAttribute("styleCode", "Bold");
				node.addText(captionText);
				StructDocElementNode row = this.generateText(er.getClinicalStatement(), subContext, document);
				
				
				row = row;
			}
		}
		
		return retVal;
	}
	
	/**
	 * Generate display for the author
	 */
	private StructDocTextNode generateAuthorDisplay(II authorId, ClinicalDocument document)
	{
		// Find the author
		for(Author aut : document.getAuthor())
			if(aut.getAssignedAuthor().getId().contains(authorId))
				return new StructDocTextNode(aut.getAssignedAuthor().getAssignedAuthorChoiceIfAssignedPerson().getName().get(0).toString());
		return new StructDocTextNode("Unknown");
	}
	/**
	 * Get a displayable value for IVL
	 */
	private Collection<? extends StructDocNode> generateIvlDisplay(SXCM<TS> timeComponent) {
		List<StructDocNode> retVal = new ArrayList<StructDocNode>();
		
		
		if(timeComponent.getValue() != null && !timeComponent.getValue().isNull())
		{
			retVal.add(new StructDocTextNode(" On "));
			retVal.add(this.createTimeDisplay(timeComponent.getValue()));
		}
		
		if(timeComponent instanceof IVL)
		{
			IVL<TS> ivl = (IVL<TS>)timeComponent;
			if(ivl.getLow() != null && !ivl.getLow().isNull())
			{
				retVal.add(new StructDocTextNode(" Since "));
				retVal.add(this.createTimeDisplay(ivl.getLow()));
			}
			if(ivl.getHigh() != null && !ivl.getHigh().isNull())
			{
				retVal.add(new StructDocTextNode(" Until "));
				retVal.add(this.createTimeDisplay(ivl.getHigh()));
			}
		}		
		return retVal;
	}

	/**
	 * Text node
	 */
	private StructDocTextNode createTimeDisplay(TS value) {
		
		if(value == null)
			return new StructDocTextNode();
		SimpleDateFormat valueDateFormat = new SimpleDateFormat(this.getDateFormat(value));
		return new StructDocTextNode(valueDateFormat.format(value.getDateValue().getTime()));
	}

	/**
	 * Get the date format
	 */
	private String getDateFormat(TS value) {
		
		if(value == null)
			return "";
		StringBuilder dateFormat = new StringBuilder();
		int precision = value.getDateValuePrecision();
		
		if(TS.DAY <= precision)
			dateFormat.append("E ");
		if(TS.MONTH <= precision)
			dateFormat.append("MMM ");
		if(TS.DAY <= precision)
			dateFormat.append("d ");
		if(TS.HOURNOTIMEZONE <= precision)
			dateFormat.append("HH");
		if(TS.MINUTENOTIMEZONE <= precision)
			dateFormat.append(":mm");
		if(TS.SECONDNOTIMEZONE <= precision)
			dateFormat.append(":ss");
		if(value.getValue().contains("-"))
			dateFormat.append(" zzz ");
		if(TS.YEAR <= precision)
			dateFormat.append("yyyy");
		
		return dateFormat.toString();
    }

	/**
	 * Create an observation table returning the body
	 */
	private StructDocElementNode createObservationTable(StructDocElementNode context) {
		// For each component place in a table row!
		StructDocElementNode table = context.addElement("table"),
				thead = table.addElement("thead"),
				theadRow = thead.addElement("tr"), 
				tbody = table.addElement("tbody");
		
		theadRow.addElement("th", "Observation");
		theadRow.addElement("th", "Date/Time");
		theadRow.addElement("th", "Observed Value");
		theadRow.addElement("th", "Author");
		theadRow.addElement("th", "Interpretation");
		theadRow.addElement("th", "Status");
		theadRow.addElement("th", "Notes");
		
		return tbody;
	}
	
}
