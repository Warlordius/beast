package com.github.beast.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.github.beast.util.Configuration;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Tagger. Stanford
 * 
 * @author Štefan Sabo
 * @see 1.0
 */
public class Tagger {

	/** Tagger object used to process input text. */
	private MaxentTagger tagger;

	/**
	 * Creates a new instance of <code>Tagger</code>, using dictionary path from
	 * {@link Configuration} to create a {@link MaxentTagger} class object to
	 * process input text.
	 */
	public Tagger() {

		try {
			tagger = new MaxentTagger(Configuration.getInstance().getTaggerPath());
		} catch (ClassNotFoundException | IOException e) {
			System.err.println("Failed to initialize tagger:" + e);
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Returns a random noun from a string representing text.
	 * 
	 * @param input the text to be processed
	 * @return a random noun from the given text
	 */
	public String getRandomNoun(final String input) {

		String taggedString = tagger.tagString(input);
		//String[] tokens = taggedString.split(" ");
		ArrayList<String> nouns = new ArrayList<String>();

		for (String token : taggedString.split(" ")) {
			if (token.contains("_NN")) {
				nouns.add(token.substring(0, token.indexOf('_')));
			}
		}
		
		Random generator = new Random();
		return nouns.get(generator.nextInt(nouns.size()));
	}

	public ArrayList<String> getAllNouns(final String input) {

		String taggedString = tagger.tagString(input);
		String[] tokens = taggedString.split(" ");
		ArrayList<String> nouns = new ArrayList<String>();

		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("_NN")) {
				int pos = tokens[i].indexOf('_');
				nouns.add(tokens[i].substring(0, pos));
			}
		}
		return nouns;
	}

}