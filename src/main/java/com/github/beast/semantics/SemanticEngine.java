package com.github.beast.semantics;

import java.util.ArrayList;

import com.github.beast.util.Configuration;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

/**
 * Class representing engine, handling semantic queries to <a
 * href="http://wordnet.princeton.edu/wordnet/">Wordnet</a> database. Currently
 * the root of a given noun may be established, through
 * {@link #getNounRoot(String)}, or all synonyms of a noun may be retrieved
 * through {@link #getSynonyms(String)}.
 * 
 * @author Štefan Sabo
 * @version 1.0
 * @see <a href="http://wordnet.princeton.edu/wordnet/">Wordnet</a>
 */
public class SemanticEngine {

	/** Wordnet database object. */
	private static WordNetDatabase database;

	/**
	 * Creates a new Wordnet database and sets system property
	 * <code>wordnet.database.dir</code>, pointing to Wordnet database in the
	 * file system, according to settings given in {@link Configuration} class.
	 */
	public SemanticEngine() {

		System.setProperty("wordnet.database.dir", Configuration.getInstance().getWordnetDir());
		database = WordNetDatabase.getFileInstance();
	}

	/**
	 * Retrieves a root form of a noun from the Wordnet database.
	 * 
	 * @param input arbitrary form of a noun
	 * @return root form of the given noun, retrieved from Wordnet database. If
	 *         no root form is retrieved for a given word, the word itself is
	 *         returned.
	 */
	public String getRootNoun(final String input) {

		String[] candidates = database.getBaseFormCandidates(input, SynsetType.NOUN);

		if (candidates.length == 0) {
			return input;
		} else {
			return candidates[0];
		}
	}

	/**
	 * Retrieves all synonyms of a given noun from the Wordnet database.
	 * 
	 * @param input arbitrary noun
	 * @return list of synonyms of the noun
	 */
	public ArrayList<String> getSynonyms(final String input) {

		NounSynset nounSynset;
		ArrayList<String> synonyms = new ArrayList<String>();
		Synset[] synsets = database.getSynsets(input, SynsetType.NOUN);
		String[] tokens;

		if (synsets.length == 0) {
			synonyms.add(input);
			return synonyms;
		}

		for (Synset synset : synsets) {
			nounSynset = (NounSynset) synset;
			tokens = nounSynset.getWordForms();

			for (String token : tokens) {
				if (!synonyms.contains(token)) {
					synonyms.add(token);
				}
			}
		}
		return synonyms;
	}
}