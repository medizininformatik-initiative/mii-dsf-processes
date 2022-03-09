package de.medizininformatik_initiative.processes.documentation.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Documentation
{
	/**
	 * @return <code>true</code> if this property is required for processes that are listed in
	 *         {@link Documentation#processNames}
	 */
	boolean required() default false;

	/**
	 * @return <code>true</code> if a docker secret file can be used to configure this property, else
	 *         <code>false</code>, which means that a docker secret file <b>cannot</b> be used to configure this
	 *         property
	 */
	boolean filePropertySupported() default false;

	/**
	 * @return an empty array if all processes use this property or an array of length >=1 containing only specific
	 *         processes that use this property, but not all
	 */
	String[] processNames() default {};

	/**
	 * @return description helping to configure this property
	 */
	String description();

	/**
	 * @return example value helping to configure this property
	 */
	String example();

	/**
	 * @return recommendation helping to configure this property
	 */
	String recommendation();
}
