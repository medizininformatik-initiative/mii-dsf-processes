package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.BasicFhirWebserviceClient;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;

public class DeleteData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteData.class);

	public DeleteData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String coordinatingSiteIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		IdType binaryId = new IdType(
				(String) execution.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE));

		logger.info(
				"Permanently deleting encrypted Binary with id '{}' provided for COS '{}' and data-transfer project '{}' "
						+ "referenced in Task with id '{}'",
				binaryId.getValue(), coordinatingSiteIdentifier, projectIdentifier,
				getLeadingTaskFromExecutionVariables(execution).getId());

		try
		{
			deletePermanently(binaryId);
		}
		catch (Exception exception)
		{
			logger.warn(
					"Could not permanently delete data-set for COS '{}' and data-transfer project '{}' referenced in Task with id '{}' - {}",
					coordinatingSiteIdentifier, projectIdentifier, task.getId(), exception.getMessage());
			throw new RuntimeException("Could not permanently delete data-set for COS '" + coordinatingSiteIdentifier
					+ "' and data-transfer project '" + projectIdentifier + "' referenced in Task with id '"
					+ task.getId() + "'", exception);
		}
	}

	private void deletePermanently(IdType binaryId)
	{
		BasicFhirWebserviceClient client = getFhirWebserviceClientProvider().getLocalWebserviceClient().withRetry(
				ConstantsDataTransfer.DSF_CLIENT_RETRY_TIMES, ConstantsDataTransfer.DSF_CLIENT_RETRY_INTERVAL_5MIN);
		client.delete(Binary.class, binaryId.getIdPart());
		client.deletePermanently(Binary.class, binaryId.getIdPart());
	}
}
