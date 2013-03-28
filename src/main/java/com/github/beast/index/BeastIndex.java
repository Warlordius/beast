package com.github.beast.index;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexHits;

import com.github.beast.Beast;
import com.github.beast.page.Page;
import com.github.beast.page.PageReuters;
import com.github.beast.parser.ParserReuters;
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

	private static Index<Node> keywords;
	private static Index<Node> pageIndex;
	private static GraphDatabaseService graphDb;
	private static Index<Node> allNodeIndex;

	private static enum Rel implements RelationshipType { // relationship
		// definitions
		KEYWORD, LINK, RELATED
	}

	// adds a hook to ensure correct shutdown of database in case of unexpected
	// exit
	private static void registerShutdownHook(final GraphDatabaseService graphDb) {

		Runtime.getRuntime().addShutdownHook(new Thread() {
			@Override
			public void run() {

				graphDb.shutdown();
			}
		});
	}

	// public LinkedList<Page> pages;

	// add a keyword to a given page
	public void addKeyword(Page page, String keyword, double quality) {

		Transaction tx = graphDb.beginTx();
		try {
			Node node = allNodeIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();
			Node keywordNode = getKeywordNode(keyword);

			// if keyword is new, create it
			if (keywordNode == null) {
				keywordNode = graphDb.createNode();
				keywordNode.setProperty(KEYWORD_NAME, keyword);
				keywords.add(keywordNode, KEYWORD_NAME, keyword);
			}

			// find out, if there already is a relationship between page and
			// keyword
			Iterator<Relationship> iter = node.getRelationships(Rel.KEYWORD).iterator();
			Relationship rel = null;
			while ((iter.hasNext()) && (rel == null)) {
				Relationship tmp = iter.next();
				if (tmp.getOtherNode(node).getProperty(KEYWORD_NAME).toString().equals(keyword)) {
					rel = tmp;
				}
			}
			// if not, create new relationship
			if (rel == null) {
				rel = keywordNode.createRelationshipTo(node, Rel.KEYWORD);
				rel.setProperty(KEYWORD_RELEVANCE, quality);
			}
			// update old relationship if the new one has higher quality
			else {
				if ((rel.hasProperty(KEYWORD_RELEVANCE)) && (Double.parseDouble(rel.getProperty(KEYWORD_RELEVANCE).toString()) < quality)) {
					rel.setProperty(KEYWORD_RELEVANCE, quality);
				}
			}
			tx.success();
		} finally {
			tx.finish();
		}
	}

	// add a relation of mutual relevance for two p
	public Relationship addRelation(final Page first, final Page second, final String keyword, double relevance) {

		Relationship newRel;

		Transaction tx = graphDb.beginTx();
		try {
			Node firstNode = pageIndex.get(PAGE_KEY, first.getUrl().toString()).getSingle();
			Node secondNode = pageIndex.get(PAGE_KEY, second.getUrl().toString()).getSingle();

			Iterator<Relationship> iter = firstNode.getRelationships(Rel.RELATED).iterator();

			while (iter.hasNext()) {
				Relationship rel = iter.next();
				// check if nodes have a common relation
				if (rel.getOtherNode(firstNode).equals(secondNode)) {
					// check if nodes have a common relation concerning given
					// keyword
					if ((rel.hasProperty(REL_KEYWORD)) && (rel.getProperty(REL_KEYWORD).equals(keyword))) {
						// check if the relation has lower relevance than given
						// relevance, if so, update relevance, otherwise end
						// (relation is already established, return it)
						if ((rel.hasProperty(REL_RELEVANCE)) && (Double.parseDouble(rel.getProperty(REL_RELEVANCE).toString()) < relevance)) {
							rel.setProperty(REL_RELEVANCE, relevance);
						}
						tx.success();
						return rel;
					}
				}
			}

			// relation was not found
			newRel = firstNode.createRelationshipTo(secondNode, Rel.RELATED);
			newRel.setProperty(REL_KEYWORD, keyword);
			newRel.setProperty(REL_RELEVANCE, relevance);
			tx.success();
			return newRel;
		} finally {
			tx.finish();
		}

	}

	/**
	 * Exports Neo4j graph representation of this index into an XML file.
	 * 
	 * @param outputPath - path to file, where Neo4j graph will be exported as
	 *        XML.
	 */
	public void exportToXML(String graphPath, String outputPath) {

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

	// get a random keyword of a given page
	public double getKeywordRelevance(String keyword, Page page) {

		Node node = pageIndex.get(PAGE_KEY, page.getUrl().toString()).getSingle();

		if (node == null)
			return 0;

		Iterator<Relationship> iter = node.getRelationships(Rel.KEYWORD).iterator();

		while (iter.hasNext()) {

			Relationship rel = iter.next();

			if (rel.getOtherNode(node).hasProperty(KEYWORD_NAME) && (rel.getOtherNode(node).getProperty(KEYWORD_NAME).toString().equals(keyword))) {
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

		if (Beast.config.useSemantics()) {
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

		int pageNum = numPages(pageIndex);
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
	public Node indexPage(final Page page) {

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

		Transaction tx = graphDb.beginTx();
		try {
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
			newPage.setProperty(PAGE_KEY, page.getUrl().toString());
			newPage.setProperty(PAGE_INDEXED, true);

			if (page.getArchiveFile() != null)
				newPage.setProperty(PAGE_PATH, page.getArchiveFile().getAbsolutePath());
			if (page.getTitle() != null)
				newPage.setProperty(PAGE_TITLE, page.getTitle());
			if (page.getLocation() != null)
				newPage.setProperty(PAGE_LOCATION, page.getLocation());
			if (page.getTimestamp() != null)
				newPage.setProperty(PAGE_TIMESTAMP, page.getTimestamp().toString());
			if (page.getLastIndexed() != null)
				newPage.setProperty(PAGE_LASTINDEX, page.getLastIndexed().toString());
			if (page.getText() != null)
				newPage.setProperty(PAGE_TEXT, page.getText().toString());
			if (page.getPerex() != null)
				newPage.setProperty(PAGE_PEREX, page.getPerex().toString());

			// EXPERIMENTAL
			if (page.getTimestamp() != null) {
				double timemilis = page.getTimestamp().getTime();
				newPage.setProperty(PAGE_TIME, timemilis);
			}

			// add outgoing links within the page as nonindexed nodes
			for (int i = 0; i < page.getLinks().size(); i++) {

				Node linkedPage;

				if (containsUrl(page.getLinks().get(i).getUrl(), allNodeIndex)) {
					linkedPage = allNodeIndex.get(PAGE_KEY, page.getLinks().get(i).getUrl()).getSingle();
				} else {
					linkedPage = graphDb.createNode();
					linkedPage.setProperty(PAGE_KEY, page.getLinks().get(i).getUrl().toString());
					linkedPage.setProperty(PAGE_INDEXED, false);
					allNodeIndex.add(linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl());
				}
				Relationship relation = newPage.createRelationshipTo(linkedPage, Rel.LINK);
				relation.setProperty(ANCHOR_TEXT, page.getLinks().get(i).getAnchorText());
			}

			if ((nodeIsNew) && (!containsUrl(page.getUrl(), allNodeIndex)))
				allNodeIndex.add(newPage, PAGE_KEY, page.getUrl().toString());

			pageIndex.add(newPage, PAGE_KEY, page.getUrl().toString());

			tx.success();

			return newPage;
		} finally {
			tx.finish();
		}
	}

	/**
	 * Initializes the graph database, using the default path
	 */
	public void init() {

		init(Beast.config.getDatabaseDir());
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

		graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(path);
		pageIndex = graphDb.index().forNodes("pages");
		allNodeIndex = graphDb.index().forNodes("allnodes");
		keywords = graphDb.index().forNodes("keywords");
		registerShutdownHook(graphDb);
		indexStartingPages();
				
		System.out.println("index running");
		// listKeywords(keywords);
		// listNodes(pageIndex, "output.txt");

		Transaction tx = graphDb.beginTx();
		try {
			tx.success(); 
		} finally {
			tx.finish();
		}
	}

	/**
	 * Lists all indexed nodes, representing currently indexed pages. Along with
	 * the nodes, link structure and identified keywords are also displayed for
	 * every indexed nodes.
	 * 
	 * @param nodes - index of nodes to be displayed
	 */
	public void listNodes(final Index<Node> nodes) {

		int pageCount = 0;

		System.out.println("<<<< NODES >>>>");

		// get nodes from the index
		IndexHits<Node> result = nodes.query(PAGE_KEY, "*");

		// repeat for every result (node) from the given index
		while (result.iterator().hasNext()) {

			pageCount++;

			Node oneResult = result.iterator().next();
			System.out.println("Page " + pageCount + "\n");
			System.out.println("URL: " + oneResult.getProperty(PAGE_KEY));
			System.out.println("title: " + oneResult.getProperty(PAGE_TITLE));

			System.out.println();
			System.out.println("Links: ");

			Iterator<Relationship> rel = oneResult.getRelationships().iterator();
			int linkCount = 0;

			while (rel.hasNext()) {

				Relationship link = rel.next();
				if (link.isType(Rel.LINK)) {
					linkCount++;
					Node other = link.getOtherNode(oneResult);

					String otherKey;
					String otherTitle;

					if (other.hasProperty(PAGE_KEY))
						otherKey = other.getProperty(PAGE_KEY).toString();
					else
						otherKey = " - no url -";

					if (other.hasProperty(PAGE_TITLE))
						otherTitle = ", " + other.getProperty(PAGE_TITLE).toString();
					else
						otherTitle = ", << no title >>";

					System.out.println("     " + otherKey + otherTitle);
				}
			}

			System.out.println();
			System.out.println("Total links: " + linkCount);

			System.out.println("--------------------------");
		}
	}

	/**
	 * Lists all indexed nodes, representing currently indexed pages. Along with
	 * the nodes, link structure and identified keywords are also displayed for
	 * every indexed nodes.
	 * 
	 * @param nodes - index of nodes to be listed.
	 * @param path - string containing path to a file, where the output should
	 *        be redirected.
	 */
	public void listNodes(Index<Node> nodes, String path) {

		// if path is set, redirect output to given file
		if (!path.isEmpty()) {

			PrintStream output = System.out;

			try {
				PrintStream ps = new PrintStream(new FileOutputStream(path));
				System.setOut(ps);
				listNodes(nodes);
				ps.close();

			} catch (FileNotFoundException ex) {
				System.out.println("File not found: " + path);
				return;

			} finally {
				// redirect output back to original system stream
				System.setOut(output);
			}
		}
	}

	public void listNodes(String path) {

		listNodes(pageIndex, path);
	}

	// return the number of indexed pages including linked ones
	public int numAllPages() {

		return numPages(allNodeIndex);
	}

	// return the number of indexed and processed pages
	public int numIndexedPages() {

		return numPages(pageIndex);
	}

	// return the number of indexed keywords
	public int numKeywords() {

		return numPages(keywords);
	}

	// retrieve a page object from a given node
	public Page pageFromNode(final Node node) {

		URL url;
		Page page = null;
		
		try {
			url = Utility.stringToURL(node.getProperty(PAGE_KEY).toString());
			page = new PageReuters(url);
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
			page.setFile(newFile);
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
	public Node reindexPage(final Page page) {

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

		Transaction tx = graphDb.beginTx();

		try {
			System.out.println("update - " + page.getUrl());
			page.setLastIndexed(new Date());
			newPage.setProperty(PAGE_KEY, page.getUrl().toString());
			newPage.setProperty(PAGE_INDEXED, true);

			// try to process page, if not yet processed
			try {
				page.process(Page.REPROCESS);
			} catch (NullPointerException e) {
				System.err.println("Failed to process page: " + page.getUrl());
				return null;
			}

			if (page.getText() != null) {
				newPage.setProperty(PAGE_TEXT, page.getText().toString());
			}
			if (page.getPerex() != null) {
				newPage.setProperty(PAGE_PEREX, page.getPerex().toString());
			}
			if (page.getArchiveFile() != null) {
				newPage.setProperty(PAGE_PATH, page.getArchiveFile().getAbsolutePath());
			}
			if (page.getTitle() != null) {
				newPage.setProperty(PAGE_TITLE, page.getTitle());
			}
			if (page.getLocation() != null) {
				newPage.setProperty(PAGE_LOCATION, page.getLocation());
			}
			if (page.getTimestamp() != null) {
				newPage.setProperty(PAGE_TIMESTAMP, page.getTimestamp().toString());
			}
			if (page.getLastIndexed() != null) {
				newPage.setProperty(PAGE_LASTINDEX, page.getLastIndexed().toString());
			}

			// remove all old outgoing links first
			Iterator<Relationship> rel = newPage.getRelationships(Rel.LINK).iterator();

			while (rel.hasNext()) {

				Relationship link = rel.next();

				if (link.getStartNode().equals(newPage)) {
					link.delete();
				}
			}

			// update links
			for (int i = 0; i < page.getLinks().size(); i++) {

				Node linkedPage;
				if (containsUrl(page.getLinks().get(i).getUrl(), allNodeIndex)) {
					linkedPage = allNodeIndex.get(PAGE_KEY, page.getLinks().get(i).getUrl()).getSingle();
				} else {
					linkedPage = graphDb.createNode();
					linkedPage.setProperty(PAGE_KEY, page.getLinks().get(i).getUrl().toString());
					linkedPage.setProperty(PAGE_INDEXED, false);
				}

				Relationship relation = newPage.createRelationshipTo(linkedPage, Rel.LINK);
				relation.setProperty(ANCHOR_TEXT, page.getLinks().get(i).getAnchorText());
				allNodeIndex.add(linkedPage, PAGE_KEY, page.getLinks().get(i).getUrl());
			}

			tx.success();
			return newPage;
		} finally {
			tx.finish();
		}
	}

	// shutdown the running index database
	public void shutdown() {

		Beast.log("shutting down");
		graphDb.shutdown();
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

		PageReuters newPage;
		ArrayList<String> links = ParserReuters.getStartingPages();
		
		for (String link : links) {	
			try {
				newPage = new PageReuters(Utility.stringToURL(link));
    			if (!containsUrl(newPage.getUrl(), allNodeIndex)) {
    				newPage.process();
    				indexPage(newPage);
    			}    			
			} catch (MalformedURLException e) {
				System.err.println("Malformed URL: " + link);
			}			
		}			
	}

	/**
	 * 
	 * Lists current indexed keywords from the given index of keywords.
	 * 
	 * @param keywords - index of keywords to be displayed.
	 */
	@SuppressWarnings("unused")
	private void listKeywords(Index<Node> keywords) {

		System.out.println("<<<< KEYWORDS >>>>");

		// get keywords from the index
		IndexHits<Node> result = keywords.query(KEYWORD_NAME, "*");

		// repeats for every result (keyword) from given index
		while (result.iterator().hasNext()) {

			int relCount = 0;
			Node oneResult = result.iterator().next();
			Date mostRecent = new Date(1000);
			Iterator<Relationship> rel = oneResult.getRelationships().iterator();

			// counts occurrences of the keyword
			while (rel.hasNext()) {

				relCount++;
				Node otherNode = rel.next().getOtherNode(oneResult);

				if (otherNode.hasProperty(PAGE_TIMESTAMP)) {

					try {
						DateFormat format = new SimpleDateFormat(DEFAULT_DATE_FORMAT, Locale.ENGLISH);
						Date thisDate = format.parse(otherNode.getProperty(PAGE_LASTINDEX).toString());
						if (thisDate.after(mostRecent))
							mostRecent = thisDate;
					}

					catch (ParseException e) {
						System.out.println("Wrong date format: " + e);
					}
				}
			}
			System.out.println(oneResult.getProperty(KEYWORD_NAME) + " ## pages: " + relCount + " ## most recent: " + mostRecent.toString());
		}
	}

	// return the number of pages in a given index
	private int numPages(Index<Node> index) {

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
}