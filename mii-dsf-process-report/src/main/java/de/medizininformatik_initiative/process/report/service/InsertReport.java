package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_RECEIVE_ERROR;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIVE_ERROR;
import static de.medizininformatik_initiative.process.report.ConstantsReport.NAMING_SYSTEM_MII_REPORT;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.camunda.bpm.engine.delegate.BpmnError;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.report.util.ReportStatusGenerator;

public class InsertReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(InsertReport.class);

	private final ReportStatusGenerator reportStatusGenerator;

	public InsertReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, ReportStatusGenerator reportStatusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.reportStatusGenerator = reportStatusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(reportStatusGenerator, "reportStatusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task task = getLeadingTaskFromExecutionVariables();

		Bundle report = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE);
		report.setId("").getMeta().setVersionId("").setTag(null);

		Identifier reportIdentifier = getReportIdentifier(task);
		report.setIdentifier(reportIdentifier);

		getReadAccessHelper().addLocal(report);
		getReadAccessHelper().addOrganization(report, task.getRequester().getIdentifier().getValue());

		try
		{
			IdType reportId = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.updateConditionaly(report, Map.of("identifier", Collections
							.singletonList(reportIdentifier.getSystem() + "|" + reportIdentifier.getValue())));

			logger.info("Stored report with id='{}'...", reportId.getValue());
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(reportStatusGenerator.createReportStatusOutput(
					CODESYSTEM_MII_REPORT_STATUS_VALUE_RECEIVE_ERROR, exception.getMessage()));
			updateLeadingTaskInExecutionVariables(task);

			logger.warn("Storing report from Task with id '{}' failed: {}", task.getId(), exception.getMessage());
			throw new BpmnError(BPMN_EXECUTION_VARIABLE_RECEIVE_ERROR, exception.getMessage());
		}
	}

	private Identifier getReportIdentifier(Task task)
	{
		return new Identifier().setSystem(NAMING_SYSTEM_MII_REPORT)
				.setValue("Report_" + task.getRequester().getIdentifier().getValue());
	}
}
