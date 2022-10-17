package de.medizininformatik_initiative.process.kds.report.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.List;
import java.util.Objects;

import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.runtime.ProcessInstance;
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

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;

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
		logger.info("Stopping active instances of process with id '{}'",
				ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART);
		stopActiveInstancesOfProcess(execution);

		logger.debug("Setting variable '{}' to false",
				ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_STOP_TIMER);
		execution.setVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_STOP_TIMER,
				Variables.booleanValue(false));

		String timerInterval = getTimerInterval(execution);
		logger.debug("Setting variable '{}' to {}",
				ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_TIMER_INTERVAL, timerInterval);
		execution.setVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_TIMER_INTERVAL,
				Variables.stringValue(timerInterval));

		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET,
				TargetValues.create(Target.createUniDirectionalTarget(organizationProvider.getLocalIdentifierValue(),
						endpointProvider.getLocalEndpointIdentifier().getValue(),
						endpointProvider.getLocalEndpointAddress())));
	}

	private void stopActiveInstancesOfProcess(DelegateExecution execution)
	{
		RuntimeService runtimeService = execution.getProcessEngineServices().getRuntimeService();

		String currentInstanceId = execution.getActivityInstanceId();
		List<ProcessInstance> activeInstances = runtimeService.createProcessInstanceQuery()
				.processDefinitionKey(ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART).active().list();

		logger.debug("Found {} active instance{} of process with id '{}'{}", activeInstances.size(),
				activeInstances.size() == 1 ? "" : "s", ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART,
				activeInstances.size() == 0 ? ", nothing to delete"
						: activeInstances.size() == 1 ? ", deleting it" : ", deleting all of them");

		activeInstances.stream().filter(i -> !currentInstanceId.equals(i.getProcessInstanceId()))
				.forEach(i -> runtimeService.deleteProcessInstance(i.getProcessInstanceId(),
						"Only one process instance with id '"
								+ ConstantsKdsReport.PROCESS_NAME_FULL_KDS_REPORT_AUTOSTART + "' can exist"));
	}

	private String getTimerInterval(DelegateExecution execution)
	{
		return getTaskHelper()
				.getFirstInputParameterStringValue(getLeadingTaskFromExecutionVariables(execution),
						ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT,
						ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_TIMER_INTERVAL)
				.orElse(ConstantsKdsReport.KDS_REPORT_TIMER_INTERVAL_DEFAULT_VALUE);
	}
}
