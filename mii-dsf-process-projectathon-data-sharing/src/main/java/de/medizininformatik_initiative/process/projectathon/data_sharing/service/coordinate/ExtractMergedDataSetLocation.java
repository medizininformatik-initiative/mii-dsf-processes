package de.medizininformatik_initiative.process.projectathon.data_sharing.service.coordinate;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.UrlType;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;

public class ExtractMergedDataSetLocation extends AbstractServiceDelegate
{
	public ExtractMergedDataSetLocation(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task currenTask = getCurrentTaskFromExecutionVariables();
		Task leadingTask = getLeadingTaskFromExecutionVariables();

		String dataSetLocation = currenTask.getInput().stream().filter(i -> i.getType().getCoding().stream()
				.anyMatch(c -> ConstantsDataSharing.CODESYSTEM_DATA_SHARING.equals(c.getSystem())
						&& ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION.equals(c.getCode())))
				.map(Task.ParameterComponent::getValue).filter(t -> t instanceof UrlType).map(t -> (UrlType) t)
				.map(PrimitiveType::getValue).findFirst().orElseThrow(() -> new RuntimeException(
						"Task does not contain contract-location-input: " + currenTask.getId()));

		Task.TaskOutputComponent dataSetLocationOutput = new Task.TaskOutputComponent();
		dataSetLocationOutput.getType().addCoding().setSystem(ConstantsDataSharing.CODESYSTEM_DATA_SHARING)
				.setCode(ConstantsDataSharing.CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION);
		dataSetLocationOutput.setValue(new UrlType().setValue(dataSetLocation));

		leadingTask.addOutput(dataSetLocationOutput);
		updateLeadingTaskInExecutionVariables(leadingTask);
	}
}
