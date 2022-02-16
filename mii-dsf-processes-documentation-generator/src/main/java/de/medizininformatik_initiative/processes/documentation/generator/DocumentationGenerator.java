package de.medizininformatik_initiative.processes.documentation.generator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

public class DocumentationGenerator
{
	private static final Logger logger = LoggerFactory.getLogger(DocumentationGenerator.class);

	public static void main(String[] args)
	{
		new DocumentationGenerator().execute(args);
	}

	public void execute(String[] args)
	{
		Arrays.asList(args).forEach(this::generateDocumentation);
	}

	private void generateDocumentation(String configDirectory)
	{
		String filename = "target/Documentation_" + configDirectory + ".md";
		logger.info("Generating documentation for directory: {} in file: {}", configDirectory, filename);

		Reflections reflections = createReflections(configDirectory);
		List<Field> fields = getFields(reflections, Documentation.class);
		writeFields(fields, filename, configDirectory);
	}

	private Reflections createReflections(String configDirectory)
	{
		ConfigurationBuilder configurationBuilder = new ConfigurationBuilder()
				.setUrls(ClasspathHelper.forPackage(configDirectory)).setScanners(Scanners.FieldsAnnotated);
		return new Reflections(configurationBuilder);
	}

	private List<Field> getFields(Reflections reflections, Class<? extends Annotation> annotation)
	{
		List<Field> fields = new ArrayList<>(reflections.getFieldsAnnotatedWith(annotation));
		Collections.reverse(fields);
		return fields;
	}

	private void writeFields(List<Field> fields, String filename, String configDirectory)
	{
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename)))
		{
			fields.stream().map(this::createDocumentation).forEach(d -> write(writer, filename, d));
		}
		catch (IOException e)
		{
			logger.warn("Could not generate documentation for directory: {}, reason: {}", configDirectory,
					e.getMessage());
		}
	}

	private String createDocumentation(Field field)
	{
		Documentation documentation = field.getAnnotation(Documentation.class);
		Value value = field.getAnnotation(Value.class);

		String[] valueSplit = value.value().replaceAll("\\$", "").replace("#", "").replace("{", "").replace("}", "")
				.split(":");

		return String.format(
				"### %s \n **Property:** %s \n **Required:** %s \n **Processes:** %s \n **Description:** %s \n **Example:** %s \n **Recommendation:** %s \n **Default:** %s \n",
				documentation.environmentVariables(), valueSplit[0], documentation.required(),
				documentation.processNames(), documentation.description(), documentation.example(),
				documentation.recommendation(),
				(valueSplit.length == 2 && !"null".equals(valueSplit[1])) ? valueSplit[1] : "not set by default");
	}

	private void write(BufferedWriter writer, String filename, String string)
	{
		try
		{
			writer.append(string);
		}
		catch (IOException e)
		{
			logger.warn("Writing the following string to file {} failed: \n {} reason: {}", filename, string,
					e.getMessage());
		}
	}
}
