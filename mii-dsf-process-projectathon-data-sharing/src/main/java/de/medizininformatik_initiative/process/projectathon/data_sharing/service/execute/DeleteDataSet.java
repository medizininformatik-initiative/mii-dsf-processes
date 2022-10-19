package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.fhir.client.FhirWebserviceClient;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class DeleteDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(DeleteDataSet.class);

	public DeleteDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		IdType binaryId = new IdType(
				(String) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE));

		deletePermanently(binaryId);

		logger.info(
				"Permanently deleted encrypted Binary with id '{}' provided for COS '{}' and  data-sharing project '{}' "
						+ "referenced in Task with id '{}'",
				binaryId.getValue(), cosIdentifier, projectIdentifier,
				getLeadingTaskFromExecutionVariables(execution).getId());
	}

	private void deletePermanently(IdType binaryId)
	{
		FhirWebserviceClient client = getFhirWebserviceClientProvider().getLocalWebserviceClient();
		client.delete(Binary.class, binaryId.getIdPart());
		client.deletePermanently(Binary.class, binaryId.getIdPart());
	}
}
