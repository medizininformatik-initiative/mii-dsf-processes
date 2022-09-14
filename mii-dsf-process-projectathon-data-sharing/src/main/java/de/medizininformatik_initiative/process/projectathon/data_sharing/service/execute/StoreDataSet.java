package de.medizininformatik_initiative.process.projectathon.data_sharing.service.execute;

import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;

import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.process.projectathon.data_sharing.ConstantsDataSharing;
import de.medizininformatik_initiative.processes.kds.client.logging.DataLogger;

public class StoreDataSet extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreDataSet.class);

	private final DataLogger dataLogger;

	public StoreDataSet(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, DataLogger dataLogger)
	{
		super(clientProvider, taskHelper, readAccessHelper);
		this.dataLogger = dataLogger;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();
		Objects.requireNonNull(dataLogger, "dataLogger");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		String cosIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER);
		String projectIdentifier = (String) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		byte[] bundleEncrypted = (byte[]) execution
				.getVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);

		Binary binary = createBinary(bundleEncrypted, cosIdentifier);
		String binaryId = storeBinary(binary);

		logger.info(
				"Stored Binary with id='{}' provided for COS-identifier='{}' and project-identifier='{}' referenced in Task with id='{}'",
				binaryId, cosIdentifier, projectIdentifier, getLeadingTaskFromExecutionVariables().getId());

		execution.setVariable(ConstantsDataSharing.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE,
				Variables.stringValue(binaryId));
	}

	private Binary createBinary(byte[] content, String cosIdentifier)
	{
		Reference securityContext = new Reference();
		securityContext.setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(cosIdentifier);
		Binary binary = new Binary().setContentType(MediaType.APPLICATION_OCTET_STREAM)
				.setSecurityContext(securityContext).setData(content);

		dataLogger.logResource("Encrypted Binary", binary);

		return binary;
	}

	private String storeBinary(Binary binary)
	{
		IdType created = createBinaryResource(binary);
		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Binary.name(),
				created.getIdPart(), created.getVersionIdPart()).getValue();
	}

	private IdType createBinaryResource(Binary binary)
	{
		return getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn().create(binary);
	}
}
