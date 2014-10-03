package org.openmrs.module.shr.odd.generator.section.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.module.shr.odd.generator.SectionGenerator;

/**
 * Section generator factory will construct or retrieve an
 * instance of the section generator specified
 */
public final class SectionGeneratorFactory {

	// Cached instances
	private final static Log log = LogFactory.getLog(SectionGeneratorFactory.class);
	
	/**
	 * Gets or creates the specified section generator
	 * Auto generated method comment
	 * 
	 * @param clazz
	 * @return
	 */
	public static SectionGenerator createInstance(Class<? extends SectionGenerator> clazz) {
		try {
            return clazz.newInstance();
        }
        catch (Exception e) {
            // TODO Auto-generated catch block
            log.error("Error generated creating generator", e);
            return null;
        }
    }
	
}
