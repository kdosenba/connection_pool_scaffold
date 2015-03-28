package com.seraj.interview.configuration;

import java.util.Properties;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

public class TestConfigurationReader {

	private static Properties properties;
	private static String stringValue = "string";
	private static int intValue = 123;
	private static long longValue = 456;

	@BeforeClass
	public static void setupProperties() {
		properties = new Properties();
		properties.setProperty("stringField", "string");
		properties.setProperty("intField", "" + 123);
		properties.setProperty("longField", "" + 456);
	}

	@Test
	public void testLoadConfigurations() {
		ConfigurableTestClass testClass = new ConfigurableTestClass();
		ConfigurationReader.loadConfigurations(properties,
				ConfigurableTestClass.class, testClass);
		Assert.assertEquals(stringValue, testClass.stringField);
		Assert.assertEquals(intValue, testClass.intField);
		Assert.assertEquals(longValue, testClass.longField);
	}

	private final class ConfigurableTestClass {
		@MyConfigurable
		protected String stringField;
		@MyConfigurable
		protected int intField;
		@MyConfigurable
		protected long longField;
	}
}
