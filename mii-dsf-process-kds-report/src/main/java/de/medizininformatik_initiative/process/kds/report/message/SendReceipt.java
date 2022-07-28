package de.medizininformatik_initiative.process.kds.report.message;

import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_ERROR;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR;
import static de.medizininformatik_initiative.process.kds.report.ConstantsKdsReport.CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS;

import java.util.Objects;
import java.util.stream.Stream;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.AbstractTaskMessageSend;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Type;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.process.kds.report.util.KdsReportStatusGenerator;

public class SendReceipt extends AbstractTaskMessageSend implements InitializingBean
{
	private final KdsReportStatusGenerator kdsReportStatusGenerator;

	public SendReceipt(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, FhirContext fhirContext,
			KdsReportStatusGenerator kdsReportStatusGenerator)
	{
		super(clientProvider, taskHelper, readAccessHelper, organizationProvider, fhirContext);
		this.kdsReportStatusGenerator = kdsReportStatusGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(kdsReportStatusGenerator, "kdsReportStatusGenerator");
	}

	@Override
	protected Stream<Task.ParameterComponent> getAdditionalInputParameters(DelegateExecution execution)
	{
		if (execution.getVariable(BPMN_EXECUTION_VARIABLE_KDS_REPORT_RECEIVE_ERROR) != null)
			return createReceiptError();
		else
			return createReceiptOk();
	}

	private Stream<Task.ParameterComponent> createReceiptError()
	{
		return kdsReportStatusGenerator.transformOutputToInputComponent(getLeadingTaskFromExecutionVariables())
				.map(this::receiveToReceiptStatus);
	}

	private Task.ParameterComponent receiveToReceiptStatus(Task.ParameterComponent parameterComponent)
	{
		Type value = parameterComponent.getValue();
		if (value instanceof Coding)
		{
			Coding coding = (Coding) value;
			if (CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIVE_ERROR.equals(coding.getCode()))
			{
				coding.setCode(CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_ERROR);
			}
		}

		return parameterComponent;
	}

	private Stream<Task.ParameterComponent> createReceiptOk()
	{
		Task.ParameterComponent parameterComponent = new Task.ParameterComponent();
		parameterComponent.getType().addCoding().setSystem(CODESYSTEM_MII_KDS_REPORT)
				.setCode(CODESYSTEM_MII_KDS_REPORT_VALUE_REPORT_STATUS);
		parameterComponent.setValue(new Coding().setSystem(CODESYSTEM_MII_KDS_REPORT_STATUS)
				.setCode(CODESYSTEM_MII_KDS_REPORT_STATUS_VALUE_RECEIPT_OK));

		return Stream.of(parameterComponent);
	}
}
