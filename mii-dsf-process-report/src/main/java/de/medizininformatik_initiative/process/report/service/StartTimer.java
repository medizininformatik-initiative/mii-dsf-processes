package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_STOP_TIMER;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_REPORT_TIMER_INTERVAL;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_VALUE_TIMER_INTERVAL;
import static de.medizininformatik_initiative.process.report.ConstantsReport.REPORT_TIMER_INTERVAL_DEFAULT_VALUE;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class StartTimer extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(StartTimer.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;

	public StartTimer(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		logger.debug("Setting variable '{}' to false", BPMN_EXECUTION_VARIABLE_REPORT_STOP_TIMER);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_REPORT_STOP_TIMER, Variables.booleanValue(false));

		String timerInterval = getTimerInterval();
		logger.debug("Setting variable '{}' to {}", BPMN_EXECUTION_VARIABLE_REPORT_TIMER_INTERVAL, timerInterval);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_REPORT_TIMER_INTERVAL, Variables.stringValue(timerInterval));

		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(organizationProvider.getLocalIdentifierValue(),
						endpointProvider.getLocalEndpointIdentifier().getValue(),
						endpointProvider.getLocalEndpointAddress())));
	}

	private String getTimerInterval()
	{
		return getTaskHelper().getFirstInputParameterStringValue(getLeadingTaskFromExecutionVariables(),
				CODESYSTEM_MII_REPORT, CODESYSTEM_MII_REPORT_VALUE_TIMER_INTERVAL)
				.orElse(REPORT_TIMER_INTERVAL_DEFAULT_VALUE);
	}
}