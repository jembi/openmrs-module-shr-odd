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
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.CV;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Act;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Author;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ObservationInterpretation;
import org.marc.everest.rmim.uv.cdar2.vocabulary.x_DocumentSubject;


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
		else
			return new StructDocElementNode("content", statement.toString());
	}

	/**
	 * Text for Acts
	 */
	public StructDocElementNode generateText(Act act, StructDocElementNode context, ClinicalDocument document)
	{
		for(EntryRelationship er : act.getEntryRelationship())
			return this.generateText(er.getClinicalStatement(), context, document);
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
			retVal.addElement("td", String.format("%s", observation.getCode().getDisplayName()));
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
			CV obsValue = (CV)observation.getValue();
			if(obsValue.getOriginalText() != null)
			{
				String codeOrgTextRef = String.format("code%s", UUID.randomUUID());
				codeOrgTextRef = codeOrgTextRef.substring(0, codeOrgTextRef.indexOf("-"));
				valueTd = retVal.addElement("td", obsValue.getOriginalText().toString());
				valueTd.addAttribute("ID", codeOrgTextRef);
				obsValue.getOriginalText().setReference(new TEL("#" + codeOrgTextRef));
				obsValue.getOriginalText().setData((byte[])null);
			}
			else
				valueTd = retVal.addElement("td", obsValue.getDisplayName());
				
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
		if(observation.getInterpretationCode() != null && observation.getInterpretationCode().isNull())
		{
			StructDocElementNode content = retVal.addElement("td");
			for(CE<ObservationInterpretation> interp : observation.getInterpretationCode())
				content.addText(interp.getDisplayName() + " ");
		}
		else
			retVal.addElement("td", "");
		
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
			this.createObservationTable(context).getChildren().add(retVal);
			retVal = (StructDocElementNode)context.getChildren().get(0);
		}
		else if(context.getName().equals("list"))
		{
			context = context.addElement("item");
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
					node.getChildren().add(retVal);
		}
		else
			context.getChildren().add(retVal);
		
		// Now, is there sub-observations
		if(observation.getEntryRelationship().size() > 0)
		{
			StructDocElementNode relationshipRow = context.addElement("tr");
			relationshipRow.addElement("td", "Additional Information");
			StructDocElementNode relationshipContent = relationshipRow.addElement("td");
			relationshipContent.addAttribute("colspan", "5");
			StructDocElementNode subContext = this.createObservationTable(relationshipContent.addElement("list").addElement("item"));
			for(EntryRelationship er : observation.getEntryRelationship())
				this.generateText(er.getClinicalStatement(), subContext, document);
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
	private Collection<? extends StructDocNode> generateIvlDisplay(IVL<TS> ivl) {
		List<StructDocNode> retVal = new ArrayList<StructDocNode>();
		if(ivl.getLow() != null && !ivl.getLow().isNull())
		{
			retVal.add(new StructDocTextNode(" Since "));
			SimpleDateFormat lowDateFormat = new SimpleDateFormat(this.getDateFormat(ivl.getLow()));
			retVal.add(new StructDocTextNode(lowDateFormat.format(ivl.getLow().getDateValue().getTime())));
		}
		if(ivl.getHigh() != null && !ivl.getHigh().isNull())
		{
			retVal.add(new StructDocTextNode(" Until "));
			SimpleDateFormat highDateFormat = new SimpleDateFormat(this.getDateFormat(ivl.getHigh()));
			retVal.add(new StructDocTextNode(highDateFormat.format(ivl.getHigh().getDateValue().getTime())));
		}
		if(ivl.getValue() != null && !ivl.getValue().isNull())
		{
			retVal.add(new StructDocTextNode(" On "));
			SimpleDateFormat valueDateFormat = new SimpleDateFormat(this.getDateFormat(ivl.getValue()));
			retVal.add(new StructDocTextNode(valueDateFormat.format(ivl.getValue().getDateValue().getTime())));
		}
		return retVal;
	}

	/**
	 * Get the date format
	 */
	private String getDateFormat(TS value) {
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
		theadRow.addElement("th", "Notes");
		
		return tbody;
	}
	
}
