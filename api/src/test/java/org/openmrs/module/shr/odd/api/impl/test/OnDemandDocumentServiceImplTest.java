package org.openmrs.module.shr.odd.api.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.interfaces.IResultDetail;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.rim.InfrastructureRoot;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptSource;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.CdaHandlerConstants;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.exception.DocumentValidationException;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.api.test.CdaLoggingUtils;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.module.shr.odd.subscriber.AntepartumSubscriber;
import org.openmrs.module.shr.odd.subscriber.GenericDocumentSubscriber;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;

/**
 * On Demand Document module test
 */
public class OnDemandDocumentServiceImplTest extends BaseModuleContextSensitiveTest {

	// The service under test
	private OnDemandDocumentService m_oddService;
	private CdaImportService m_importService;
	// Log
	private final Log log = LogFactory.getLog(this.getClass());
	
	private static final String ACTIVE_LIST_INITIAL_XML = "include/OnDemandTest.xml";
	
	private static final String CIEL_LIST_INITIAL_XML = "include/CielList.xml";

	/**
	 * Setup the database and get necessary services
	 * @throws Exception 
	 */
	@Before
	public void beforeTest() throws Exception
	{
		this.m_oddService = Context.getService(OnDemandDocumentService.class);
		this.m_importService = Context.getService(CdaImportService.class);
		super.clearHibernateCache();
		super.initializeInMemoryDatabase();
		GlobalProperty saveDir = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_COMPLEX_OBS_DIR, "C:\\data\\");
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_DEFAULT_LOCATION_NAME, "Elbonia Shared Health Authority DC");
		Context.getAdministrationService().setGlobalProperty(CdaHandlerConfiguration.PROP_VALIDATE_CONCEPT_STRUCTURE, "false");
		Context.getAdministrationService().setGlobalProperty("order.nextOrderNumberSeed", "1");
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT, "1066");
		Context.getAdministrationService().saveGlobalProperty(saveDir);
		executeDataSet(ACTIVE_LIST_INITIAL_XML);
		executeDataSet(CIEL_LIST_INITIAL_XML);
		// Register the handler for a generic
		this.m_importService.subscribeImport(null, GenericDocumentSubscriber.getInstance());
		this.m_importService.subscribeImport(CdaHandlerConstants.DOC_TEMPLATE_ANTEPARTUM_SUMMARY, AntepartumSubscriber.getInstance());
		this.m_importService.subscribeImport(CdaHandlerConstants.DOC_TEMPLATE_ANTEPARTUM_HISTORY_AND_PHYSICAL, AntepartumSubscriber.getInstance());

	}
	
	/**
	 * Do the parsing of a CDA
	 */
	private Visit doParseCda(String resourceName)
	{
		
		URL validAphpSample = this.getClass().getResource(resourceName);
		File fileUnderTest = new File(validAphpSample.getFile());
		FileInputStream fs = null;
		try
		{
			fs = new FileInputStream(fileUnderTest);
			Visit parsedVisit = this.m_importService.importDocument(fs);
			assertEquals(parsedVisit, Context.getVisitService().getVisitByUuid(parsedVisit.getUuid()));
			return parsedVisit;
		}
		catch(DocumentValidationException e)
		{
			log.error(String.format("Error in %s", FormatterUtil.toWireFormat(((InfrastructureRoot)e.getTarget()).getTemplateId())));
			for(IResultDetail dtl : e.getValidationIssues())
				log.error(String.format("%s %s", dtl.getType(), dtl.getMessage()));
			return null;
		}
		catch(DocumentImportException e)
		{
			log.error("Error generated", e);
			return null;
		}
        catch (FileNotFoundException e) {
	        // TODO Auto-generated catch block
	        log.error("Error generated", e);
	        return null;
        }

	}
	
	/**
	 * Test that the trigger event was fired
	 */
	@Test
	public void testWasTriggerFired()
	{
		// First import the APS document
		Visit visit = this.doParseCda("/validAphpSampleFullSections.xml");
		// Get patient information by name
		List<Patient> patient = Context.getPatientService().getPatients("Sarah Levin");
		Assert.assertEquals(1, patient.size());
		List<OnDemandDocumentRegistration> oddDocuments = this.m_oddService.getOnDemandDocumentRegistrationsByPatient(patient.get(0));
		Assert.assertEquals(2, oddDocuments.size());
	}
	
	/**
	 * Test the generation of an CCD document
	 */
	@Test
	public void testGenerateAPSAndCCDDocument()
	{
		// Register the handler for a generic
		// First import the APS document
		Visit visit = this.doParseCda("/validAphpSampleFullSections.xml");
		// Get patient information by name
		List<Patient> patient = Context.getPatientService().getPatients("Sarah Levin");
		Assert.assertEquals(1, patient.size());
		List<OnDemandDocumentRegistration> oddDocuments = this.m_oddService.getOnDemandDocumentRegistrationsByPatient(patient.get(0));
		Assert.assertEquals(2, oddDocuments.size());
		// Generate CCD
		try {
	        ClinicalDocument doc = this.m_oddService.generateOnDemandDocument(oddDocuments.get(0));
	        log.error(CdaLoggingUtils.getCdaAsString(doc));
	        doc = this.m_oddService.generateOnDemandDocument(oddDocuments.get(1));
	        log.error(CdaLoggingUtils.getCdaAsString(doc));

        }
        catch (Exception e) {
	        log.error("Error generated", e);
	        fail(e.getMessage());
        }
	}
	
	
	/**
	 * Test the generation of an CCD document with two encounters
	 */
	@Test
	public void testGenerateCCDTwoEncounters()
	{
		// First import the APS document
		Visit visit1 = this.doParseCda("/validAphpSamplePovich.xml"),
				visit2 = this.doParseCda("/validAphpSamplePovich2.xml");
		// Get patient information by name
		List<Patient> patient = Context.getPatientService().getPatients("Mary Levin");
		Assert.assertEquals(1, patient.size());
		List<OnDemandDocumentRegistration> oddDocuments = this.m_oddService.getOnDemandDocumentRegistrationsByPatient(patient.get(0));
		// One APS one CCD
		Assert.assertEquals(3, oddDocuments.size());
		Assert.assertEquals(2, oddDocuments.get(1).getEncounterLinks().size());
		
		// Generate CCD
		try {
	        ClinicalDocument doc = this.m_oddService.generateOnDemandDocument(oddDocuments.get(0));
	        log.error(CdaLoggingUtils.getCdaAsString(doc));
        }
        catch (Exception e) {
	        log.error("Error generated", e);
	        fail(e.getMessage());
        }
	}

	
	/**
	 * Test the generation of a CCD document with problems and meds
	 *
	 */
	@Test
	public void testGenerateCCDFullProblemsAndMeds()
	{
		// First import the APS document
		Visit visit1 = this.doParseCda("/validCdaLevel3Sample.xml");
		// Get patient information by name
		List<Patient> patient = Context.getPatientService().getPatients("Patty");
		Assert.assertEquals(1, patient.size());
		List<OnDemandDocumentRegistration> oddDocuments = this.m_oddService.getOnDemandDocumentRegistrationsByPatient(patient.get(0));
		Assert.assertEquals(1, oddDocuments.size());
		Assert.assertEquals(1, oddDocuments.get(0).getEncounterLinks().size());
		
		// Generate CCD
		try {
	        ClinicalDocument doc = this.m_oddService.generateOnDemandDocument(oddDocuments.get(0));
	        log.error(CdaLoggingUtils.getCdaAsString(doc));
        }
        catch (Exception e) {
	        log.error("Error generated", e);
	        fail(e.getMessage());
        }
		
	}
	

}
