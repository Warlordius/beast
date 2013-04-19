package com.github.beast.database;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import com.github.beast.Beast;
import com.github.beast.page.ArticlePage;
import com.github.beast.page.Link;
import com.github.beast.page.Page;
import com.github.beast.page.ReutersPage;
import com.github.beast.parser.ReutersParser;
import com.github.beast.util.Configuration;
import com.github.beast.util.Utility;

import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.impls.neo4j.Neo4jGraph;
import com.tinkerpop.blueprints.util.io.graphml.GraphMLWriter;

public class BeastIndex {

	private static final String ANCHOR_TEXT = "anchor_text";
	private static final String DEFAULT_DATE_FORMAT = "EEE MMM dd HH:mm:ss z yyyy";
	private static final String KEYWORD_NAME = "keyword";
	private static final String KEYWORD_RELEVANCE = "keyword_relevance";
	private static final String PAGE_INDEXED = "indexed";
	private static final String PAGE_KEY = "url";
	private static final String PAGE_LASTINDEX = "last_indexed";
	private static final String PAGE_LOCATION = "location";
	private static final String PAGE_PATH = "path";
	private static final String PAGE_PEREX = "perex";
	private static final String PAGE_TEXT = "text";
	private static final String PAGE_TIME = "time_milis";
	private static final String PAGE_TIMESTAMP = "timestamp";
	private static final String PAGE_TITLE = "title";
	private static final String REL_KEYWORD = "relationship_keyword";
	private static final String REL_RELEVANCE = "relationship_relevance";

	/** Graph database object. */
	private static GraphDatabase graphDb;

	/** Index of all keyword nodes. */
	private static Index<Node> keywords;

	/** Index of all nodes representing already indexed pages. */
	private static Index<Node> pageIndex;

	/** Index of all nodes representing pages. */
	private static Index<Node> allNodeIndex;

	/** Relationship type definitions. */
	protected static enum Rel implements RelationshipType {

		KEYWORD, LINK, RELATED
	}

	/**
	 * Adds a new keyword to the given {@link Page page}, with given relevance.
	 * If the page already contains the keyword, keyword relevance is updated
	 * only if value of new relevance is higher than original value. Otherwise
	 * original value is kept.
	 * 
	 * @param page the page to which the keyword is being added to
	 * @param keyword the keyword being added
	 * @param relevance the relevance of given keyword to the given page
	 */
	public void addKeyword(final Page page, final String keyword, final double relevance) {

		Node node = allNodeIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();
		Node keywordNode = getKeywordNode(keyword);
		Relationship relationship;

		if (keywordNode == null) {
			keywordNode = graphDb.createNode(KEYWORD_NAME, keyword);
			graphDb.addToIndex(keywords, keywordNode, KEYWORD_NAME, keyword);
		}

		relationship = graphDb.getSingleRelationship(node, keywordNode, Rel.KEYWORD);

		if (relationship == null) {
			relationship = graphDb.addRelationship(keywordNode,  node,  Rel.KEYWORD);
			graphDb.setProperty(relationship, KEYWORD_RELEVANCE, relevance);
		} else if (!(relationship.hasProperty(KEYWORD_RELEVANCE))
				|| (Double.parseDouble(relationship.getProperty(KEYWORD_RELEVANCE).toString()) < relevance)) {
			graphDb.setProperty(relationship, KEYWORD_RELEVANCE, relevance);
		}
	}

	// add a relation of mutual relevance for two p
	public Relationship addRelation(final Page first, final Page second, final String keyword, double relevance) {

		// TODO: create method in graphdatabase to set properties at relationship creation
		ArrayList<Relationship> relationships;
		Relationship newRelationship;

		Node firstNode = pageIndex.get(PAGE_KEY, first.getUrl().toString()).getSingle();
		Node secondNode = pageIndex.get(PAGE_KEY, second.getUrl().toString()).getSingle();

		relationships = graphDb.getRelationship(firstNode, secondNode, Rel.RELATED);
		
		for (Relationship rel : relationships) {
			if (rel.hasProperty(REL_KEYWORD)) {
				if ((Double.parseDouble(rel.getProperty(REL_RELEVANCE).toString()) < relevance)) {
					graphDb.setProperty(rel, REL_RELEVANCE, relevance);
				}
				return rel;
			}
		}		

		newRelationship = graphDb.addRelationship(firstNode, secondNode, Rel.RELATED);
		graphDb.setProperty(newRelationship, REL_KEYWORD, keyword);
		graphDb.setProperty(newRelationship, REL_RELEVANCE, relevance);
		return newRelationship;		
	}

	// get a random keyword of a given page
	public double getKeywordRelevance(String keyword, Page page) {

		Node node = pageIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();

		if (node == null)
			return 0;

		Iterator<Relationship> iter = node.getRelationships(Rel.KEYWORD).iterator();

		while (iter.hasNext()) {

			Relationship rel = iter.next();

			if (rel.getOtherNode(node).hasProperty(KEYWORD_NAME)
					&& (rel.getOtherNode(node).getProperty(KEYWORD_NAME).toString().equals(keyword))) {
				double relevance = Double.parseDouble(rel.getProperty(KEYWORD_RELEVANCE).toString());
				return relevance;
			}
		}

		return 0;
	}

	// get a random keyword of a given page
	public String getRandKeyword(Page page) {

		Node node = pageIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();

		ArrayList<String> keywords = Beast.tagger.getAllNouns(page.getTitle());

		Iterator<Relationship> iter = node.getRelationships(Rel.KEYWORD).iterator();

		while (iter.hasNext()) {
			Relationship rel = iter.next();
			if (rel.getOtherNode(node).hasProperty(KEYWORD_NAME)) {
				keywords.add(rel.getOtherNode(node).getProperty(KEYWORD_NAME).toString());
			}
		}
		Random generator = new Random();
		String keyword = keywords.get(generator.nextInt(keywords.size()));

		if (Configuration.getInstance().useSemantics()) {
			return Beast.semEngine.getRootNoun(keyword);
		} else {
			return keyword;
		}
	}

	// retrieve a random neighbour of a page
	public Page getRandNeighbour(final Page page) {

		Node node;
		Node otherNode;
		Random generator = new Random();
		ArrayList<Relationship> relArray = new ArrayList<Relationship>();

		if (!containsUrl(page.getUrl(), allNodeIndex)) {
			return null;
		}

		node = allNodeIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();
		// iter = node.getRelationships(Rel.LINK).iterator();

		for (Relationship rel : node.getRelationships(Rel.LINK)) {
			relArray.add(rel);
		}

		if (relArray.size() == 0) {
			return null;
		} else {
			otherNode = relArray.get(generator.nextInt(relArray.size())).getOtherNode(node);
			return pageFromNode(otherNode);
		}
	}

	// retrieve a random page from index
	public Page getRandPage() {

		int pageNum = getNumberOfPages(pageIndex);
		Random generator = new Random();
		int choice = generator.nextInt(pageNum);
		IndexHits<Node> result = pageIndex.query(PAGE_KEY, "*");

		for (int i = 0; i < choice; i++) {
			result.next();
		}

		Node node = result.next();
		result.close();
		Page page = pageFromNode(node);
		return page;
	}

	/**
	 * Indexes and existing page, adding it into database, along with adding all
	 * linked pages as not indexed nodes, for reference.
	 * 
	 * @param page - page to be indexed.
	 * @return Indexed page as a Node in graph database.
	 */
	public Node indexPage(final ArticlePage page) {

		Node newPage = null;
		boolean nodeIsNew = true;

		// find out if node already exists
		// if we found a page and its already indexed, we return the page
		IndexHits<Node> resultIndexed = pageIndex.get(PAGE_KEY, page.getUrl().toString());
		if (resultIndexed.size() > 0) {
			newPage = resultIndexed.next();
			resultIndexed.close();

			return newPage;
		}

		// otherwise if we found a node that's not yet fully indexed, we update
		// the page we have found
		IndexHits<Node> result = allNodeIndex.get(PAGE_KEY, page.getUrl().toString());
		if (result.size() > 0) {
			newPage = result.next();
			result.close();
			nodeIsNew = false;
		}

		if (nodeIsNew) {
			newPage = graphDb.createNode();
		}

		// try to process page, if not yet processed
		try {
			page.process();
		} catch (NullPointerException e) {
			System.err.println("Failed to process page: " + page.getUrl());
			return null;
		}

		System.out.println("index - " + page.getUrl());
		page.setLastIndexed(new Date());
		graphDb.setProperty(newPage, PAGE_KEY, page.getUrl().toString());
		graphDb.setProperty(newPage, PAGE_INDEXED, true);

		if (page.getArchiveFile() != null)
			graphDb.setProperty(newPage, PAGE_PATH, page.getArchiveFile().getAbsolutePath());
		if (page.getTitle() != null)
			graphDb.setProperty(newPage, PAGE_TITLE, page.getTitle());
		if (page.getLocation() != null)
			graphDb.setProperty(newPage, PAGE_LOCATION, page.getLocation());
		if (page.getTimestamp() != null)
			graphDb.setProperty(newPage, PAGE_TIMESTAMP, page.getTimestamp().toString());
		if (page.getLastIndexed() != null)
			graphDb.setProperty(newPage, PAGE_LASTINDEX, page.getLastIndexed().toString());
		if (page.getText() != null)
			graphDb.setProperty(newPage, PAGE_TEXT, page.getText().toString());
		if (page.getPerex() != null)
			graphDb.setProperty(newPage, PAGE_PEREX, page.getPerex().toString());

		if (page.getTimestamp() != null) {
			double timemilis = page.getTimestamp().getTime();
			graphDb.setProperty(newPage, PAGE_TIME, timemilis);
		}

		// add outgoing links within the page as nonindexed nodes
		for (int i = 0; i < page.getLinks().size(); i++) {

			Node linkedPage;

			if (containsUrl(page.getLinks().get(i).getUrl(), allNodeIndex)) {
				linkedPage = allNodeIndex.get(PAGE_KEY, page.getLinks().get(i).getUrl()).getSingle();
			} else {
				linkedPage = graphDb.createNode();
				graphDb.setProperty(linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl().toString());
				graphDb.setProperty(linkedPage, PAGE_INDEXED, false);
				graphDb.addToIndex(allNodeIndex, linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl());
			}
			Relationship relation = graphDb.addRelationship(newPage,  linkedPage,  Rel.LINK);
			graphDb.setProperty(relation, ANCHOR_TEXT, page.getLinks().get(i).getAnchorText());
		}

		if ((nodeIsNew) && (!containsUrl(page.getUrl(), allNodeIndex)))
			graphDb.addToIndex(allNodeIndex, newPage, PAGE_KEY, page.getUrl().toString());

		graphDb.addToIndex(pageIndex, newPage, PAGE_KEY, page.getUrl().toString());

		return newPage;
	}

	/**
	 * Initializes the graph database, using the default path
	 */
	public void init() {

		init(Configuration.getInstance().getDatabaseDir());
	}

	/**
	 * Initializes the graph database, using the set path, creating indices for
	 * all indexed pages, all nodes overall (necessary for keeping track of
	 * linked pages that have not been indexed yet) and index for keywords.
	 * 
	 * Also contains optional keyword listing and testing code during
	 * development (to be removed later).
	 */
	public void init(final String path) {

		graphDb = new GraphDatabase(path);
		pageIndex = graphDb.createIndex("pages");
		allNodeIndex = graphDb.createIndex("allnodes");
		keywords = graphDb.createIndex("keywords");
		indexStartingPages();

		System.out.println("index running");
	}

	// return the number of indexed pages including linked ones
	public int numAllPages() {

		return getNumberOfPages(allNodeIndex);
	}

	// return the number of indexed and processed pages
	public int numIndexedPages() {

		return getNumberOfPages(pageIndex);
	}

	// return the number of indexed keywords
	public int numKeywords() {

		return getNumberOfPages(keywords);
	}

	// retrieve a page object from a given node
	public ArticlePage pageFromNode(final Node node) {

		URL url;
		ArticlePage page = null;

		try {
			url = Utility.stringToURL(node.getProperty(PAGE_KEY).toString());
			page = new ReutersPage(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (node.hasProperty(PAGE_INDEXED)) {
			if (node.getProperty(PAGE_INDEXED).toString() == "true") {
				page.setIndexed(true);
			} else {
				page.setIndexed(false);
			}
		}
		if (node.hasProperty(PAGE_PATH)) {
			File newFile = new File(node.getProperty(PAGE_PATH).toString());
			page.setArchiveFile(newFile);
		}
		if (node.hasProperty(PAGE_TITLE)) {
			page.setTitle(node.getProperty(PAGE_TITLE).toString());
		}
		if (node.hasProperty(PAGE_LOCATION)) {
			page.setLocation(node.getProperty(PAGE_LOCATION).toString());
		}
		if (node.hasProperty(PAGE_TEXT)) {
			page.setText(new StringBuffer(node.getProperty(PAGE_TEXT).toString()));
		}
		if (node.hasProperty(PAGE_PEREX)) {
			page.setPerex(new StringBuffer(node.getProperty(PAGE_PEREX).toString()));
		}
		if (node.hasProperty(PAGE_LASTINDEX)) {
			try {
				DateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH);
				page.setLastIndexed(format.parse(node.getProperty(PAGE_LASTINDEX).toString()));
			} catch (ParseException e) {
				System.out.println("Wrong date format: " + e);
			}
		}
		if (node.hasProperty(PAGE_TIMESTAMP)) {
			try {
				DateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH);
				page.setTimestamp(format.parse(node.getProperty(PAGE_TIMESTAMP).toString()));
			} catch (ParseException e) {
				System.out.println("Wrong date format: " + e);
			}
		}
		return page;
	}

	// retrieve a page object from a given url
	public Page pageFromUrl(String url) throws MalformedURLException {

		Node node = pageIndex.get(PAGE_KEY, url).getSingle();
		return pageFromNode(node);
	}

	/**
	 * Reindexed an existing page, that has already been indexed, along with
	 * adding all linked pages as not indexed nodes, for reference. If the given
	 * page has not yet been indexed, the page is indexed for the first time,
	 * otherwise the page is reprocessed and indexed anew, possibly with new set
	 * of links. Ingoing links are not affected.
	 * 
	 * @param page - Page to be reindexed.
	 * @return Indexed page as a Node in graph database.
	 */
	public Node reindexPage(final ArticlePage page) {

		Node newPage = null;

		// if page is not yet indexed properly, index it normally
		IndexHits<Node> resultIndexed = pageIndex.get(PAGE_KEY, page.getUrl().toString());
		if (resultIndexed.size() == 0) {
			resultIndexed.close();
			newPage = indexPage(page);
			return newPage;
		} else { 							// otherwise use it for reindexing
			newPage = resultIndexed.next();
			resultIndexed.close();
		}

		System.out.println("update - " + page.getUrl());
		page.setLastIndexed(new Date());
		graphDb.setProperty(newPage, PAGE_KEY, page.getUrl().toString());
		graphDb.setProperty(newPage, PAGE_INDEXED, true);

		// try to process page, if not yet processed
		try {
			page.process(Page.REPROCESS);
		} catch (NullPointerException e) {
			System.err.println("Failed to process page: " + page.getUrl());
			return null;
		}

		if (page.getText() != null) {
			graphDb.setProperty(newPage, PAGE_TEXT, page.getText().toString());
		}
		if (page.getPerex() != null) {
			graphDb.setProperty(newPage, PAGE_PEREX, page.getPerex().toString());
		}
		if (page.getArchiveFile() != null) {
			graphDb.setProperty(newPage, PAGE_PATH, page.getArchiveFile().getAbsolutePath());
		}
		if (page.getTitle() != null) {
			graphDb.setProperty(newPage, PAGE_TITLE, page.getTitle());
		}
		if (page.getLocation() != null) {
			graphDb.setProperty(newPage, PAGE_LOCATION, page.getLocation());
			}
		if (page.getTimestamp() != null) {
			graphDb.setProperty(newPage, PAGE_TIMESTAMP, page.getTimestamp().toString());
		}
		if (page.getLastIndexed() != null) {
			graphDb.setProperty(newPage, PAGE_LASTINDEX, page.getLastIndexed().toString());
		}

		// remove all old outgoing links first
		Iterator<Relationship> rel = newPage.getRelationships(Rel.LINK).iterator();

		while (rel.hasNext()) {
			Relationship link = rel.next();
			if (link.getStartNode().equals(newPage)) {
				graphDb.deleteRelationship(link);
			}
		}

		// update links
		for (int i = 0; i < page.getLinks().size(); i++) {

			Node linkedPage;
			
			if (containsUrl(page.getLinks().get(i).getUrl(), allNodeIndex)) {
				linkedPage = allNodeIndex.get(PAGE_KEY, page.getLinks().get(i).getUrl()).getSingle();
			} else {
				linkedPage = graphDb.createNode();
				graphDb.setProperty(linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl().toString());
				graphDb.setProperty(linkedPage, PAGE_INDEXED, false);
			}

			Relationship relation = graphDb.addRelationship(newPage,  linkedPage,  Rel.LINK);
			graphDb.setProperty(relation, ANCHOR_TEXT, page.getLinks().get(i).getAnchorText());
			graphDb.addToIndex(allNodeIndex, linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl());
		}
		return newPage;
	}

	// check, if the page already exists in the index
	private boolean containsUrl(final URL url, Index<Node> index) {

		IndexHits<Node> result = index.get(PAGE_KEY, url.toString());

		if (result.size() > 0) {
			result.close();
			return true;
		} else {
			result.close();
			return false;
		}
	}

	// retrieve a keyword node
	private Node getKeywordNode(String keyword) {

		Node node = keywords.get(KEYWORD_NAME, keyword).getSingle();
		return node;
	}

	/**
	 * Indexes the starting pages, currently preset for Reuters home page and
	 * using reuters page parser.
	 */
	private void indexStartingPages() {

		ReutersPage newPage;
		List<Link> links = ReutersParser.getInstance().getStartingPages();

		for (Link link : links) {
			newPage = new ReutersPage(link.getUrl());
			if (!containsUrl(newPage.getUrl(), allNodeIndex)) {
				try {
					newPage.process();
				} catch (NullPointerException e) {
					System.err.println("Failed to process page: " + newPage.getUrl());
					continue;
				}
				indexPage(newPage);
			}
		}
	}

	// return the number of pages in a given index
	private int getNumberOfPages(Index<Node> index) {

		int numPages;
		IndexHits<Node> result;
		if (index.getName().equals("keywords")) {
			result = index.query(KEYWORD_NAME, "*");
		} else {
			result = index.query(PAGE_KEY, "*");
		}
		numPages = result.size();
		result.close();
		return numPages;
	}	
	
	/**
	 * Exports Neo4j graph representation of this index into an XML file.
	 * 
	 * @param outputPath - path to file, where Neo4j graph will be exported as
	 *        XML.
	 */
	public void exportToXML(final String graphPath, final String outputPath) {

		Graph graph = new Neo4jGraph(graphPath);

		try {
			FileOutputStream fs = new FileOutputStream(outputPath);
			GraphMLWriter.outputGraph(graph, fs);
			System.out.println("Graph exported into file : \"" + outputPath + "\"");
			fs.close();

		} catch (IOException e) {
			System.out.println("IO Error: " + e);
			return;
		}
	}
	
	public void shutdown() {
		
		graphDb.shutdown();
	}
}