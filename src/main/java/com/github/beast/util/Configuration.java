package com.github.beast.util;

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
	private static final String DEFAULT_PAGE_ARCHIVE = "true";
	private static final String DEFAULT_MAIN_DIR = "C:\\_beast_data\\";
	private static final String DEFAULT_DATABASE_DIR = "C:\\_beast_data\\graph\\";
	private static final String DEFAULT_LOG_FILE = "C:\\_beast_data\\log.txt\\";
	private static final String DEFAULT_PAGE_ARCHIVE_DIR = "C:\\_beast_data\\pages\\";
	private static final String DEFAULT_WORDNET_LOCATION = "C:\\Program Files (x86)\\Wordnet\\2.1\\dict\\";
	private static final String DEFAULT_BEE_MESSAGES = "false";
	private static final String DEFAULT_REFRESH_DELAY = "1800000";
	private static final String DEFAULT_REQUEST_DELAY = "5000";
	private static final String DEFAULT_PROPERTIES_FILE = ".properties";
	private static final String DEFAULT_TAGGER_PATH = "tagger\\english-left3words-distsim.tagger";
	

	// property names
	private static final String LOGGING = "use_logging";
	private static final String SEMANTICS = "use_semantics";
	private static final String PAGE_ARCHIVE = "use_page_archive";
	private static final String MAIN_DIR = "main_dir";
	private static final String DATABASE_DIR = "database_dir";
	private static final String WORDNET_LOCATION = "wordnet_dir";
	private static final String LOG_FILE = "log_file";
	private static final String BEE_MESSAGES = "bee_messages";
	private static final String REFRESH_DELAY = "bee_refresh_delay";
	private static final String REQUEST_DELAY = "bee_request_delay";
	private static final String PAGE_ARCHIVE_DIR = "page_archive_dir";
	private static final String TAGGER_PATH = "tagger_path";

	private static Configuration instance;

	private boolean logging;
	private boolean semantics;
	private boolean beeMessages;
	private int refreshDelay;
	private int requestDelay;
	private String databaseDir;
	private String logFile;
	private String resourceDir;
	private String wordnetDir;
	private String taggerPath;

	/**
	 * Determines the directory for archiving of downloaded pages, if enabled by
	 * setting {@link #pageArchive} to <i>true</i>.
	 */
	private String pageArchiveDir;

	/**
	 * Boolean value indicating the use of page archiving. If page archiving is
	 * enabled, content of the downloaded pages is stored locally for future
	 * reference in a folder determined by {@link #pageArchiveDir}.
	 */
	private boolean pageArchive;

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
			instance = new Configuration(path);
		}

		return instance;
	}

	/**
	 * @return path to directory where database is stored
	 */
	public String getDatabaseDir() {

		return databaseDir;
	}

	/**
	 * @return path to the log file
	 */
	public String getLogFile() {

		return logFile;
	}

	/**
	 * @return path to the main folder where results from runs are stored
	 */
	public String getMainDir() {

		return resourceDir;
	}

	/**
	 * @return path to the directory where text of crawled articles is stored
	 *         for future reference
	 */
	public String getPageArchiveDir() {

		return pageArchiveDir;
	}

	/**
	 * 
	 * @return delay between refreshing a page, in milliseconds
	 */
	public int getRefreshDelay() {

		return refreshDelay;
	}

	/**
	 * @return path to the tagger dictionary
	 */
	public String getTaggerPath() {

		return taggerPath;
	}

	/**
	 * @return path to the directory of Wordnet dictionary
	 */
	public String getWordnetDir() {

		return wordnetDir;
	}

	/**
	 * @return boolean value indicating the verbose mode of bee agents,
	 *         <i>true</i> for enabled verbose mode, othervise <i>false</i>
	 */
	public boolean useBeeMessages() {

		return beeMessages;
	}

	/**
	 * @return boolean value indicating the use of logging, <i>true</i> for
	 *         logging enabled, otherwise <i>false</i>
	 */
	public boolean useLogging() {

		return logging;
	}

	/**
	 * @return <i>true</i> if page archiving is enabled, otherwise <i>false</i>.
	 */
	public boolean usePageArchive() {

		return pageArchive;
	}

	/**
	 * @return boolean value indicating the use of semantics when evaluating
	 *         documents, <i>true</i> for semantics enabled, otherwise
	 *         <i>false</i>
	 * @see com.github.beast.semantics.SemanticEngine SemanticEngine
	 */
	public boolean useSemantics() {

		return semantics;
	}

	public int getRequestDelay() {
		
		return requestDelay;
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
			e.printStackTrace();
			System.exit(1);
		}

		logging = Boolean.parseBoolean(properties.getProperty(LOGGING, DEFAULT_LOGGING));
		semantics = Boolean.parseBoolean(properties.getProperty(SEMANTICS, DEFAULT_SEMANTICS));
		beeMessages = Boolean.parseBoolean(properties.getProperty(BEE_MESSAGES, DEFAULT_BEE_MESSAGES));
		pageArchive = Boolean.parseBoolean(properties.getProperty(PAGE_ARCHIVE, DEFAULT_PAGE_ARCHIVE));

		refreshDelay = Integer.parseInt(properties.getProperty(REFRESH_DELAY, DEFAULT_REFRESH_DELAY));
		requestDelay = Integer.parseInt(properties.getProperty(REQUEST_DELAY, DEFAULT_REQUEST_DELAY));
		
		resourceDir = properties.getProperty(MAIN_DIR, DEFAULT_MAIN_DIR);
		logFile = properties.getProperty(LOG_FILE, DEFAULT_LOG_FILE);
		databaseDir = properties.getProperty(DATABASE_DIR, DEFAULT_DATABASE_DIR);
		wordnetDir = properties.getProperty(WORDNET_LOCATION, DEFAULT_WORDNET_LOCATION);
		pageArchiveDir = properties.getProperty(PAGE_ARCHIVE_DIR, DEFAULT_PAGE_ARCHIVE_DIR);
		taggerPath = properties.getProperty(TAGGER_PATH, DEFAULT_TAGGER_PATH);
	}
}