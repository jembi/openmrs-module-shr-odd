package org.openmrs.module.shr.odd.api.test;

import java.io.ByteArrayOutputStream;

import org.apache.commons.logging.LogFactory;
import org.jfree.util.Log;
import org.marc.everest.formatters.interfaces.IFormatterGraphResult;
import org.marc.everest.formatters.xml.datatypes.r1.DatatypeFormatter;
import org.marc.everest.formatters.xml.datatypes.r1.R1FormatterCompatibilityMode;
import org.marc.everest.formatters.xml.its1.XmlIts1Formatter;
import org.marc.everest.interfaces.IResultDetail;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;

/**
 * Utilities for logging CDA documents
 */
public class CdaLoggingUtils {
	
	/**
	 * Get the clinical document as a string
	 */
	public static final String getCdaAsString(ClinicalDocument document)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		XmlIts1Formatter fmtr = new XmlIts1Formatter();
		fmtr.getGraphAides().add(new DatatypeFormatter(R1FormatterCompatibilityMode.ClinicalDocumentArchitecture));
		fmtr.setValidateConformance(false);
		IFormatterGraphResult result = fmtr.graph(bos, document);
		for(IResultDetail dtl : result.getDetails())
			if(dtl.getException() != null)
				dtl.getException().printStackTrace();
		return new String(bos.toByteArray());
		
	}
	
}
