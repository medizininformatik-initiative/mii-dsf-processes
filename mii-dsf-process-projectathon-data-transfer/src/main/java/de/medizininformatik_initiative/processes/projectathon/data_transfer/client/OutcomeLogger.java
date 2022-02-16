package de.medizininformatik_initiative.processes.projectathon.data_transfer.client;

import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.slf4j.Logger;

public class OutcomeLogger
{
	private final Logger logger;

	public OutcomeLogger(Logger logger)
	{
		this.logger = logger;
	}

	public void logOutcome(OperationOutcome outcome)
	{
		outcome.getIssue().forEach(issue ->
		{
			String display = issue.getCode() == null ? null : issue.getCode().getDisplay();
			String details = issue.getDetails() == null ? null : issue.getDetails().getText();
			String diagnostics = issue.getDiagnostics();

			String message = Stream.of(display, details, diagnostics).filter(s -> s != null && !s.isBlank())
					.collect(Collectors.joining(" "));

			getLoggerForSeverity(issue.getSeverity(), logger).accept("Issue: {}", message);
		});
	}

	private BiConsumer<String, Object> getLoggerForSeverity(IssueSeverity severity, Logger logger)
	{
		if (severity != null)
		{
			switch (severity)
			{
				case ERROR:
				case FATAL:
					return logger::error;
				case WARNING:
					return logger::warn;
				case NULL:
				case INFORMATION:
				default:
					return logger::info;
			}
		}

		return logger::info;
	}
}
