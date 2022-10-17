package de.medizininformatik_initiative.process.kds.report.message;

import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Task;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;

public class StartSendKdsReport extends AbstractTaskMessageSend
{
	public StartSendKdsReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		return getLeadingTaskFromExecutionVariables(execution).getInput().stream()
				.filter(Task.ParameterComponent::hasType)
				.filter(i -> i.getType().getCoding().stream()
						.anyMatch(c -> ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT.equals(c.getSystem())
								&& ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_SEARCH_BUNDLE_REFERENCE
										.equals(c.getCode())));
	}
}
