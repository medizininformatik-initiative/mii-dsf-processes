package de.medizininformatik_initiative.process.report.bpe;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.UUID;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.highmed.fhir.client.PreferReturnMinimalWithRetry;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.report.service.CheckSearchBundle;

@RunWith(MockitoJUnitRunner.class)
public class CheckSearchBundleServiceTest
{
	@Mock
	private DelegateExecution execution;

	@Mock
	private TaskHelper taskHelper;

	@Mock
	private FhirWebserviceClientProvider clientProvider;

	@Mock
	private FhirWebserviceClient webserviceClient;

	@Mock
	private PreferReturnMinimalWithRetry preferReturnMinimalWithRetry;

	@Mock
	private ProcessEngine processEngine;

	@Mock
	private RuntimeService runtimeService;

	@Captor
	ArgumentCaptor<String> system;

	@Captor
	ArgumentCaptor<String> code;

	@Captor
	ArgumentCaptor<String> error;

	@InjectMocks
	private CheckSearchBundle service;

	@Test
	public void testValid()
	{
		testValid("/fhir/Bundle/search-bundle.xml");

	}

	@Test
	public void testInvalidResource()
	{
		testInvalid("/fhir/Bundle/search-bundle-invalid-resource.xml", "resources");
	}

	@Test
	public void testInvalidRequestMethod()
	{
		testInvalid("/fhir/Bundle/search-bundle-invalid-request-method.xml", "GET");
	}

	@Test
	public void testInvalidNoSummary()
	{
		testInvalid("/fhir/Bundle/search-bundle-invalid-no-summary.xml", "_summary=count");
	}

	@Test
	public void testInvalidParam()
	{
		testInvalid("/fhir/Bundle/search-bundle-invalid-param.xml", "invalid search params");
	}

	private void testValid(String pathToBundle)
	{
		try (InputStream in = getClass().getResourceAsStream(pathToBundle))
		{
			Bundle bundle = FhirContext.forR4().newXmlParser().parseResource(Bundle.class, in);
			Mockito.when(execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE)).thenReturn(bundle);

			service.execute(execution);
		}
		catch (Exception exception)
		{
			fail();
		}
	}

	private void testInvalid(String pathToBundle, String errorContains)
	{
		Task task = (Task) new Task().setId(UUID.randomUUID().toString());

		try (InputStream in = getClass().getResourceAsStream(pathToBundle))
		{
			Bundle bundle = FhirContext.forR4().newXmlParser().parseResource(Bundle.class, in);
			Mockito.when(execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE)).thenReturn(bundle);
			Mockito.when(execution.getVariable("task")).thenReturn(task);
			Mockito.when(execution.getProcessDefinitionId()).thenReturn("processDefinitionId");
			Mockito.when(execution.getActivityInstanceId()).thenReturn("activityInstanceId");
			Mockito.when(clientProvider.getLocalWebserviceClient()).thenReturn(webserviceClient);
			Mockito.when(webserviceClient.withMinimalReturn()).thenReturn(preferReturnMinimalWithRetry);
			Mockito.when(preferReturnMinimalWithRetry.update(task)).thenReturn(new IdType(task.getId()));
			Mockito.when(execution.getProcessEngine()).thenReturn(processEngine);
			Mockito.when(processEngine.getRuntimeService()).thenReturn(runtimeService);

			service.execute(execution);
			Mockito.verify(taskHelper).createOutput(system.capture(), code.capture(), error.capture());

			assertEquals(CODESYSTEM_HIGHMED_BPMN, system.getValue());
			assertEquals(CODESYSTEM_HIGHMED_BPMN_VALUE_ERROR, code.getValue());
			assertTrue(error.getValue().contains(errorContains));
		}
		catch (Exception exception)
		{
			fail();
		}
	}
}
