package com.github.beast.tagger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import com.github.beast.util.Configuration;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * Part of speech tagger. Utilizes Stanford Log-linear Part-Of-Speech Tagger,
 * which needs to be installed beforehand. Path to tagger dictionary is
 * determined through {@link Configuration}. Currently only the extraction of
 * nouns is supported.
 * 
 * @author Štefan Sabo
 * @version 1.0
 * @see <a href="http://nlp.stanford.edu/software/tagger.shtml">Stanford
 *      Tagger</a>
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

		ArrayList<String> nouns = getAllNouns(input);
		Random generator = new Random();
		return nouns.get(generator.nextInt(nouns.size()));
	}

	/**
	 * Extract word from a token with assigned part of speech tags, effectively
	 * removing the part of speech mark, added by {@link #tagger}.
	 * 
	 * @param token word labeled by appropriate part of speech tag (e.g.
	 *        <i>house_NN</i>)
	 * @return word without part of speech tag (e.g. <i>house</i>)
	 */
	public static String getWordFromToken(final String token) {

		char tokenSeparator = '_';
		return token.substring(0, token.indexOf(tokenSeparator));
	}

	/**
	 * Returns a list of all nouns in a string representing text.
	 * 
	 * @param input the text to be processed
	 * @return list of all nouns in the text
	 */
	public ArrayList<String> getAllNouns(final String input) {

		String nounString = "_NN";
		String taggedString = tagger.tagString(input);
		ArrayList<String> nouns = new ArrayList<String>();

		for (String token : taggedString.split(" ")) {
			if (token.contains(nounString)) {
				nouns.add(getWordFromToken(token));
			}
		}
		return nouns;
	}

}