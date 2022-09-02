package de.medizininformatik_initiative.process.projectathon.data_sharing;

import static de.medizininformatik_initiative.process.projectathon.data_sharing.DataSharingProcessPluginDefinition.VERSION;

public interface ConstantsDataSharing
{
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
	String SEND_DATA_SET_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/sendDataSet";
	String SEND_DATA_SET_PROCESS_URI_WITH_LATEST_VERSION = SEND_DATA_SET_PROCESS_URI + "/" + VERSION;
	String SEND_DATA_SET_MESSAGE_NAME = "sendDataSet";

	String SEND_MERGED_DATA_SET_TASK_PROFILE = "http://medizininformatik-initiative.de/fhir/StructureDefinition/task-send-merged-data-set";
	String SEND_MERGED_DATA_SET_TASK_PROFILE_WITH_LATEST_VERSION = SEND_MERGED_DATA_SET_TASK_PROFILE + "|" + VERSION;
	String SEND_MERGED_DATA_SET_PROCESS_URI = "http://medizininformatik-initiative.de/bpe/Process/sendMergedDataSet";
	String SEND_MERGED_DATA_SET_PROCESS_URI_WITH_LATEST_VERSION = SEND_MERGED_DATA_SET_PROCESS_URI + "/" + VERSION;
	String SEND_MERGED_DATA_SET_MESSAGE_NAME = "sendMergedDataSet";

	// CODESYSTEM DATA SHARING

	String CODESYSTEM_DATA_SHARING = "http://medizininformatik-initiative.de/fhir/CodeSystem/data-sharing";
	String CODESYSTEM_DATA_SHARING_VALUE_MEDIC_IDENTIFIER = "medic-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_MEDIC_CORRELATION_KEY = "medic-correlation-key";
	String CODESYSTEM_DATA_SHARING_VALUE_COS_IDENTIFIER = "cos-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_PROJECT_IDENTIFIER = "project-identifier";
	String CODESYSTEM_DATA_SHARING_VALUE_CONTRACT_LOCATION = "contract-location";

	// NAMINGSYSTEM PROJECT IDENTIFIER

	String NAMINGSYSTEM_PROJECT_IDENTIFIER = "http://medizininformatik-initiative.de/sid/project-identifier";

	// BPMN VARIABLES

	String BPMN_EXECUTION_VARIABLE_PROJECT_IDENTIFIER = "project-identifier";
	String BPMN_EXECUTION_VARIABLE_COS_IDENTIFIER = "cos-identifier";
	String BPMN_EXECUTION_VARIABLE_CONTRACT_LOCATION = "contract-location";
}
