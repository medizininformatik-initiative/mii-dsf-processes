package de.medizininformatik_initiative.process.projectathon.data_sharing.service.merge;

import static java.util.stream.Collectors.toList;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
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
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		Task task = getCurrentTaskFromExecutionVariables();
		String sendingOrganization = task.getRequester().getIdentifier().getValue();

		IdType dataSetReference = getDataSetReference(task);
		logger.info("Downloading data-set with id='{}' from organization='{}' for project-identifier='{}'",
				dataSetReference.getValue(), sendingOrganization, projectIdentifier);

		byte[] bundleEncrypted = readDataSet(dataSetReference);
		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED,
				Variables.byteArrayValue(bundleEncrypted));
	}

	private IdType getDataSetReference(Task task)
	{
		List<String> dataSetReferences = getTaskHelper()
				.getInputParameterReferenceValues(task, ConstantsDataSharing.CODESYSTEM_DATA_SHARING,
						ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_REFERENCE)
				.filter(Reference::hasReference).map(Reference::getReference).collect(toList());

		if (dataSetReferences.size() < 1)
			throw new IllegalArgumentException("No data-set reference present in task with id='" + task.getId() + "'");

		if (dataSetReferences.size() > 1)
			logger.warn("Found {} data-set references in task with id='{}', using only the first",
					dataSetReferences.size(), task.getId());

		return new IdType(dataSetReferences.get(0));
	}

	private byte[] readDataSet(IdType dataSetReference)
	{
		FhirWebserviceClient client = getFhirWebserviceClientProvider()
				.getWebserviceClient(dataSetReference.getBaseUrl());

		try (InputStream binary = readBinaryResource(client, dataSetReference.getIdPart(),
				dataSetReference.getVersionIdPart()))
		{
			return binary.readAllBytes();
		}
		catch (Exception exception)
		{
			throw new RuntimeException("Downloading Binary with id='" + dataSetReference.getValue() + "' failed.",
					exception);
		}
	}

	private InputStream readBinaryResource(FhirWebserviceClient client, String id, String version)
	{

		if (version != null && !version.isEmpty())
			return client.readBinary(id, version, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
		else
			return client.readBinary(id, MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM));
	}
}
