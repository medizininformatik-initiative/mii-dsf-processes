package de.medizininformatik_initiative.process.projectathon.data_sharing;

import static de.medizininformatik_initiative.process.projectathon.data_sharing.DataSharingProcessPluginDefinition.VERSION;

public interface ConstantsDataSharing
{
	// BPMN VARIABLES

	String BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER = "projectIdentifier";
	String BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION = "contractLocation";
	String BPMN_EXECUTION_VARIABLE_EXTRACTION_INTERVAL = "extractionInterval";
	String BPMN_EXECUTION_VARIABLE_RESEARCHER_IDENTIFIERS = "researcherIdentifiers";
	String BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER = "cosIdentifier";
	String BPMN_EXECUTION_VARIABLE_DOCUMENT_REFERENCE = "documentReference";
	String BPMN_EXECUTION_VARIABLE_DATA_RESOURCE = "dataResource";
	String BPMN_EXECUTION_VARIABLE_DATA_SET = "dataSet";
	String BPMN_EXECUTION_VARIABLE_DATA_SET_ENCRYPTED = "dataSetEncrypted";
	String BPMN_EXECUTION_VARIABLE_DATA_SET_REFERENCE = "dataSetReference";
	String BPMN_EXECUTION_VARIABLE_DATA_SET_LOCATION = "dataSetLocation";
	String BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR = "executeDataSharingError";
	String BPMN_EXECUTION_VARIABLE_DATA_SHARING_EXECUTE_ERROR_MESSAGE = "executeDataSharingErrorMessage";
	String BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR = "mergeDataSharingError";
	String BPMN_EXECUTION_VARIABLE_DATA_SHARING_MERGE_ERROR_MESSAGE = "mergeDataSharingErrorMessage";

	// NAMINGSYSTEMS

	String NAMINGSYSTEM_RESEARCHER_IDENTIFIER = "http://medizininformatik-initiative.de/sid/researcher-identifier";
	String NAMINGSYSTEM_PROJECT_IDENTIFIER = "http://medizininformatik-initiative.de/sid/project-identifier";

	// CODESYSTEM DATA SHARING

	String CODESYSTEM_DATA_SHARING = "http://medizininformatik-initiative.de/fhir/CodeSystem/data-sharing";
	String CODESYSTEM_DATA_SHARING_VALUE_RESEARCHER_IDENTIFIER = "researcher-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER = "medic-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY = "medic-correlation-key";
	String CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER = "cos-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER = "project-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION = "contract-location";
	String CODESYSTEM_DATA_SHARING_VALUE_EXTRACTION_INTERVAL = "extraction-interval";
	String CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_LOCATION = "data-set-location";
	String CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_REFERENCE = "data-set-reference";
	String CODESYSTEM_DATA_SHARING_VALUE_DATA_SET_MISSING = "data-set-missing";
	String CODESYSTEM_DATA_SHARING_VALUE_DOCUMENT_REFERENCE_REFERENCE = "document-reference-reference";

	// CODESYSTEM CRYPTOGRAPHY

	String CODESYSTEM_CRYPTOGRAPHY = "http://medizininformatik-initiative.de/fhir/CodeSystem/cryptography";
	String CODESYSTEM_CRYPTOGRAPHY_VALUE_PUBLIC_KEY = "public-key";

	// EXTENSIONS

	String EXTENSION_URL_MEDIC_IDENTIFIER = "http://medizininformatik-initiative.de/fhir/Extension/medic-identifier";

	// QUESTIONNAIRE RELEASE DATA-SET

	String QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_RELEASE = "release";
	String QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DATA_SET_URL = "data-set-url";
	String QUESTIONNAIRES_RELEASE_DATA_SET_ITEM_DISPLAY = "display";
	String QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_PROJECT_IDENTIFIER = "{project-identifier-placeholder}";
	String QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_COS_IDENTIFIER = "{cos-identifier-placeholder}";
	String QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_KDS_STORE_BASE_URL = "{kds-store-base-url-placeholder}";
	String QUESTIONNAIRES_RELEASE_DATA_SET_PLACEHOLDER_NEW_LINE = "{new-line-placeholder}";

	// OTHERS

	String DATA_EXTRACTION_INTERVAL_DEFAULT_VALUE = "P28D";

	int DSF_CLIENT_RETRY_TIMES = 6;
	long DSF_CLIENT_RETRY_INTERVAL_5MIN = 300000;

	// PROCESS NAMES

	String PROCESS_NAME_COORDINATE_DATA_SHARING = "coordinateDataSharing";
	String PROCESS_NAME_FULL_COORDINATE_DATA_SHARING = "medizininformatik-initiativede_"
			+ PROCESS_NAME_COORDINATE_DATA_SHARING;

	String PROCESS_NAME_EXECUTE_DATA_SHARING = "executeDataSharing";
	String PROCESS_NAME_FULL_EXECUTE_DATA_SHARING = "medizininformatik-initiativede_"
			+ PROCESS_NAME_EXECUTE_DATA_SHARING;

	String PROCESS_NAME_MERGE_DATA_SHARING = "mergeDataSharing";
	String PROCESS_NAME_FULL_MERGE_DATA_SHARING = "medizininformatik-initiativede_" + PROCESS_NAME_MERGE_DATA_SHARING;

	// TASK PROFILES & PROCESS URIs

	String COORDINATE_DATA_SHARING_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-coordinate-data-sharing";
	String COORDINATE_DATA_SHARING_TASK_PROFILE_WITH_LATEST_VERSION = COORDINATE_DATA_SHARING_TASK_PROFILE + "|"
			+ VERSION;
	String COORDINATE_DATA_SHARING_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/coordinateDataSharing";
	String COORDINATE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION = COORDINATE_DATA_SHARING_PROCESS_URI + "/"
			+ VERSION;
	String COORDINATE_DATA_SHARING_MESSAGE_NAME = "coordinateDataSharing";

	String EXECUTE_DATA_SHARING_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-execute-data-sharing";
	String EXECUTE_DATA_SHARING_TASK_PROFILE_WITH_LATEST_VERSION = EXECUTE_DATA_SHARING_TASK_PROFILE + "|" + VERSION;
	String EXECUTE_DATA_SHARING_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/executeDataSharing";
	String EXECUTE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION = EXECUTE_DATA_SHARING_PROCESS_URI + "/" + VERSION;
	String EXECUTE_DATA_SHARING_MESSAGE_NAME = "executeDataSharing";

	String MERGE_DATA_SHARING_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-merge-data-sharing";
	String MERGE_DATA_SHARING_TASK_PROFILE_WITH_LATEST_VERSION = MERGE_DATA_SHARING_TASK_PROFILE + "|" + VERSION;
	String MERGE_DATA_SHARING_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/mergeDataSharing";
	String MERGE_DATA_SHARING_PROCESS_URI_WITH_LATEST_VERSION = MERGE_DATA_SHARING_PROCESS_URI + "/" + VERSION;
	String MERGE_DATA_SHARING_MESSAGE_NAME = "mergeDataSharing";

	String SEND_DATA_SET_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-send-data-set";
	String SEND_DATA_SET_TASK_PROFILE_WITH_LATEST_VERSION = SEND_DATA_SET_TASK_PROFILE + "|" + VERSION;
	String SEND_DATA_SET_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/mergeDataSharing";
	String SEND_DATA_SET_PROCESS_URI_WITH_LATEST_VERSION = SEND_DATA_SET_PROCESS_URI + "/" + VERSION;
	String SEND_DATA_SET_MESSAGE_NAME = "sendDataSet";

	String SEND_RECEIVED_DATA_SET_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-send-received-data-set";
	String SEND_RECEIVED_DATA_SET_TASK_PROFILE_WITH_LATEST_VERSION = SEND_RECEIVED_DATA_SET_TASK_PROFILE + "|"
			+ VERSION;
	String SEND_RECEIVED_DATA_SET_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/coordinateDataSharing";
	String SEND_RECEIVED_DATA_SET_PROCESS_URI_WITH_LATEST_VERSION = SEND_RECEIVED_DATA_SET_PROCESS_URI + "/" + VERSION;
	String SEND_RECEIVED_DATA_SET_MESSAGE_NAME = "sendReceivedDataSet";

	String SEND_MERGED_DATA_SET_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-send-merged-data-set";
	String SEND_MERGED_DATA_SET_TASK_PROFILE_WITH_LATEST_VERSION = SEND_MERGED_DATA_SET_TASK_PROFILE + "|" + VERSION;
	String SEND_MERGED_DATA_SET_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/coordinateDataSharing";
	String SEND_MERGED_DATA_SET_PROCESS_URI_WITH_LATEST_VERSION = SEND_MERGED_DATA_SET_PROCESS_URI + "/" + VERSION;
	String SEND_MERGED_DATA_SET_MESSAGE_NAME = "sendMergedDataSet";
}
