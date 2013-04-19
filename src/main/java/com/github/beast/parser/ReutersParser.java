package com.github.beast.parser;

import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.github.beast.page.Link;
import com.github.beast.page.Page;
import com.github.beast.page.ReutersPage;
import com.github.beast.util.Utility;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

/**
 * Singleton class representing a {@link Parser} for Reuters web pages. In
 * addition to standard <code>Parser</code> class, ability to extract timestamp,
 * location and perex of a page are added, through new methods
 * <ul>
 * <li>{@link #parseTimestamp(ReutersPage)}</li>
 * <li>{@link #parseLocation(ReutersPage)}</li>
 * <li>{@link #parsePerex(ReutersPage)}</li>
 * </ul>
 * Furthermore, extraction of title is suited for Reuters pages, overriding
 * {@link #parseTitle(ReutersPage)}, so that only the title of article is
 * extracted, omitting other text. Link extraction through {@link
 * parseLinks(ReutersPage)} is changed, so that links are extracted only from
 * specific parts of a page, in order to avoid following links not leading to
 * articles.
 * 
 * @author Štefan Sabo
 * @version 1.0
 */
public final class ReutersParser extends Parser {

	private static final String HOMEPAGE = "http://www.reuters.com";

	/** Instance of a singleton class. */
	private static ReutersParser instance;

	/**
	 * Constructor of the <code>ReutersParser</code> class. Is private because
	 * <code>Parser</code> is a singleton class.
	 */
	private ReutersParser() {

	}

	/**
	 * Returns instance of the <code>ReutersParser</code> singleton class. If no
	 * instance exists, new instance is created.
	 * 
	 * @return instance of the singleton class
	 */
	public static ReutersParser getInstance() {

		if (instance == null) {
			instance = new ReutersParser();
		}
		return instance;
	}

	/**
	 * Returns the starting pages extracted from the Reuters home page.
	 * Currently is set to extract starting pages from news making headlines.
	 * 
	 * @return list of strings representing URLs of starting pages
	 */
	public List<Link> getStartingPages() {

		final String homePage = "http://www.reuters.com";
		ReutersPage page;

		try {
			page = new ReutersPage(Utility.stringToURL(homePage));
			List<Link> pages = getLatestHeadlines(page);
			return pages;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Extracts HTML {@link Link links} pointing to articles from a given
	 * {@link ReutersPage}. All relative links are changed to absolute and
	 * protocol is prefixed in order to obtain valid absolute URLs.
	 * <p>
	 * Links to articles are extracted from <i>related news</i> and <i>most
	 * popular</i> sections. If no link is found in both sections, the page is
	 * considered to be a fact summary and an attempt is made to extract links
	 * from <i>related facts</i> box.
	 * 
	 * @param page the reuters page to be processed
	 * @return list of outgoing links to articles in the processed page
	 */
	public List<Link> parseLinks(final ReutersPage page) {

		Source source = new Source(page.getCode());
		List<Link> linksRelated = parseLinks(source.getElementById("relatedNews"), HOMEPAGE);
		List<Link> linksDiscussed = parseLinks(source.getElementById("most-popular"), HOMEPAGE);

		// if no related pages are found, try extracting related facts
		if ((linksRelated.size() == 0) && (linksDiscussed.size() == 0)) {
			return parseLinks(source.getElementById("relatedFactboxes"), "http://www.reuters.com");
		}

		for (Link link : linksDiscussed) {
			if (!(linksRelated.contains(link))) {
				linksRelated.add(link);
			}
		}
		return linksRelated;
	}

	/**
	 * Attempts to extract <i>location</i> information from a
	 * {@link ReutersPage}. Location refers to a text label at the start of the
	 * article, naming the location relevant to the actual article (e.g.
	 * "<i>PARIS, France</i>").
	 * 
	 * @param page the page containing the processed article
	 * @return text of the <i>location</i> label
	 */
	public String parseLocation(final ReutersPage page) {

		Source source = new Source(page.getCode());
		Element articleElement = source.getElementById("articleText");
		Element locationElement;
		String location;

		try {
			locationElement = articleElement.getFirstElement("class", "location", true);
			location = new String(locationElement.getContent().toString());
		} catch (NullPointerException e) {
			return null;
		}
		return location;
	}

	/**
	 * Extracts the highlighted first paragraph from a {@link ReutersPage}. A
	 * paragraph is returned, only if properly highlighted in text, otherwise
	 * <code>null</code> is returned.
	 * 
	 * @param page the page containing the processed article
	 * @return text of the highlighted first article, or <code>null</code> if no
	 *         highlighted first article is present
	 */
	public StringBuffer parsePerex(final ReutersPage page) {

		Source source = new Source(page.getCode());
		Element articleElement;
		Element focus;
		TextExtractor focusExtractor;
		StringBuffer perex;

		try {
			articleElement = source.getElementById("articleText");
			focus = articleElement.getFirstElement("class", "focusParagraph", true);
			focusExtractor = new TextExtractor(focus);
			perex = new StringBuffer(focusExtractor.toString());
		} catch (NullPointerException e) {
			return null;
		}
		return perex;
	}

	/**
	 * Attempts to extract the text of an article from a {@link ReutersPage}.
	 * The extraction is based on the occurrence of "<i>articleText</i> element
	 * in the HTML code of the processed page. If no article text is identified
	 * within the page.
	 * 
	 * @param page the processed
	 * @return text of the article in the page, or <code>null</code> if no
	 *         article is present
	 */
	public StringBuffer parseText(final ReutersPage page) {

		Source source = new Source(page.getCode());
		Element articleElement;
		TextExtractor extractor;
		StringBuffer pageText;

		try {
			articleElement = source.getElementById("articleText");
			extractor = new TextExtractor(articleElement);
			pageText = new StringBuffer(extractor.toString());
		} catch (NullPointerException e) {
			System.err.println("No article text in page body: " + page.getUrl());
			throw new NullPointerException();
		}
		return pageText;
	}

	/**
	 * Attempts to extract <i>timestamp</i> information from a
	 * {@link ReutersPage}. Timestamp refers to a text label at the start of the
	 * article, naming the date and time of last modification of the article.
	 * 
	 * @param page the page containing the processed article
	 * @return last modification date and time of the article, or
	 *         <code>null</code> if no such information is present
	 */
	public Date parseTimestamp(final ReutersPage page) {

		Source source = new Source(page.getCode());
		Element articleElement = source.getElementById("articleText");
		Element timeElement;
		String timeString = null;
		Date timestamp;

		try {
			timeElement = articleElement.getFirstElement("class", "timestamp", true);
			timeString = timeElement.getContent().toString();
			timestamp = new SimpleDateFormat("EEE MMM dd, yyyy hh:mmaa zzz", Locale.ENGLISH).parse(timeString);
		} catch (NullPointerException e) {
			return null;
		} catch (ParseException e) {
			System.err.println("Invalid timestamp format: " + timeString);
			return null;
		}
		return timestamp;
	}

	/**
	 * Extracts the title from a {@link ReutersPage}. The part after "|" (i.e.
	 * " | Reuters ") is discarded.
	 * 
	 * @param page a page to be processed.
	 * @return title of a page.
	 */
	public String parseTitle(final Page page) {

		Source source = new Source(page.getCode());
		Element titleElement = source.getFirstElement("title");
		String title = titleElement.getContent().toString();

		int pos = title.lastIndexOf('|');
		if (pos > 0) {
			title = title.substring(0, pos);
		}
		return title.trim();
	}

	/**
	 * Extracts HTML {@link Link links} pointing to articles from a given
	 * {@link Segment} of a page. All relative links are changed to absolute and
	 * protocol is prefixed in order to obtain valid absolute URLs.
	 * <p>
	 * Extraction is based on {@link Parser#parseLinks(Segment, String)} method,
	 * however all links not containing either string "<i>places</i>" or string
	 * "<i>articles/</i>" are removed, as these do not generally point to text
	 * articles in Reuters web page.
	 * 
	 * @param segment the segment of a HTML document to be processed
	 * @param hostUrl the URL of containing document, needed in order to handle
	 *        relative links
	 * @return list of outgoing links pointing to articles in the processed
	 *         segment
	 */
	protected List<Link> parseLinks(final Segment segment, final String hostUrl) {

		Link link;
		List<Link> links = new LinkedList<Link>();
		links = super.parseLinks(segment, hostUrl);
		String urlText;
		Iterator<Link> iter = links.iterator();

		while (iter.hasNext()) {
			link = iter.next();
			urlText = link.getUrl().toString();
			if (!(urlText.contains("article/")) && !(urlText.contains("places"))) {
				iter.remove();
			}
		}
		return links;
	}

	/**
	 * Extracts articles making the latest headlines from a Reuters page that
	 * are used as a starting point for search.
	 * 
	 * @param page - page where links of latest headlines are expected to be
	 *        found
	 * @return list of strings containing urls of extracted pages
	 */
	private List<Link> getLatestHeadlines(final Page page) {

		Source source = new Source(page.getCode());
		Element topStoryElement = source.getElementById("topStory");
		Element bigStoryElement = source.getElementById("topStoryNuclear");
		Element headlineElement = source.getElementById("latestHeadlines");
		String hostUrl = page.getUrl().getHost();

		List<Link> links;
		List<Link> topLinks;

		// breaking news - no other stories present
		if (bigStoryElement != null) {

			links = parseLinks(bigStoryElement, hostUrl);
			return links;
		}

		links = parseLinks(topStoryElement, hostUrl);
		topLinks = parseLinks(headlineElement, hostUrl);

		if (topLinks.size() > 0) {
			links.addAll(topLinks);
		}
		return links;
	}
}