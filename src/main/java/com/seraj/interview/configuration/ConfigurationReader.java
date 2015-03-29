package com.seraj.interview.configuration;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Properties;

import javax.activation.UnsupportedDataTypeException;

import org.apache.log4j.Logger;

/**
 * A static helper class designed to interpret marker {@link Annotation}s used
 * to identify fields that can be configured using a java {@link Properties}.
 * 
 * @author Seraj Dosenbach
 *
 */
public class ConfigurationReader {

	private static final Logger LOG = Logger
			.getLogger(ConfigurationReader.class);

	/**
	 * For each {@link Field} annotated with {@link Configurable}, set its value
	 * to that found in the given java {@link Properties}; using the
	 * {@link Field} name locate the appropriate property. If no property exists
	 * then the {@link Field} remains unchanged.
	 * 
	 * @param properties
	 * @param classToConfigure
	 * @param instanceToConfigure
	 */
	public static <T> void loadConfigurations(Properties properties,
			Class<T> classToConfigure, T instanceToConfigure) {
		for (Field field : classToConfigure.getDeclaredFields()) {
			if (field.isAnnotationPresent(Configurable.class)) {
				String value = properties.getProperty(field.getName());
				if (value != null) {
					field.setAccessible(true);
					try {
						Object objectValue = stringToTypedObject(value,
								field.getType());
						field.set(instanceToConfigure, objectValue);
						LOG.info("[Configure] Setting '" + objectValue
								+ "' into " + field);
					} catch (Exception e) {
						LOG.warn("[Configure] Failed to configure: " + field
								+ " with value: " + value, e);
						continue;
					}
				} else {
					LOG.debug("[Configure] No configurations found for "
							+ field);
				}
			}
		}
	}

	/**
	 * A helper method used to convert the value, represented in a String, into
	 * an {@link Object} of the given type.
	 * 
	 * @param value
	 *            The value in string form
	 * @param type
	 *            The type to which the value should be converted.
	 * @return The {@link Object} form of the value.
	 * @throws UnsupportedDataTypeException
	 *             Thrown when the type is unsupported.
	 */
	private static Object stringToTypedObject(String value, Class<?> type)
			throws UnsupportedDataTypeException {
		if (String.class.isAssignableFrom(type)) {
			return value;
		} else if (int.class.isAssignableFrom(type)) {
			return Integer.valueOf(value);
		} else if (long.class.isAssignableFrom(type)) {
			return Long.valueOf(value);
		}
		throw new UnsupportedDataTypeException(type
				+ " is not supported during configuration parsing.");
	}
}
