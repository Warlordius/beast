package com.github.beast.utility;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Encapsulates configuration settings used in BeAST project.
 * 
 * Configuration provides a singleton object, that holds all configuration
 * settings, read from a configuration file, provided during instantiation as
 * parameter of {@link #getInstance(String)} method. If no file is provided a
 * default file is used.
 * <p>
 * All configuration settings are provided with default values, which are used,
 * if no value are found in given configuration file.
 * 
 * @version 1.0
 * @author Štefan Sabo
 */
public final class Configuration {

	// default property values, all need to be string because of property parser
	private static final String DEFAULT_LOGGING = "true";
	private static final String DEFAULT_SEMANTICS = "true";
	private static final String DEFAULT_RESOURCE_DIR = "C:\\_beast_data\\";
	private static final String DEFAULT_DATABASE_DIR = "C:\\_beast_data\\graph\\";
	private static final String DEFAULT_LOG_FILE = "C:\\_beast_data\\log.txt\\";
	private static final String DEFAULT_WORDNET_LOCATION = "C:\\Program Files (x86)\\Wordnet\\2.1\\dict\\";
	private static final String DEFAULT_BEE_MESSAGES = "false";
	private static final String DEFAULT_BEE_ADD_THRESHOLD = "0.3";
	private static final String DEFAULT_REFRESH_DELAY = "1800000";
	private static final String DEFAULT_PROPERTIES_FILE = ".properties";

	// property names
	private static final String LOGGING = "use_logging";
	private static final String SEMANTICS = "use_semantics";
	private static final String RESOURCE_DIR = "resource_dir";
	private static final String DATABASE_DIR = "database_dir";
	private static final String WORDNET_LOCATION = "wordnet_dir";
	private static final String LOG_FILE = "log_file";
	private static final String BEE_MESSAGES = "bee_messages";
	private static final String BEE_ADD_THRESHOLD = "bee_add_threshold";
	private static final String REFRESH_DELAY = "bee_refresh_delay";

	private static Configuration instance;

	private boolean logging;
	private boolean semantics;
	private boolean beeMessages;
	private int refreshDelay;
	private double beeAddThreshold;
	private String databaseDir;
	private String logFile;
	private String resourceDir;
	private String wordnetDir;

	/**
	 * Class constructor, reads config file at the default location and sets
	 * configuration accordingly.
	 */
	private Configuration() {

		readConfigFile(DEFAULT_PROPERTIES_FILE);
	}

	/**
	 * Class constructor, reads specified config file and sets configuration
	 * accordingly.
	 * 
	 * @param path the path to the configuration file
	 */
	private Configuration(final String path) {

		readConfigFile(path);
	}

	/**
	 * Returns instance of the {@link Configuration} singleton class. If no
	 * instance exists, new instance is created using default properties file.
	 * 
	 * @return instance of the singleton class
	 */
	public static Configuration getInstance() {

		return getInstance(DEFAULT_PROPERTIES_FILE);
	}

	/**
	 * Returns instance of the {@link Configuration} singleton class. If no
	 * instance exists, new instance is created using specified properties file.
	 * 
	 * @param path the path to the properties file to be read
	 * @return instance of the singleton class
	 */
	public static Configuration getInstance(final String path) {

		if (instance == null) {
			return new Configuration(path);
		} else {
			return instance;
		}
	}

	/**
	 * 
	 * @return delay between refreshing a page, in milliseconds
	 */
	public int getRefreshDelay() {

		return refreshDelay;
	}

	/**
	 * @return 
	 */
	private double getBeeAddThreshold() {

		return beeAddThreshold;
	}

	public String getDatabaseDir() {

		return databaseDir;
	}

	public String getLogFile() {

		return logFile;
	}

	public String getResourceDir() {

		return resourceDir;
	}

	public String getWordnetDir() {

		return wordnetDir;
	}

	public boolean useLogging() {

		return logging;
	}

	public boolean useSemantics() {

		return semantics;
	}

	public boolean useBeeMessages() {

		return beeMessages;
	}

	/**
	 * Reads the specified config file and sets the values configuration
	 * settings. If no value is found for a given setting, default value is
	 * used. Exits if the specified file is not found.
	 * 
	 * @param path the path to the settings file
	 */
	private void readConfigFile(final String path) {

		Properties properties = new Properties();

		try {
			properties.load(new FileInputStream(path));
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace());
			System.exit(1);
		}

		logging = Boolean.parseBoolean(properties.getProperty(LOGGING, DEFAULT_LOGGING));
		semantics = Boolean.parseBoolean(properties.getProperty(SEMANTICS, DEFAULT_SEMANTICS));
		beeMessages = Boolean.parseBoolean(properties.getProperty(BEE_MESSAGES, DEFAULT_BEE_MESSAGES));

		refreshDelay = Integer.parseInt(properties.getProperty(REFRESH_DELAY, DEFAULT_REFRESH_DELAY));
		beeAddThreshold = Double.parseDouble(properties.getProperty(BEE_ADD_THRESHOLD, DEFAULT_BEE_ADD_THRESHOLD));

		resourceDir = properties.getProperty(RESOURCE_DIR, DEFAULT_RESOURCE_DIR);
		logFile = properties.getProperty(LOG_FILE, DEFAULT_LOG_FILE);
		databaseDir = properties.getProperty(DATABASE_DIR, DEFAULT_DATABASE_DIR);
		wordnetDir = properties.getProperty(WORDNET_LOCATION, DEFAULT_WORDNET_LOCATION);

	}
}