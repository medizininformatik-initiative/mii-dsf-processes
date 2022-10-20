package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import static java.util.stream.Collectors.toList;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.BasicFhirWebserviceClient;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class DownloadDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DownloadDataSet.class);

	public DownloadDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		Task task = getCurrentTaskFromExecutionVariables(execution);
		String sendingOrganization = task.getRequester().getIdentifier().getValue();
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		IdType dataSetReference = getDataSetReference(task);

		logger.info(
				"Downloading data-set with id '{}' from organization '{}' for data-sharing project '{}' in Task with id '{}'",
				dataSetReference.getValue(), sendingOrganization, projectIdentifier, task.getId());

		try
		{
			byte[] bundleEncrypted = readDataSet(dataSetReference);
			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED,
					Variables.byteArrayValue(bundleEncrypted));
		}
		catch (Exception exception)
		{
			String message = "Could not download data-set with id '" + dataSetReference.getValue()
					+ "' from organization '" + sendingOrganization + "' and data-sharing project '" + projectIdentifier
					+ "' referenced in Task with id '" + task.getId() + "' - " + exception.getMessage();

			execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR_MESSAGE,
					Variables.stringValue(message));

			throw new BpmnError(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR, message,
					exception);
		}
	}

	private IdType getDataSetReference(Task task)
	{
		List<String> dataSetReferences = getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_REFERENCE)
				.filter(Reference::hasReference).map(Reference::getReference).collect(toList());

		if (dataSetReferences.size() < 1)
			throw new IllegalArgumentException("No data-set reference present in Task with id '" + task.getId() + "'");

		if (dataSetReferences.size() > 1)
			logger.warn("Found {} data-set references in Task with id '{}', using only the first",
					dataSetReferences.size(), task.getId());

		return new IdType(dataSetReferences.get(0));
	}

	private byte[] readDataSet(IdType dataSetReference)
	{
		BasicFhirWebserviceClient client = getFhirWebserviceClientProvider()
				.getWebserviceClient(dataSetReference.getBaseUrl())
				.withRetry(ConstantsDataSharing.DSF_CLIENT_RETRY_TIMES,
						ConstantsDataSharing.DSF_CLIENT_RETRY_INTERVAL_5MIN);

		try (InputStream binary = readBinaryResource(client, dataSetReference.getIdPart(),
				dataSetReference.getVersionIdPart()))
		{
			return binary.readAllBytes();
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Downloading Binary with id '" + dataSetReference.getValue() + "' failed",
					exception);
		}
	}

	private InputStream readBinaryResource(BasicFhirWebserviceClient client, String id, String version)
	{
		if (version != null && !version.isEmpty())
			return client.readBinary(id, version, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
		else
			return client.readBinary(id, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
	}
}
