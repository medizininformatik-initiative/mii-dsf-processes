package de.medizininformatik_initiative.process.kds.report.service;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

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

import de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class InsertKdsReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(InsertKdsReport.class);

	private final KdsReportStatusGenerator kdsReportStatusGenerator;

	public InsertKdsReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, KdsReportStatusGenerator kdsReportStatusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.kdsReportStatusGenerator = kdsReportStatusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(kdsReportStatusGenerator, "kdsReportStatusGenerator");
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Task task = getLeadingTaskFromExecutionVariables();
		Identifier reportIdentifier = getReportIdentifier(task);

		Bundle report = (Bundle) execution
				.getVariable(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_SEARCH_BUNDLE);
		report.setId("").getMeta().setVersionId("").setTag(null);
		report.setIdentifier(reportIdentifier);

		getReadAccessHelper().addLocal(report);
		getReadAccessHelper().addOrganization(report, task.getRequester().getIdentifier().getValue());

		try
		{
			IdType reportId = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
					.updateConditionaly(report, Map.of("identifier", Collections
							.singletonList(reportIdentifier.getSystem() + "|" + reportIdentifier.getValue())));

			task.addOutput(kdsReportStatusGenerator
					.createKdsReportStatusOutput(ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_OK));
			updateLeadingTaskInExecutionVariables(task);

			logger.info("Stored report bundle with id '{}' from organization '{}'", reportId.getValue(),
					task.getRequester().getIdentifier().getValue());
		}
		catch (Exception exception)
		{
			task.setStatus(Task.TaskStatus.FAILED);
			task.addOutput(kdsReportStatusGenerator.createKdsReportStatusOutput(
					ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR, exception.getMessage()));
			updateLeadingTaskInExecutionVariables(task);

			logger.warn("Storing report from Task with id '{}' failed: {}", task.getId(), exception.getMessage());
			throw new BpmnError(ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR,
					exception.getMessage());
		}
	}

	private Identifier getReportIdentifier(Task task)
	{
		return new Identifier().setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER)
				.setValue(task.getRequester().getIdentifier().getValue());
	}
}
