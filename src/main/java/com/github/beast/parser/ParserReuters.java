package com.github.beast.parser;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.github.beast.page.Link;
import com.github.beast.page.Page;
import com.github.beast.page.PageReuters;
import com.github.beast.util.Utility;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import net.htmlparser.jericho.TextExtractor;

public class ParserReuters extends Parser {

	private static final String HOMEPAGE = "http://www.reuters.com";

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
			e.printStackTrace();
			return null;
		}
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

	public List<Link> parseLinks(final Page page) {

		Source source = new Source(page.getCode());
		List<Link> linksRelated = parseLinks(source.getElementById("relatedNews"), HOMEPAGE);
		List<Link> linksDiscussed = parseLinks(source.getElementById("most-popular"), HOMEPAGE);
		
		// if no related pages are found, try extracting related facts
		if ((linksRelated.size() == 0) && (linksDiscussed.size() == 0)){
			return parseLinks(source.getElementById("relatedFactboxes"), "http://www.reuters.com");
		}

		for (Link link : linksDiscussed) {
			if (!(linksRelated.contains(link))) {
				linksRelated.add(link);
			}
		}

		return linksRelated;
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

	public String parseLocation(PageReuters page) throws NullPointerException {

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

	public StringBuffer parsePerex(PageReuters page) {

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

	public StringBuffer parseText(PageReuters page) {

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
			return null;
		}

		return pageText;
	}

	public Date parseTimestamp(PageReuters page) throws NullPointerException {

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
	 * Parses the title of a Reuters page.
	 * 
	 * @param page - Page to be parsed.
	 * @return String containing the title of a page.
	 */
	public String parseTitle(Page page) {

		Source source = new Source(page.getCode());
		Element title = source.getFirstElement("title");
		String rawTitle = title.getContent().toString();
		int pos = rawTitle.lastIndexOf('|');

		// cut out the part after last '|' character (i.e. " | Reuters ")
		if (pos > 0)
			rawTitle = rawTitle.substring(0, pos);

		return rawTitle.trim();
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

		String hostUrl = page.getUrl().getProtocol() + "://" + page.getUrl().getHost();

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
	 * Extracts articles making the latest headlines from a page (currently,
	 * reuters type is assumed), that are used as a starting point for search
	 * 
	 * @param page - page where links of latest headlines are expected to be
	 *        found
	 * @return list of strings containing urls of extracted pages
	 */
	private ArrayList<String> getLatestHeadlines(Page page) {

		Source source = new Source(page.getCode());
		Element bigStory = source.getElementById("topStoryNuclear");
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

		elements = source.getElementById("latestHeadlines").getAllElements("a ");

		ArrayList<String> links = new ArrayList<String>();
		Iterator<Element> itr = elements.iterator();

		// top story
		Element topStory = source.getElementById("topStory").getFirstElement("a ");
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
}