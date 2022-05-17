package de.medizininformatik_initiative.process.projectathon.data_transfer;

import static de.medizininformatik_initiative.process.projectathon.data_transfer.DataTransferProcessPluginDefinition.VERSION;

public interface ConstantsDataTransfer
{
	String PROCESS_NAME_DATA_SEND = "dataSend";
	String PROCESS_NAME_DATA_RECEIVE = "dataReceive";

	String PROCESS_NAME_FULL_DATA_SEND = "medizininformatik-initiativede_" + PROCESS_NAME_DATA_SEND;
	String PROCESS_NAME_FULL_DATA_RECEIVE = "medizininformatik-initiativede_" + PROCESS_NAME_DATA_RECEIVE;

	String BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER = "project-identifier";
	String BPMN_EXECUTION_VARIABLE_COORDINATING_SITE_IDENTIFIER = "coordinating-site-identifier";
	String BPMN_EXECUTION_VARIABLE_DATA_SET = "data-set";
	String BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED = "data-set-encrypted";
	String BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE = "data-set-reference";
	String BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE = "document-reference";
	String BPMN_EXECUTION_VARIABLE_BINARY = "binary";

	String NAMINGSYSTEM_MII_PROJECT_IDENTIFIER = "http://medizininformatik-initiative.de/sid/project-identifier";

	String CODESYSTEM_MII_DATA_TRANSFER = "http://medizininformatik-initiative.de/fhir/CodeSystem/data-transfer";
	String CODESYSTEM_MII_DATA_TRANSFER_VALUE_COORDINATING_SITE_IDENTIFIER = "coordinating-site-identifier";
	String CODESYSTEM_MII_DATA_TRANSFER_VALUE_PROJECT_IDENTIFIER = "project-identifier";
	String CODESYSTEM_MII_DATA_TRANSFER_VALUE_DATA_SET_REFERENCE = "data-set-reference";
	String CODESYSTEM_MII_DATA_TRANSFER_VALUE_DOCUMENT_REFERENCE_LOCATION = "document-reference-location";

	String CODESYSTEM_MII_CRYPTOGRAPHY = "http://medizininformatik-initiative.de/fhir/CodeSystem/cryptography";
	String CODESYSTEM_MII_CRYPTOGRAPHY_VALUE_PUBLIC_KEY = "public-key";

	String PROFILE_MII_TASK_START_DATA_SEND = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-start-data-send";
	String PROFILE_MII_TASK_START_DATA_SEND_AND_LATEST_VERSION = PROFILE_MII_TASK_START_DATA_SEND + "|" + VERSION;
	String PROFILE_MII_TASK_DATA_SEND_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/dataSend/";
	String PROFILE_MII_TASK_DATA_SEND_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_TASK_DATA_SEND_PROCESS_URI + VERSION;
	String PROFILE_MII_TASK_START_DATA_SEND_MESSAGE_NAME = "startDataSendMii";

	String PROFILE_MII_TASK_START_DATA_RECEIVE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-start-data-receive";
	String PROFILE_MII_TASK_START_DATA_RECEIVE_AND_LATEST_VERSION = PROFILE_MII_TASK_START_DATA_RECEIVE + "|" + VERSION;
	String PROFILE_MII_TASK_DATA_RECEIVE_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/dataReceive/";
	String PROFILE_MII_TASK_DATA_RECEIVE_PROCESS_URI_AND_LATEST_VERSION = PROFILE_MII_TASK_DATA_RECEIVE_PROCESS_URI
			+ VERSION;
	String PROFILE_MII_TASK_START_DATA_RECEIVE_MESSAGE_NAME = "startDataReceiveMii";
}
