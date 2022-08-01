package de.medizininformatik_initiative.process.kds.report.util;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.EXTENSION_KDS_REPORT_STATUS_ERROR_URL;

import java.util.stream.Stream;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class KdsReportStatusGenerator
{
	public ParameterComponent createKdsReportStatusInput(String statusCode)
	{
		return createKdsReportStatusInput(statusCode, null);
	}

	public ParameterComponent createKdsReportStatusInput(String statusCode, String errorMessage)
	{
		ParameterComponent input = new ParameterComponent();
		input.setValue(new Coding().setSystem(CODESYSTEM_MII_KDS_REPORT_STATUS).setCode(statusCode));
		input.getType().addCoding().setSystem(CODESYSTEM_MII_KDS_REPORT)
				.setCode(CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(input, errorMessage);

		return input;
	}

	public TaskOutputComponent createKdsReportStatusOutput(String statusCode)
	{
		return createKdsReportStatusOutput(statusCode, null);
	}

	public TaskOutputComponent createKdsReportStatusOutput(String statusCode, String errorMessage)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.setValue(new Coding().setSystem(CODESYSTEM_MII_KDS_REPORT_STATUS).setCode(statusCode));
		output.getType().addCoding().setSystem(CODESYSTEM_MII_KDS_REPORT)
				.setCode(CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(output, errorMessage);

		return output;
	}

	private void addErrorExtension(BackboneElement element, String errorMessage)
	{
		element.addExtension().setUrl(EXTENSION_KDS_REPORT_STATUS_ERROR_URL).setValue(new StringType(errorMessage));
	}

	public void transformInputToOutput(Task inputTask, Task outputTask)
	{
		transformInputToOutputComponents(inputTask).forEach(outputTask::addOutput);
	}

	public Stream<Task.TaskOutputComponent> transformInputToOutputComponents(Task inputTask)
	{
		return inputTask.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> CODESYSTEM_MII_KDS_REPORT.equals(c.getSystem())
								&& CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS.equals(c.getCode())))
				.map(this::toTaskOutputComponent);
	}

	private TaskOutputComponent toTaskOutputComponent(ParameterComponent inputComponent)
	{
		TaskOutputComponent outputComponent = new TaskOutputComponent().setType(inputComponent.getType())
				.setValue(inputComponent.getValue().copy());
		outputComponent.setExtension(inputComponent.getExtension());

		return outputComponent;
	}

	public void transformOutputToInput(Task outputTask, Task inputTask)
	{
		transformOutputToInputComponent(outputTask).forEach(inputTask::addInput);
	}

	public Stream<ParameterComponent> transformOutputToInputComponent(Task outputTask)
	{
		return outputTask.getOutput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> CODESYSTEM_MII_KDS_REPORT.equals(c.getSystem())
								&& CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS.equals(c.getCode())))
				.map(this::toTaskInputComponent);
	}

	private ParameterComponent toTaskInputComponent(TaskOutputComponent outputComponent)
	{
		ParameterComponent inputComponent = new ParameterComponent().setType(outputComponent.getType())
				.setValue(outputComponent.getValue().copy());
		inputComponent.setExtension(outputComponent.getExtension());

		return inputComponent;
	}
}
