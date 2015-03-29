package com.seraj.interview.configuration;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestConfigurationReader {

	private static Properties properties;
	private static String stringValue = "string";
	private static String stringValue2 = "anotherString";
	private static int intValue = 123;
	private static long longValue = 456;

	@BeforeClass
	public static void setupProperties() {
		properties = new Properties();
		properties.setProperty("stringField", stringValue);
		properties.setProperty("intField", "" + intValue);
		properties.setProperty("longField", "" + longValue);
		properties.setProperty("notAConfigurableField", stringValue2);
	}

	@Test
	public void testLoadConfigurations() {
		ConfigurableTestClass testClass = new ConfigurableTestClass();
		ConfigurationReader.loadConfigurations(properties,
				ConfigurableTestClass.class, testClass);
		Assert.assertEquals(stringValue, testClass.stringField);
		Assert.assertEquals(intValue, testClass.intField);
		Assert.assertEquals(longValue, testClass.longField);
		Assert.assertNull(testClass.notConfiguredField);
		Assert.assertNull(testClass.notAConfigurableField);
	}

	private final class ConfigurableTestClass {
		@Configurable
		protected String stringField;
		@Configurable
		protected int intField;
		@Configurable
		protected long longField;

		// This field is marked for configuration but is not configured in the
		// properties.
		@Configurable
		protected String notConfiguredField;
		// This field is not marked for configuration but a configuration exists
		// in the properties file.
		protected String notAConfigurableField;
	}
}
