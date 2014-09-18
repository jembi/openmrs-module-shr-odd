/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */
package org.openmrs.module.shr.odd;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.api.context.Context;
import org.openmrs.module.ModuleActivator;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.contenthandler.api.AlreadyRegisteredException;
import org.openmrs.module.shr.contenthandler.api.ContentHandlerService;
import org.openmrs.module.shr.contenthandler.api.InvalidContentTypeException;
import org.openmrs.module.shr.odd.contenthandler.OnDemandDocumentContentHandler;
import org.openmrs.module.shr.odd.exception.OnDemandDocumentException;
import org.openmrs.module.shr.odd.subscriber.GenericDocumentSubscriber;

/**
 * This class contains the logic that is run every time this module is either started or stopped.
 */
public class OnDemandDocumentsActivator implements ModuleActivator {
	
	protected Log log = LogFactory.getLog(getClass());
	
	/**
	 * Register subscribers
	 */
	private void registerSubscribers()
	{
		CdaImportService importService = Context.getService(CdaImportService.class);
		importService.subscribeImport(null, GenericDocumentSubscriber.getInstance());
	}
	
	/**
	 * Register content handlers
	 */
	private void registerContentHandlers()
	{
		// Register the format codes
		ContentHandlerService contentHandler = Context.getService(ContentHandlerService.class);
		try {
	        contentHandler.registerContentHandler("none/none", OnDemandDocumentContentHandler.getInstance());
        }
        catch (Exception e) {
        	throw new OnDemandDocumentException(e.getMessage(), e);
        }
	}

	/**
	 * @see ModuleActivator#contextRefreshed()
	 */
	public void contextRefreshed() {
		this.registerSubscribers();
		this.registerContentHandlers();
		log.info("SHR ODD Module refreshed");
	}
	
	/**
	 * @see ModuleActivator#started()
	 */
	public void started() {
		this.registerSubscribers();
		this.registerContentHandlers();
		log.info("SHR ODD Module started");
		
	}
	
	/**
	 * @see ModuleActivator#stopped()
	 */
	public void stopped() {
		log.info("SHR ODD Module stopped");
	}
	
	/**
	 * @see ModuleActivator#willRefreshContext()
	 */
	public void willRefreshContext() {
		log.info("Refreshing SHR ODD Module");
	}
	
	/**
	 * @see ModuleActivator#willStart()
	 */
	public void willStart() {
		log.info("Starting SHR ODD Module");
	}
	
	/**
	 * @see ModuleActivator#willStop()
	 */
	public void willStop() {
		log.info("Stopping SHR ODD Module");
	}
		
}
