package de.medizininformatik_initiative.processes.test.data.generator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.medizininformatik_initiative.processes.test.data.generator.CertificateGenerator.CertificateFiles;

public class EnvGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(EnvGenerator.class);

	private static final String USER_THUMBPRINTS = "USER_THUMBPRINTS";
	private static final String USER_THUMBPRINTS_PERMANENTDELETE = "USER_THUMBPRINTS_PERMANENT_DELETE";

	private static final class EnvEntry
	{
		final String userThumbprintsVariableName;
		final Stream<String> userThumbprints;
		final String userThumbprintsPermanentDeleteVariableName;
		final Stream<String> userThumbprintsPermanentDelete;

		EnvEntry(String userThumbprintsVariableName, Stream<String> userThumbprints,
				String userThumbprintsPermanentDeleteVariableName, Stream<String> userThumbprintsPermanentDelete)
		{
			this.userThumbprintsVariableName = userThumbprintsVariableName;
			this.userThumbprints = userThumbprints;
			this.userThumbprintsPermanentDeleteVariableName = userThumbprintsPermanentDeleteVariableName;
			this.userThumbprintsPermanentDelete = userThumbprintsPermanentDelete;
		}
	}

	public void generateAndWriteDockerTestFhirEnvFiles(Map<String, CertificateFiles> clientCertificateFilesByCommonName)
	{
		Stream<String> dicUserThumbprints = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "dic-client",
				"Webbrowser Test User");
		Stream<String> dicUserThumbprintsPermanentDelete = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"dic-client", "Webbrowser Test User");

		Stream<String> cosUserThumbprints = filterAndMapToThumbprint(clientCertificateFilesByCommonName, "cos-client",
				"Webbrowser Test User");
		Stream<String> cosUserThumbprintsPermanentDelete = filterAndMapToThumbprint(clientCertificateFilesByCommonName,
				"cos-client", "Webbrowser Test User");

		List<EnvEntry> entries = List.of(
				new EnvEntry("DIC_" + USER_THUMBPRINTS, dicUserThumbprints, "DIC_" + USER_THUMBPRINTS_PERMANENTDELETE,
						dicUserThumbprintsPermanentDelete),
				new EnvEntry("COS_" + USER_THUMBPRINTS, cosUserThumbprints, "COS_" + USER_THUMBPRINTS_PERMANENTDELETE,
						cosUserThumbprintsPermanentDelete));

		writeEnvFile(Paths.get("../mii-dsf-processes-docker-test-setup/.env"), entries);
	}

	private Stream<String> filterAndMapToThumbprint(Map<String, CertificateFiles> clientCertificateFilesByCommonName,
			String... commonNames)
	{
		return clientCertificateFilesByCommonName.entrySet().stream()
				.filter(entry -> Arrays.asList(commonNames).contains(entry.getKey()))
				.sorted(Comparator.comparing(e -> Arrays.asList(commonNames).indexOf(e.getKey()))).map(Entry::getValue)
				.map(CertificateFiles::getCertificateSha512ThumbprintHex);
	}

	private void writeEnvFile(Path target, List<? extends EnvEntry> entries)
	{
		StringBuilder builder = new StringBuilder();

		for (int i = 0; i < entries.size(); i++)
		{
			EnvEntry entry = entries.get(i);

			builder.append(entry.userThumbprintsVariableName);
			builder.append('=');
			builder.append(entry.userThumbprints.collect(Collectors.joining(",")));
			builder.append('\n');
			builder.append(entry.userThumbprintsPermanentDeleteVariableName);
			builder.append('=');
			builder.append(entry.userThumbprintsPermanentDelete.collect(Collectors.joining(",")));

			if ((i + 1) < entries.size())
				builder.append("\n\n");
		}

		try
		{
			logger.info("Writing .env file to {}", target.toString());
			Files.writeString(target, builder.toString());
		}
		catch (IOException e)
		{
			logger.error("Error while writing .env file to " + target.toString(), e);
			throw new RuntimeException(e);
		}
	}
}
