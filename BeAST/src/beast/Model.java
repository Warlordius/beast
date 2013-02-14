package beast;

import java.io.*;
import java.util.*;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;

import beast.crawler.Crawler;
import beast.indexNew.BeastIndex;
import beast.semantics.SemanticEngine;
import beast.tagger.Tagger;

public class Model {

	public static final boolean logging = true;
	public static final boolean useSemantics = true;
	public static final String RESOURCE_DIR = "C:\\_beast_data\\";
	public static final String DEFAULT_INIT_FILE = RESOURCE_DIR + "startPages.txt";
	public static final String DEFAULT_LOG_FILE = RESOURCE_DIR + "log.txt";
	public static final String DEFAULT_DB_FILE = RESOURCE_DIR + "graph";
	
	public static final BeastIndex index = new BeastIndex();
	public static final Crawler crawler = new Crawler(index);
	public static final Tagger tagger = new Tagger();
	public static final SemanticEngine semEngine = new SemanticEngine();
//	public static Tagger tagger;
	
	public static void main(String[] args) throws Exception {
	
	// wordnet config
	System.setProperty("wordnet.database.dir", "C:\\Program Files (x86)\\Wordnet\\2.1\\dict\\");	
		
	// neo4j config
	Config.LoggerProvider = LoggerProvider.DISABLED;
	
//	semEngine.getAllSynsets("marius2.txt");
//	semEngine.getSynonyms("");
//	System.out.println(semEngine.getRootNoun("rebels"));
		
	runHarvestToday(50, 10000);
	
//	System.out.println(semEngine.getSynonyms("rebels"));
//	System.out.println("-----");

}
	
	public static void log(String string) {
	if (logging) {
	
		try {
			FileWriter writer = new FileWriter(new File(DEFAULT_LOG_FILE),true);
			Date now = new Date();
			writer.append(now.toString() + " " + string);
			writer.append(System.getProperty("line.separator"));
			writer.close();
		} catch (Exception e) {
			System.out.println("");
		}
	}	
}

	public static void runHarvestToday(int bees, int iters) {
	
	final String experimentLocation = RESOURCE_DIR + "_experiment_data/";
	
	Calendar today = Calendar.getInstance();
	String semIndicator;
	
	if (useSemantics)
		semIndicator = "s";
	else
		semIndicator = "n";
	
	int year = today.get(Calendar.YEAR);
	int month = today.get(Calendar.MONTH) + 1;
	int day = today.get(Calendar.DAY_OF_MONTH);
	
	String folder = String.format("%04d-%02d-%02d_%d_%d_%s", year, month, day, bees, iters, semIndicator);	
	String path = experimentLocation + folder;
	
	runHarvest(bees, iters, path);
}

/** 
 	* Run a single harvest action, with set number of bees and iterations.
 * 
 * @param bees - number of bees to be used.
 * @param iters - number of iterations.
 * @param path - path of the index to be used in action. Also is the path where results are saved.
 */
	public static void runHarvest(int bees, int iters, String path) {
	
	final String filename = "_output.graphml";
	
	Date start = new Date();
	
	index.init(path);
	log("index initialized at: " + path);
	System.out.println("index initialized at: " + path);
		
	log("beginning crawl");
	crawler.init();
	crawler.doCrawl(bees, iters);
	
	//Graphics.init();
	System.out.println("All pages: " + index.numAllPages());
	System.out.println("Indexed pages: " + index.numIndexedPages());
	System.out.println("Keywords: " + index.numKeywords());
	
	index.shutdown();
	index.exportToXML(path, path + "/" + filename);
	
	Date end = new Date();
	long runtime = end.getTime() - start.getTime();
	
	int hours = (int) (runtime / 3600000);
	runtime = runtime % 3600000;
	int minutes = (int) (runtime / 60000);
	runtime = runtime % 60000;
	int seconds = (int) (runtime / 1000);
	runtime = runtime % 1000;
	int milis = (int) runtime;
	
	String runstr = String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milis);
	log("Task finished with " + bees + "bees and " + iters + "iterations in " + runstr);
	System.out.println("Task finished in " + runstr);
	
	}
}