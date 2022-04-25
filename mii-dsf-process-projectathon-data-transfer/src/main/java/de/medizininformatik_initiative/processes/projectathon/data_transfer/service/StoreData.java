package de.medizininformatik_initiative.processes.projectathon.data_transfer.service;

import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE;
import static de.medizininformatik_initiative.processes.projectathon.data_transfer.ConstantsDataTransfer.BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE;
import static org.highmed.dsf.bpe.ConstantsBase.CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM;

import java.util.Objects;

import javax.ws.rs.core.MediaType;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.processes.projectathon.data_transfer.util.LoggingHelper;

public class StoreData extends AbstractServiceDelegate
{
	private static final Logger logger = LoggerFactory.getLogger(StoreData.class);

	private final EndpointProvider endpointProvider;

	public StoreData(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, EndpointProvider endpointProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.endpointProvider = endpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(endpointProvider, "endpointProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		byte[] bundleEncrypted = (byte[]) execution.getVariable(BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED);
		String coordinatingSiteIdentifier = (String) execution
				.getVariable(BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER);
		String projectIdentifier = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);

		Binary binary = createBinary(bundleEncrypted, coordinatingSiteIdentifier);
		String binaryId = storeBinary(binary);

		logger.info("Stored Binary with id='{}' provided for project-identifier='{}' referenced in Task with id='{}'",
				binaryId, projectIdentifier, getLeadingTaskFromExecutionVariables().getId());

		Target target = createTarget(coordinatingSiteIdentifier);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE, binaryId);
		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(target));
	}

	private Binary createBinary(byte[] content, String coordinatingSiteIdentifier)
	{
		Reference securityContext = new Reference();
		securityContext.setType(ResourceType.Organization.name()).getIdentifier()
				.setSystem(NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER).setValue(coordinatingSiteIdentifier);
		Binary binary = new Binary().setContentType(MediaType.APPLICATION_OCTET_STREAM)
				.setSecurityContext(securityContext).setData(content);

		LoggingHelper.logDebugBinary("Created Binary", binary);

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

	private Target createTarget(String coordinatingSiteIdentifier)
	{
		String endpointUrl = getEndpointUrl(coordinatingSiteIdentifier);
		return Target.createUniDirectionalTarget(coordinatingSiteIdentifier, endpointUrl);
	}

	private String getEndpointUrl(String identifier)
	{
		return endpointProvider.getFirstConsortiumEndpointAdress(
				NAMINGSYSTEM_HIGHMED_ORGANIZATION_IDENTIFIER_MEDICAL_INFORMATICS_INITIATIVE_CONSORTIUM,
				CODESYSTEM_HIGHMED_ORGANIZATION_ROLE, CODESYSTEM_HIGHMED_ORGANIZATION_ROLE_VALUE_COS, identifier).get();
	}
}
