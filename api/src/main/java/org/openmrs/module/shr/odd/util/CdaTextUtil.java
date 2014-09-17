package org.openmrs.module.shr.odd.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.marc.everest.datatypes.ED;
import org.marc.everest.datatypes.SD;
import org.marc.everest.datatypes.TEL;
import org.marc.everest.datatypes.TS;
import org.marc.everest.datatypes.doc.StructDocElementNode;
import org.marc.everest.datatypes.doc.StructDocNode;
import org.marc.everest.datatypes.doc.StructDocTextNode;
import org.marc.everest.datatypes.generic.CE;
import org.marc.everest.datatypes.generic.IVL;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalStatement;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Component4;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Entry;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.EntryRelationship;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Observation;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Organizer;
import org.marc.everest.rmim.uv.cdar2.vocabulary.ObservationInterpretation;


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
	public StructDocElementNode generateText(ClinicalStatement statement, StructDocElementNode context)
	{
		if(statement.isPOCD_MT000040UVOrganizer())
			return this.generateText((Organizer)statement, context);
		else if(statement.isPOCD_MT000040UVObservation())
			return this.generateText((Observation)statement, context);
		else
			return new StructDocElementNode("content", statement.toString());
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
	public StructDocElementNode generateText(Organizer organizer, StructDocElementNode context)
	{
		StructDocElementNode retVal = new StructDocElementNode("item");
		StructDocElementNode caption = retVal.addElement("caption");
		caption.addText(organizer.getCode().getDisplayName() + " (effective: ");
		caption.getChildren().addAll(this.generateIvlDisplay(organizer.getEffectiveTime()));
		caption.addText(")");
		
		StructDocElementNode tbody = this.createObservationTable(retVal);
		
		// Component
		for(Component4 comp : organizer.getComponent())
			this.generateText(comp.getClinicalStatement(), tbody);
		
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
	public StructDocElementNode generateText(Observation observation, StructDocElementNode context)
	{
		StructDocElementNode retVal = new StructDocElementNode("tr");

		String id = String.format("obs%s", observation.hashCode());
		retVal.addAttribute("ID", id);
		
		// TODO: entry relationships
		if(observation.getCode() != null && !observation.getCode().isNull())
			retVal.addElement("td", String.format("%s (OBS)", observation.getCode().getDisplayName()));
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
		if(observation.getValue() != null && !observation.getValue().isNull())
			retVal.addElement("td", observation.getValue().toString());
		else if(observation.getValue() != null)
			retVal.addElement("td", observation.getValue().getNullFlavor().getCode().toString());
		else
			retVal.addElement("td", "Unknown / Not Present");
		
		// Interpretation
		if(observation.getInterpretationCode() != null && observation.getInterpretationCode().isNull())
		{
			StructDocElementNode content = retVal.addElement("td");
			for(CE<ObservationInterpretation> interp : observation.getInterpretationCode())
				content.addText(interp.getDisplayName() + " ");
		}
		else
			retVal.addElement("td", "Not Present");
		
		// Notes
		if(observation.getText() != null)
		{
			retVal.addElement("td", observation.getText().toString());
		}
		
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
		else
			context.getChildren().add(retVal);
		
		// Now, is there sub-observations
		if(observation.getEntryRelationship().size() > 0)
		{
			StructDocElementNode relationshipRow = context.addElement("tr");
			relationshipRow.addElement("td", "Related Acts");
			StructDocElementNode relationshipContent = relationshipRow.addElement("td");
			relationshipContent.addAttribute("colspan", "4");
			StructDocElementNode subContext = this.createObservationTable(relationshipContent.addElement("list").addElement("item"));
			for(EntryRelationship er : observation.getEntryRelationship())
				this.generateText(er.getClinicalStatement(), subContext);
		}
		
		return retVal;
	}
	
	/**
	 * Get a displayable value for IVL
	 */
	private Collection<? extends StructDocNode> generateIvlDisplay(IVL<TS> ivl) {
		List<StructDocNode> retVal = new ArrayList<StructDocNode>();
		if(ivl.getLow() != null && !ivl.getLow().isNull())
			retVal.add(new StructDocTextNode(" Since " + ivl.getLow().getDateValue().getTime().toString() + " "));
		if(ivl.getHigh() != null && !ivl.getHigh().isNull())
			retVal.add(new StructDocTextNode(" Until " + ivl.getLow().getDateValue().getTime().toString() + " "));
		if(ivl.getValue() != null && !ivl.getValue().isNull())
			retVal.add(new StructDocTextNode(" On " + ivl.getValue().getDateValue().getTime()));
		return retVal;
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
		
		theadRow.addElement("th", "Type");
		theadRow.addElement("th", "Date/Time");
		theadRow.addElement("th", "Value");
		theadRow.addElement("th", "Interpretation");
		theadRow.addElement("th", "Notes");
		
		return tbody;
	}
	
}
