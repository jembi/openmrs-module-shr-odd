package org.openmrs.module.shr.odd.api.impl.test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.marc.everest.formatters.FormatterUtil;
import org.marc.everest.interfaces.IResultDetail;
import org.marc.everest.rmim.uv.cdar2.pocd_mt000040uv.ClinicalDocument;
import org.marc.everest.rmim.uv.cdar2.rim.InfrastructureRoot;
import org.openmrs.GlobalProperty;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.shr.cdahandler.api.CdaImportService;
import org.openmrs.module.shr.cdahandler.configuration.CdaHandlerConfiguration;
import org.openmrs.module.shr.cdahandler.exception.DocumentImportException;
import org.openmrs.module.shr.cdahandler.exception.DocumentValidationException;
import org.openmrs.module.shr.odd.api.OnDemandDocumentService;
import org.openmrs.module.shr.odd.model.OnDemandDocumentRegistration;
import org.openmrs.test.BaseModuleContextSensitiveTest;
import org.openmrs.util.OpenmrsConstants;
import org.slf4j.LoggerFactory;

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
		GlobalProperty saveDir = new GlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_COMPLEX_OBS_DIR, "C:\\data\\");
		Context.getAdministrationService().setGlobalProperty(CdaHandlerConfiguration.PROP_VALIDATE_CONCEPT_STRUCTURE, "false");
		Context.getAdministrationService().setGlobalProperty("order.nextOrderNumberSeed", "1");
		Context.getAdministrationService().setGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_FALSE_CONCEPT, "1066");
		Context.getAdministrationService().saveGlobalProperty(saveDir);
		initializeInMemoryDatabase();
		executeDataSet(ACTIVE_LIST_INITIAL_XML);
		executeDataSet(CIEL_LIST_INITIAL_XML);
	}
	
	/**
	 * Do the parsing of a CDA
	 */
	private String doParseCda(String resourceName)
	{
		URL validAphpSample = this.getClass().getResource(resourceName);
		File fileUnderTest = new File(validAphpSample.getFile());
		FileInputStream fs = null;
		try
		{
			fs = new FileInputStream(fileUnderTest);
			Visit parsedVisit = this.m_importService.importDocument(fs);
			assertEquals(parsedVisit, Context.getVisitService().getVisitByUuid(parsedVisit.getUuid()));
			return parsedVisit.getUuid();
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
	 * Test the generation of an APS document
	 */
	public void testGenerateAps()
	{
		
		// First import the APS document
		this.doParseCda("validAphpSamplFullSections.xml");
		
		// Get patient information by name
		List<Patient> patient = Context.getPatientService().getPatients("Sarah Levin");
		Assert.assertEquals(1, patient.size());
		List<OnDemandDocumentRegistration> oddDocuments = this.m_oddService.getOnDemandDocumentRegistrationsByPatient(patient.get(0));
		Assert.assertEquals(1, oddDocuments.size());
		
		// Generate the odd
		try {
	        ClinicalDocument generatedDocument = this.m_oddService.generateOnDemandDocument(oddDocuments.get(0));
	        // Assert a few items in the generated document
        }
        catch (Exception e) {
        	fail(e.getMessage());
        }
		
	}
}
