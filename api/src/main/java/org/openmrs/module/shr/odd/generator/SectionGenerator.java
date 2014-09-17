package org.openmrs.module.shr.odd.generator;

import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.Section;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;

/**
 * Section generator interface
 */
public interface SectionGenerator {
	
	/**
	 * Generate a section from the specified grouping of observations
	 */
	public Section generateSection();

	/**
	 * Sets the context of this section generator
	 * @param context
	 */
	public void setRegistration(OnDemandDocumentRegistration context);
}
