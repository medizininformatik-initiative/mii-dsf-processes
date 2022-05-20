package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE;

import javax.ws.rs.WebApplicationException;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.FhirResourceValues;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadSearchBundle extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadSearchBundle.class);

	public DownloadSearchBundle(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		String searchBundleReference = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_REFERENCE);
		IdType searchBundleId = new IdType(searchBundleReference);

		Bundle bundle = readSearchBundle(searchBundleId);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE, FhirResourceValues.create(bundle));
	}

	private Bundle readSearchBundle(IdType searchBundleId)
	{
		Task task = getLeadingTaskFromExecutionVariables();

		FhirWebserviceClient client = getFhirWebserviceClientProvider()
				.getWebserviceClient(searchBundleId.getBaseUrl());

		try
		{
			if (searchBundleId.hasVersionIdPart())
				return client.read(Bundle.class, searchBundleId.getIdPart(), searchBundleId.getVersionIdPart());
			else
				return client.read(Bundle.class, searchBundleId.getIdPart());
		}
		catch (WebApplicationException exception)
		{
			logger.error("Error while reading search Bundle with id {} from organization {}: {}",
					searchBundleId.getValue(), task.getRequester().getReference(), exception.getMessage());
			throw new RuntimeException("Error while reading search Bundle with id " + searchBundleId.getValue()
					+ " from organization " + task.getRequester().getReference() + ", " + exception.getMessage(),
					exception);
		}
	}
}
