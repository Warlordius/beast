package com.github.configuration;

import java.io.*;
import java.util.Properties;

/**
 * Class encapsulating configuration settings used in BeAST project.
 * Configuration provides a singleton object, that holds all configuration
 * settings, read from a configuration file, provided during instantiation as
 * parameter of {@link #getInstance(String)} method. If no file is provided a
 * default file is used.
 * <p>
 * All configuration settings are provided with default values, which are used,
 * if no value are found in given configuration file.
 * 
 * @version 0.1
 * @author Štefan Sabo
 */
public class Configuration {

    // default property values
    private static final String DEFAULT_LOGGING = "true";				// needs to be string, because of property parser
    private static final String DEFAULT_SEMANTICS = "true";				// needs to be string, because of property parser
    private static final String DEFAULT_RESOURCE_DIR = "C:\\_beast_data\\";
    private static final String DEFAULT_DATABASE_DIR = "C:\\_beast_data\\graph\\";
    private static final String DEFAULT_LOG_FILE = "C:\\_beast_data\\log.txt\\";
    private static final String DEFAULT_PROPERTIES_FILE = ".properties";

    // property names
    private static final String LOGGING = "use_logging";
    private static final String SEMANTICS = "use_semantics";
    private static final String RESOURCE_DIR = "resource_dir";
    private static final String DATABASE_DIR = "database_dir";
    private static final String LOG_FILE = "log_file";

    private static Configuration instance;

    private boolean logging;
    private boolean semantics;
    private String databaseDir;
    private String logFile;
    private String resourceDir;

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
    private Configuration(String path) {

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
    public static Configuration getInstance(String path) {

	if (instance == null) {
	    return new Configuration(path);
	} else {
	    return instance;
	}
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

    public boolean useLogging() {

	return logging;
    }

    public boolean useSemantics() {

	return semantics;
    }

    /**
     * Reads the specified config file and sets the values configuration
     * settings. If no value is found for a given setting, default value is
     * used. Exits if the specified file is not found.
     * 
     * @param path the path to the settings file
     */
    private void readConfigFile(String path) {

	Properties properties = new Properties();

	try {
	    properties.load(new FileInputStream(path));

	} catch (FileNotFoundException e) {
	    System.out.println(e.getMessage());
	    System.exit(1);
	} catch (IOException e) {
	    System.out.println(e.getMessage());
	    System.out.println(e.getStackTrace());
	}

	logging = Boolean.parseBoolean(properties.getProperty(LOGGING, DEFAULT_LOGGING));
	semantics = Boolean.parseBoolean(properties.getProperty(SEMANTICS, DEFAULT_SEMANTICS));
	resourceDir = properties.getProperty(RESOURCE_DIR, DEFAULT_RESOURCE_DIR);
	logFile = properties.getProperty(LOG_FILE, DEFAULT_LOG_FILE);
	databaseDir = properties.getProperty(DATABASE_DIR, DEFAULT_DATABASE_DIR);
    }
}