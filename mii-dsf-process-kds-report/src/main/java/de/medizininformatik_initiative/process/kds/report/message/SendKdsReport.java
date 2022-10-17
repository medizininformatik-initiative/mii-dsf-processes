package de.medizininformatik_initiative.process.kds.report.message;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE_REFERENCE;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_ALLOWED;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE;

import java.util.Objects;
import java.util.stream.Stream;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class SendKdsReport extends AbstractTaskMessageSend implements InitializingBean
{
	private final KdsReportStatusGenerator statusGenerator;

	public SendKdsReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext,
			KdsReportStatusGenerator statusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);

		this.statusGenerator = statusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(statusGenerator, "statusGenerator");
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		String bundleId = (String) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE_RESPONSE_REFERENCE);

		Task.ParameterComponent parameterComponent = new Task.ParameterComponent();
		parameterComponent.getType().addCoding().setSystem(CODESYSTEM_MII_KDS_REPORT)
				.setCode(CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_RESPONSE_REFERENCE);
		parameterComponent.setValue(new Reference(bundleId).setType(ResourceType.Bundle.name()));

		return Stream.of(parameterComponent);
	}

	@Override
	protected void handleIntermediateThrowEventError(DelegateExecution execution, Exception exception,
			String errorMessage)
	{
		Task task = getLeadingTaskFromExecutionVariables(execution);

		if (task != null)
		{
			String statusCode = CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_REACHABLE;
			if (exception instanceof WebApplicationException)
			{
				WebApplicationException webApplicationException = (WebApplicationException) exception;
				if (webApplicationException.getResponse() != null && webApplicationException.getResponse()
						.getStatus() == Response.Status.FORBIDDEN.getStatusCode())
				{
					statusCode = CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_NOT_ALLOWED;
				}
			}

			task.addOutput(statusGenerator.createKdsReportStatusOutput(statusCode, createErrorMessage(exception)));
			updateLeadingTaskInExecutionVariables(execution, task);
		}

		super.handleIntermediateThrowEventError(execution, exception, errorMessage);
	}

	@Override
	protected void addErrorMessage(Task task, String errorMessage)
	{
		// nothing to do here
	}

	private String createErrorMessage(Exception exception)
	{
		return exception.getClass().getSimpleName()
				+ ((exception.getMessage() != null && !exception.getMessage().isBlank())
						? (": " + exception.getMessage())
						: "");
	}
}
