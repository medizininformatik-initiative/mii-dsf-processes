package de.medizininformatik_initiative.process.report.util;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_REPORT_STATUS;
import static de.medizininformatik_initiative.process.report.ConstantsReport.EXTENSION_REPORT_STATUS_ERROR_URL;

import org.hl7.fhir.r4.model.BackboneElement;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;

public class ReportStatusGenerator
{
	public ParameterComponent createReportStatusInput(String statusCode)
	{
		return createReportStatusInput(statusCode, null);
	}

	public ParameterComponent createReportStatusInput(String statusCode, String errorMessage)
	{
		ParameterComponent input = new ParameterComponent();
		input.setValue(new Coding().setSystem(CODESYSTEM_MII_REPORT_STATUS).setCode(statusCode));
		input.getType().addCoding().setSystem(CODESYSTEM_MII_REPORT).setCode(CODESYSTEM_MII_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(input, errorMessage);

		return input;
	}

	public TaskOutputComponent createReportStatusOutput(String statusCode)
	{
		return createReportStatusOutput(statusCode, null);
	}

	public TaskOutputComponent createReportStatusOutput(String statusCode, String errorMessage)
	{
		TaskOutputComponent output = new TaskOutputComponent();
		output.setValue(new Coding().setSystem(CODESYSTEM_MII_REPORT_STATUS).setCode(statusCode));
		output.getType().addCoding().setSystem(CODESYSTEM_MII_REPORT)
				.setCode(CODESYSTEM_MII_REPORT_VALUE_REPORT_STATUS);

		if (errorMessage != null)
			addErrorExtension(output, errorMessage);

		return output;
	}

	private void addErrorExtension(BackboneElement element, String errorMessage)
	{
		element.addExtension().setUrl(EXTENSION_REPORT_STATUS_ERROR_URL).setValue(new StringType(errorMessage));
	}

	public void transformInputToOutput(Task inputTask, Task outputTask)
	{
		inputTask.getInput().stream()
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> CODESYSTEM_MII_REPORT.equals(c.getSystem())
								&& CODESYSTEM_MII_REPORT_VALUE_REPORT_STATUS.equals(c.getCode())))
				.map(this::toTaskOutputComponent).forEach(outputTask::addOutput);
	}

	private TaskOutputComponent toTaskOutputComponent(ParameterComponent inputComponent)
	{
		TaskOutputComponent outputComponent = new TaskOutputComponent().setType(inputComponent.getType())
				.setValue(inputComponent.getValue());
		outputComponent.setExtension(inputComponent.getExtension());

		return outputComponent;
	}
}
