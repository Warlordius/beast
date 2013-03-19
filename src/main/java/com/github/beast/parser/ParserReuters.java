package com.github.beast.parser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.github.beast.page.Link;
import com.github.beast.page.Page;
import com.github.beast.page.PageReuters;
import com.github.beast.utility.Utility;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

public class ParserReuters extends Parser {

	private final static String HOMEPAGE = "http://www.reuters.com";

	public Source parsePage(PageReuters page) {

		page.title = parseTitle(page);
		page.setContent(parseContent(page));

		// extract links from the page
		page.links = parseLinks(page);

		// if no related pages are found, try extracting related facts
		if (page.links.size() == 0)
			page.links = parseLinks(page.getSource().getElementById("relatedFactboxes"), "http://www.reuters.com");

		return page.getSource();
	}

	/**
	 * Extracts articles making the latest headlines from a page (currently,
	 * reuters type is assumed), that are used as a starting point for search
	 * 
	 * @param page - page where links of latest headlines are expected to be
	 *        found
	 * @return list of strings containing urls of extracted pages
	 */
	private ArrayList<String> getLatestHeadlines(Page page) {

		Element bigStory = page.getSource().getElementById("topStoryNuclear");
		List<Element> elements;

		// breaking news - no other stories present
		if (bigStory != null) {

			Element linkElem = bigStory.getFirstElement("a ");
			ArrayList<String> links = new ArrayList<String>();

			String link = linkElem.getStartTag().getAttributeValue("href");

			link = cleanLink(link, page);

			if (link != null)
				links.add(link);

			return links;
		}

		elements = page.getSource().getElementById("latestHeadlines").getAllElements("a ");

		ArrayList<String> links = new ArrayList<String>();
		Iterator<Element> itr = elements.iterator();

		// top story
		Element topStory = page.getSource().getElementById("topStory").getFirstElement("a ");
		String topUrlText = topStory.getStartTag().getAttributeValue("href");

		if ((topUrlText = cleanLink(topUrlText, page)) != null) {
			links.add(topUrlText);
		}

		// other links
		while (itr.hasNext()) {
			Element element = itr.next();
			String urlText = element.getStartTag().getAttributeValue("href");

			if ((urlText = cleanLink(urlText, page)) != null) {
				links.add(urlText);
			}
		}

		return links;
	}

	/**
	 * Processes a link string and returns a valid full url string prefixed with
	 * protocol
	 * 
	 * @param link - String of link to be processed.
	 * @param page - Page from which the link has been extracted.
	 * @return Cleaned up version of url string ready to be processed.
	 */
	private String cleanLink(String link, Page page) {

		String hostUrl = page.url.getProtocol() + "://" + page.url.getHost();

		if ((link == null) || (link.contains("?videoId"))) {
			return null;
		}

		if (link.startsWith("/")) {
			link = hostUrl + link;
		}

		try {
			URL linkUrl = Utility.stringToURL(link);
			return linkUrl.toString();
		} catch (MalformedURLException e) {
			System.err.println("Malformed URL " + link);
			return null;
		}
	}

	/**
	 * Parses the title of a Reuters page.
	 * 
	 * @param page - Page to be parsed.
	 * @return String containing the title of a page.
	 */
	public String parseTitle(Page page) {

		Element title = page.getSource().getFirstElement("title");
		String rawTitle = title.getContent().toString();
		int pos = rawTitle.lastIndexOf('|');

		// cut out the part after last '|' character (i.e. " | Reuters ")
		if (pos > 0)
			rawTitle = rawTitle.substring(0, pos);

		return rawTitle.trim();
	}

	public List<Link> parseLinks(Page page) {

		List<Link> linksRelated = parseLinks(page.getSource().getElementById("relatedNews"), HOMEPAGE);
		List<Link> linksDiscussed = parseLinks(page.getSource().getElementById("most-popular"), HOMEPAGE);

		for (Link link : linksDiscussed) {
			if (!(linksRelated.contains(link))) {
				linksRelated.add(link);
			}
		}

		return linksRelated;
	}

	/**
	 * Parses the content of a Reuters page and updates the given fields of the
	 * page object passed to the method. Updated field include perex and text,
	 * updated every time and location plus timestamp, updated if available.
	 * 
	 * @param page - Page to be parsed and updated.
	 * @return StringBuffer containing the text of the article in the parsed
	 *         page.
	 */
	private StringBuffer parseContent(PageReuters page) {

		Source source = page.getSource();
		Element element = source.getElementById("articleText");
		Element focus = element.getFirstElement("class", "focusParagraph", true);
		TextExtractor focusExtractor = new TextExtractor(focus);
		page.perex = new StringBuffer(focusExtractor.toString());
		TextExtractor extractor = new TextExtractor(element);
		page.text = new StringBuffer(extractor.toString());

		if (element != null) {
			Element locElement = element.getFirstElement("class", "location", true);
			Element timeElement = element.getFirstElement("class", "timestamp", true);

			if (locElement != null) {
				page.location = new String(locElement.getContent().toString());
			}

			if (timeElement != null) {
				try {
					page.timestamp = new SimpleDateFormat("EEE MMM dd, yyyy hh:mmaa zzz", Locale.ENGLISH).parse(timeElement.getContent().toString());
				} catch (Exception e) {
					System.out.println("Wrong date / time format : " + timeElement.getContent().toString());
				}
			}
		}
		return page.text;
	}

	public StringBuffer parseContent(File file) {

		StringBuffer buffer = new StringBuffer();

		try {
			BufferedReader input = new BufferedReader(new FileReader(file));
			String line;

			while ((line = input.readLine()) != null) {
				buffer.append(line);
			}

			input.close();

		} catch (IOException e) {
			System.out.println("IO Exception: " + e);
		}

		// Source source = new Source(buffer);
		// Element element = source.getElementById("content");
		// Element element = source.getElementById("articleText");
		// Element focus = element.getFirstElement("class", "focusParagraph",
		// true);

		StringBuffer content = buffer;
		// TextExtractor focusExtractor = new TextExtractor(focus);
		// StringBuffer perex = new StringBuffer(focusExtractor.toString());

		// TextExtractor extractor = new TextExtractor(element);
		// StringBuffer text = new StringBuffer (extractor.toString());

		return content;
	}

	/**
	 * Returns the starting pages extracted from the reuters home page.
	 * Currently is set to extract starting pages from news making headlines.
	 * 
	 * @return ArrayList of strings representing URLs of starting pages.
	 */
	public static ArrayList<String> getStartingPages() {

		final String homePage = "http://www.reuters.com";
		PageReuters page;

		try {
			page = new PageReuters(Utility.stringToURL(homePage));
			ParserReuters parser = new ParserReuters();
			ArrayList<String> pages = parser.getLatestHeadlines(page);
			return pages;
		} catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			System.err.println(e.getStackTrace());
			return null;
		}
	}

	// TODO: fix duplicate code with Parser.parseLinks
	public List<Link> parseLinks(Segment segment, String hostUrl) {

		List<Link> links = new LinkedList<Link>();
		URL url;
		List<Element> elements;
		Iterator<Element> itr;
		String anchorText;
		String urlText;
		Element element;
		Link newLink;

		if (segment == null) {
			return links;
		}

		elements = segment.getAllElements("a ");

		itr = elements.iterator();

		while (itr.hasNext()) {

			element = itr.next();
			anchorText = element.getContent().toString();
			urlText = element.getStartTag().getAttributeValue("href");

			if (urlText != null) {

				if (urlText.startsWith("/")) {
					urlText = hostUrl + urlText;
				}

				if ((urlText.contains("article/")) || (urlText.contains("places"))) {
					try {
						url = Utility.stringToURL(urlText);
						newLink = new Link(anchorText, url);
						links.add(newLink);
					} catch (MalformedURLException e) {
						System.err.println("Malformed URL: " + urlText);
					}
				}
			}
		}
		return links;
	}
}