package de.medizininformatik_initiative.process.report.service;

import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE;
import static de.medizininformatik_initiative.process.report.ConstantsReport.NAMING_SYSTEM_MII_REPORT;
import static org.highmed.dsf.bpe.ConstantsBase.BPMN_EXECUTION_VARIABLE_TARGET;

import java.util.Collections;
import java.util.Map;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.variable.Variables;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.highmed.dsf.fhir.variables.Target;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class CreateReport extends AbstractServiceDelegate implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(CreateReport.class);

	private final OrganizationProvider organizationProvider;

	public CreateReport(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
	}

	@Override
	protected void doExecute(DelegateExecution delegateExecution)
	{
		Bundle searchBundle = (Bundle) execution.getVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE);
		Target target = (Target) execution.getVariable(BPMN_EXECUTION_VARIABLE_TARGET);

		Bundle responseBundle = executeSearchBundle(searchBundle, target);
		String responseBundleReference = storeResponseBundle(responseBundle);

		execution.setVariable(BPMN_EXECUTION_VARIABLE_SEARCH_BUNDLE_RESPONSE_REFERENCE,
				Variables.stringValue(responseBundleReference));
	}

	private Bundle executeSearchBundle(Bundle searchBundle, Target target)
	{
		// TODO Execute search Bundle against KDS FHIR Store
		Bundle bundle = new Bundle();
		bundle.setType(Bundle.BundleType.BATCHRESPONSE);
		bundle.getIdentifier().setSystem(NAMING_SYSTEM_MII_REPORT)
				.setValue("Report_" + organizationProvider.getLocalIdentifierValue());

		getReadAccessHelper().addOrganization(bundle, target.getOrganizationIdentifierValue());
		getReadAccessHelper().addOrganization(bundle, organizationProvider.getLocalIdentifierValue());

		return bundle;
	}

	private String storeResponseBundle(Bundle responseBundle)
	{
		IdType bundleIdType = getFhirWebserviceClientProvider().getLocalWebserviceClient().withMinimalReturn()
				.updateConditionaly(responseBundle, Map.of("identifier", Collections.singletonList(
						NAMING_SYSTEM_MII_REPORT + "|Report_" + organizationProvider.getLocalIdentifierValue())));

		logger.info("Stored report bundle with id {}", bundleIdType.getValue());

		return new IdType(getFhirWebserviceClientProvider().getLocalBaseUrl(), ResourceType.Bundle.name(),
				bundleIdType.getIdPart(), bundleIdType.getVersionIdPart()).getValue();
	}
}
