package de.medizininformatik_initiative.processes.projectathon.data_transfer.service;

import java.util.Objects;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.highmed.dsf.bpe.delegate.AbstractServiceDelegate;
import org.highmed.dsf.fhir.authorization.read.ReadAccessHelper;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.highmed.dsf.fhir.organization.OrganizationProvider;
import org.highmed.dsf.fhir.task.TaskHelper;
import org.springframework.beans.factory.InitializingBean;

public class ValidateDataDic extends AbstractServiceDelegate implements InitializingBean
{
	private final OrganizationProvider organizationProvider;

	public ValidateDataDic(FhirWebserviceClientProvider clientProvider, TaskHelper taskHelper,
			ReadAccessHelper readAccessHelper, OrganizationProvider organizationProvider)
	{
		super(clientProvider, taskHelper, readAccessHelper);

		this.organizationProvider = organizationProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationProvider, "organizationProvider");
	}

	@Override
	protected void doExecute(DelegateExecution execution)
	{
		// String projectIdentifier = (String) execution.getVariable(BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER);
		// DocumentReference documentReference = (DocumentReference) execution
		// .getVariable(BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE);
		// Binary binary = (Binary) execution.getVariable(BPMN_EXECUTION_VARIABLE_BINARY);
		//
		// binary.getContentType();
		// binary.getData();

		/*
		 * TODO validate declared mime-type = text/csv, if possible validate actual mime-type of data for example using
		 * Apache Tika -> https://tika.apache.org/1.21/detection.html
		 * 
		 * ...possible transfer of malicious binary data
		 */
	}
}
