package com.github.beast;

import java.io.*;
import java.util.*;

import net.htmlparser.jericho.Config;
import net.htmlparser.jericho.LoggerProvider;

import com.github.beast.configuration.Configuration;
import com.github.beast.crawler.Crawler;
import com.github.beast.index.BeastIndex;
import com.github.beast.semantics.SemanticEngine;
import com.github.beast.tagger.Tagger;

public class Model {

    public static final String PROPERTIES_FILE = ".properties";

    public static final BeastIndex index = new BeastIndex();
    public static final Crawler crawler = new Crawler(index);
    public static final Tagger tagger = new Tagger();
    public static final Configuration config = Configuration.getInstance(PROPERTIES_FILE);
    public static final SemanticEngine semEngine = new SemanticEngine();
    
    //public static Tagger tagger;

    public static void main(String[] args) throws Exception {

	System.setProperty("wordnet.database.dir", config.getWordnetDir());	// wordnet setup
	Config.LoggerProvider = LoggerProvider.DISABLED;			// neo4j logging setup
	
	//SemanticEngine.a;
	runHarvestToday(50, 10000);		
    }

    public static void log(String string) {

	if (config.useLogging()) {
	    try {
		FileWriter writer = new FileWriter(new File(config.getLogFile()), true);
		Date now = new Date();
		writer.append(now.toString() + " " + string);
		writer.append(System.getProperty("line.separator"));
		writer.close();
	    } catch (Exception e) {
		System.out.println(e.getMessage());
		System.out.println(e.getStackTrace());
	    }
	}
    }

    public static void runHarvestToday(int bees, int iters) {

	final String experimentLocation = config.getResourceDir() + "_experiment_data/";

	Calendar today = Calendar.getInstance();
	String semIndicator;

	if (config.useSemantics()) {
	    semIndicator = "s";
	} else {
	    semIndicator = "n";
	}

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
     * @param bees the number of bees to be used
     * @param iters the number of iterations
     * @param path the path of the index to be used in action. Is also used as
     *        the target path where results are saved.
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

	// Graphics.init();
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
	log("Task finished with " + bees + "bees and " + iters + " iterations in " + runstr);
	System.out.println("Task finished in " + runstr);

    }
}