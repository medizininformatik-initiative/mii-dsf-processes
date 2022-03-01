package de.medizininformatik_initiative.processes.documentation.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
/*
 * TODO add default values to properties where possible or a typical default value exists (required = false,
 * recommendation = null)
 */
public @interface Documentation
{
	/*
	 * TODO remove, should be generated using @Value annotation, if needed add a boolean property to configure that a
	 * ..._PASSWORD_FILE env Variable is not supported (boolean filePropertySupported() default true;)
	 */
	String environmentVariables();

	boolean required();

	/*
	 * TODO change to String Array, default null -> all processes (Generator could use
	 * ProcessPluginDefinition#getBpmnFiles to parse BPMN file and find process names)
	 * 
	 * Generator should validate process names against configured processes
	 */
	String processNames();

	String description();

	String example();

	String recommendation();
}
