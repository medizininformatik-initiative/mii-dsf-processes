package de.medizininformatik_initiative.process.kds.report.service;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;

public class StopTimer extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StopTimer.class);

	public StopTimer(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
	}

	@Override
	protected void doExecute(DelegateExecution execution) throws Exception
	{
		logger.debug("Setting variable '{}' to true", ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_STOP_TIMER);
		execution.setVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_STOP_TIMER,
				Variables.booleanValue(true));
	}
}
