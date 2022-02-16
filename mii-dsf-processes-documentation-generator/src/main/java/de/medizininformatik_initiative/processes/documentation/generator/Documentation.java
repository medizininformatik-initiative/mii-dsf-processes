package de.medizininformatik_initiative.processes.documentation.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Documentation
{
	String environmentVariables();

	boolean required();

	String processNames();

	String description();

	String example();

	String recommendation();
}
