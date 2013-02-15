package beast.tagger;

import java.util.ArrayList;
import java.util.Random;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tagger {
	
	public static final String TAGGER_PATH = "tagger/english-left3words-distsim.tagger";
	
	MaxentTagger tagger;
	
	public Tagger() {
		
			try {
				tagger = new MaxentTagger(TAGGER_PATH);
			}
			catch (Exception e) {
				System.out.println("Failed to init tagger:" + e);
			}
				
	}
	public String getRandomNoun (final String input) {		
		String taggedString = tagger.tagString(input);
		String[] tokens = taggedString.split(" ");
		ArrayList<String> nouns = new ArrayList<String>();
		
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].contains("_NN")) {
				int pos = tokens[i].indexOf('_');
				nouns.add(tokens[i].substring(0, pos));
			}
		}		
		Random generator = new Random();
		return nouns.get(generator.nextInt(nouns.size()));
	}
	
	public ArrayList<String> getAllNouns (final String input) {
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