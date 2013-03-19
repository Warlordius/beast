package com.github.beast.semantics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class SemanticEngine {

	private static WordNetDatabase database;

	public SemanticEngine() {

		database = WordNetDatabase.getFileInstance();
	}

	public String getRootNoun(String token) {

		String[] candidates = database.getBaseFormCandidates(token, SynsetType.NOUN);

		if (candidates.length == 0)
			return token;
		else
			return candidates[0];
	}

	public void getAllSynsets(String path) throws Exception {

		FileInputStream is = new FileInputStream(path);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));

		FileWriter fw = new FileWriter("output.txt");
		BufferedWriter bw = new BufferedWriter(fw);

		String strline;

		// NOUNS
		while ((strline = br.readLine()) != null) {

			int index = strline.indexOf(' ');
			strline = strline.substring(0, index);

			Synset[] synsets = database.getSynsets(strline, SynsetType.VERB);

			if (synsets.length > 0) {
				String line = strline + ";";

				for (int i = 0; i < synsets.length; i++) {
					String words[] = synsets[i].getWordForms();

					for (int j = 0; j < words.length; j++) {
						line = line + words[j];

						if (j != (words.length - 1)) {
							line = line + ",";
						}
					}

					line = line + ";";
				}

				System.out.println(line);
				bw.write(line + '\n');
			}
		}

		br.close();
		bw.close();
	}

	public ArrayList<String> getSynonyms(String token) {

		// get all synsets from database
		Synset[] synsets = database.getSynsets(token, SynsetType.NOUN);
		ArrayList<String> words = new ArrayList<String>();

		// add the given token only if not found - if found, other
		// form may be used
		if (synsets.length == 0) {
			words.add(token);
			return words;
		}

		// extract tokens from all synsets at once
		for (int i = 0; i < synsets.length; i++) {

			NounSynset nSyns = (NounSynset) synsets[i];

			String[] synWords = nSyns.getWordForms();

			// append words together
			for (int j = 0; j < synWords.length; j++) {

				if (!words.contains(synWords[j]))
					words.add(synWords[j]);
			}
		}

		return words;
	}
}