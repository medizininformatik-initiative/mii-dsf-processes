package de.medizininformatik_initiative.process.report.message;

import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_HEALTH_RESEARCH_PLATFORM_IDENTIFIER;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;

public class StartSendReport extends AbstractTaskMessageSend
{
	public StartSendReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		Stream<Task.ParameterComponent> searchBundleStream = getLeadingTaskFromExecutionVariables().getInput().stream()
				.filter(Task.ParameterComponent::hasType)
				.filter(i -> i.getType().getCoding().stream().anyMatch(c -> CODESYSTEM_MII_REPORT.equals(c.getSystem())
						&& CODESYSTEM_MII_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE.equals(c.getCode())));

		Stream<Task.ParameterComponent> hrpIdentifierStream = getLeadingTaskFromExecutionVariables().getInput().stream()
				.filter(Task.ParameterComponent::hasType)
				.filter(i -> i.getType().getCoding().stream().anyMatch(c -> CODESYSTEM_MII_REPORT.equals(c.getSystem())
						&& CODESYSTEM_MII_REPORT_VALUE_HEALTH_RESEARCH_PLATFORM_IDENTIFIER.equals(c.getCode())));

		return Stream.concat(searchBundleStream, hrpIdentifierStream);
	}
}
