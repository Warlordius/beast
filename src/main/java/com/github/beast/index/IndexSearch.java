package com.github.beast.index;

import java.io.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import com.github.beast.Model;

public class IndexSearch{

	public static final String INDEX_DIRECTORY = Model.RESOURCE_DIR + "index";
	public static final String FILE_DIRECTORY = Model.RESOURCE_DIR + "pages";
	public static final Version VERSION = Version.LUCENE_31;
	
	public static final String FIELD_PATH = "path";
	public static final String FIELD_CONTENTS = "contents";
	
	// TODO remove path from parameters and make return value void
	public static double indexSearch(String queryText, String path) throws Exception{
		
		String index = INDEX_DIRECTORY;
		String field = "contents";
		
		if (queryText == null) {
			System.out.println("No query term");
			return 0;
		}
		
		IndexReader reader = IndexReader.open(FSDirectory.open(new File(index)));
		IndexSearcher searcher = new IndexSearcher(reader);
		Analyzer analyzer = new StandardAnalyzer(VERSION);
		
		QueryParser parser = new QueryParser(VERSION, field, analyzer);
		Query query = parser.parse(queryText);
		//System.out.println("Searching for: " + query.toString(field));
		
		searcher.search(query, null, 100);
		//System.out.println("Time: "+(end.getTime()-start.getTime())+"ms");
		
		int hitsPerPage = 100;
		boolean raw = false;
		
		// TODO: evil code
		double bonus = doPagingSearch(searcher, query, hitsPerPage, raw, path);
		
		searcher.close();
		reader.close();
		return bonus;
	}
	// TODO: remove hits export, path from parameters
	public static double doPagingSearch(IndexSearcher searcher, Query query, int hitsPerPage, boolean raw, String pathe) throws IOException{
		
		TopDocs results = searcher.search(query, 5 * hitsPerPage);
		ScoreDoc[] hits = results.scoreDocs;
				
		int numTotalHits = results.totalHits;
		//System.out.println(numTotalHits + " total matching documents");
		
		int start = 0;
		int end = numTotalHits;
		
		for (int i = start; i < end; i++){
			if (raw) {
				System.out.println("doc="+hits[i].doc+" score="+hits[i].score);
				continue;
			}
			
			
			
			Document doc = searcher.doc(hits[i].doc);
			String path = doc.get("path");
			
			// TODO: evil code
			if (path.equals(pathe)) {
				return hits[i].score;
			}
			
			//System.out.println(path);
			//System.out.println("Title:" + doc.get("title"));
		}
		
		return 0;
	}
}