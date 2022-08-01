package de.medizininformatik_initiative.process.projectathon.data_transfer.service;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Binary;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_transfer.ConstantsDataTransfer;
import de.medizininformatik_initiative.process.projectathon.data_transfer.util.MimeTypeHelper;

public class ValidateDataDic extends AbstractServiceDelegate implements InitializingBean
{
	private final OrganizationProvider organizationProvider;
	private final MimeTypeHelper mimeTypeHelper;

	public ValidateDataDic(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider, MimeTypeHelper mimeTypeHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
		this.mimeTypeHelper = mimeTypeHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(mimeTypeHelper, "mimeTypeHelper");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Binary binary = (Binary) execution.getVariable(ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_BINARY);

		String mimeTypeBinary = binary.getContentType();
		byte[] dataBinary = binary.getData();

		mimeTypeHelper.validate(dataBinary, mimeTypeBinary);
	}
}
