package com.seraj.interview.configuration;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Properties;

import javax.activation.UnsupportedDataTypeException;

import org.apache.log4j.Logger;

public class ConfigurationReader {

	private static final Logger LOG = Logger
			.getLogger(ConfigurationReader.class);

	public static <T> void loadConfigurations(Properties properties,
			Class<T> classToConfigure, T instanceToConfigure) {
		for (Field field : classToConfigure.getDeclaredFields()) {
			LOG.debug("Looking at " + field);
			System.out.println("Annotations: "
					+ Arrays.asList(field.getAnnotations()));
			System.out.println("Declared annotations: "
					+ Arrays.asList(field.getDeclaredAnnotations()));
			if (field.isAnnotationPresent(MyConfigurable.class)) {
				String value = properties.getProperty(field.getName());
				LOG.debug("[Configure] No configurations found for " + field);
				if (value != null) {
					field.setAccessible(true);
					try {
						Object objectValue = valueToObject(value,
								field.getType());
						field.set(instanceToConfigure, objectValue);
						LOG.info("[Configure] Setting " + objectValue
								+ " into " + field);
					} catch (Exception e) {
						LOG.warn("[Configure] Failed to configure: " + field, e);
						continue;
					}
				}
			}
		}
	}

	private static Object valueToObject(String value, Class<?> type)
			throws UnsupportedDataTypeException {
		if (String.class.isAssignableFrom(type)) {
			return value;
		} else if (int.class.isAssignableFrom(type)) {
			return Integer.valueOf(value);
		} else if (float.class.isAssignableFrom(type)) {
			return Float.valueOf(value);
		}
		throw new UnsupportedDataTypeException(type
				+ " is not supported during configuration parsing.");
	}
}
