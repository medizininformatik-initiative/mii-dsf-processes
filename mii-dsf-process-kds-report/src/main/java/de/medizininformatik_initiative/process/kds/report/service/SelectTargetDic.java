package de.medizininformatik_initiative.process.kds.report.service;

import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;
import static org.highmed.dsf.bpe.ConstantsBase.NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.EndpointProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.highmed.dsf.fhir.variables.TargetValues;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Task;
import org.springframework.beans.factory.InitializingBean;

public class SelectTargetDic extends AbstractServiceDelegate implements InitializingBean
{
	private final EndpointProvider endpointProvider;

	public SelectTargetDic(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
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
		Task task = getLeadingTaskFromExecutionVariables(execution);
		String dicIdentifier = getDicOrganizationIdentifier(task);
		Endpoint dicEndpoint = getDicEndpoint(dicIdentifier);
		Target dicTarget = createTarget(dicIdentifier, dicEndpoint);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_TARGET, TargetValues.create(dicTarget));
	}

	private String getDicOrganizationIdentifier(Task task)
	{
		return task.getRequester().getIdentifier().getValue();
	}

	private Endpoint getDicEndpoint(String dicIdentifier)
	{
		return endpointProvider.getFirstDefaultEndpoint(dicIdentifier).orElseThrow(
				() -> new RuntimeException("Could not find default endpoint of organization '" + dicIdentifier + "'"));
	}

	private Target createTarget(String dicIdentifier, Endpoint dicEndpoint)
	{
		String dicEndpointIdentifier = getEndpointIdentifierValue(dicEndpoint);
		return Target.createUniDirectionalTarget(dicIdentifier, dicEndpointIdentifier, dicEndpoint.getAddress());
	}

	private String getEndpointIdentifierValue(Endpoint endpoint)
	{
		return endpoint.getIdentifier().stream()
				.filter(i -> NAMINGSYSTEM_HIGHMED_ENDPOINT_IDENTIFIER.equals(i.getSystem())).findFirst()
				.map(Identifier::getValue).orElseThrow(() -> new RuntimeException(
						"No endpoint identifier found in endpoint with id '" + endpoint.getId() + "'"));
	}
}
