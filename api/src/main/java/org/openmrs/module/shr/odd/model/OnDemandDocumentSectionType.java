package org.openmrs.module.shr.odd.model;

import org.openmrs.BaseOpenmrsMetadata;

/**
 * Represents an OnDemandDocumentSectionType
 * @author Justin
 *
 */
public class OnDemandDocumentSectionType  extends BaseOpenmrsMetadata  {

	// The java class which generates the section
	private String javaClass;
	// The section name
	private String sectionName;
	// Identifier of the meta-data
	private Integer id;
	
	/**
	 * Gets or sets the java class
	 * @return
	 */
	public String getJavaClassName() {
		return javaClass;
	}
	/**
	 * Gets or sets the java class 
	 * @param javaClass
	 */
	public void setJavaClassName(String javaClass) {
		this.javaClass = javaClass;
	}
	/**
	 * Gets the section name
	 * @return
	 */
	public String getSectionName() {
		return sectionName;
	}
	/**
	 * Sets the section name
	 * @param sectionName
	 */
	public void setSectionName(String sectionName) {
		this.sectionName = sectionName;
	}

	/**
	 * Gets the identifier of the section type
	 */
	@Override
	public Integer getId() {
		return id;
	}
	/**
	 * Sets the identifier of the section type
	 */
	@Override
	public void setId(Integer arg0) {
		id = arg0;
	}
	
	
}
