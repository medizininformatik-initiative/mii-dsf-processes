package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Binary;
import org.springframework.beans.factory.InitializingBean;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.process.projectathon.data_sharing.util.MimeTypeHelper;

public class ValidateDataSetExecute extends AbstractServiceDelegate implements InitializingBean
{
	private final MimeTypeHelper mimeTypeHelper;

	public ValidateDataSetExecute(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, MimeTypeHelper mimeTypeHelper)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.mimeTypeHelper = mimeTypeHelper;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(mimeTypeHelper, "mimeTypeHelper");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		Binary binary = (Binary) execution.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_BINARY);

		String mimeTypeBinary = binary.getContentType();
		byte[] dataBinary = binary.getData();

		mimeTypeHelper.validate(dataBinary, mimeTypeBinary);
	}
}

