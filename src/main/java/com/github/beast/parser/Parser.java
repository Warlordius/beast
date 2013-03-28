package com.github.beast.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import com.github.beast.page.*;
import com.github.beast.util.Utility;

import net.htmlparser.jericho.*;

public class Parser {

	public void parsePage(Page page) {

			
	}

	public String parseTitle(Page page) {

		Source source = new Source(page.getCode());
		Element title = source.getFirstElement("title");
		return title.getContent().toString().trim();
	}

	public StringBuffer parseContent(Page page) {

		StringBuffer content = new StringBuffer(page.getCode());
		return content;
	}

	// parse all links
	public List<Link> parseLinks(Page page) {

		Source source = new Source(page.getCode());
		List<Element> elements = source.getAllElements("a ");
		List<Link> links = new LinkedList<Link>();
		Iterator<Element> itr = elements.iterator();
		String hostUrl = page.getUrl().getProtocol() + "://" + page.getUrl().getHost();
		URL url = null;
		Link newLink;
		Element element;
		String anchorText;
		String urlText;

		while (itr.hasNext()) {
			element = itr.next();
			anchorText = element.getContent().toString();
			urlText = element.getStartTag().getAttributeValue("href");

			if (urlText != null) {
				if (urlText.startsWith("/")) {
					urlText = hostUrl + urlText;
				}

				try {
					url = Utility.stringToURL(urlText);
					newLink = new Link(anchorText, url);
					links.add(newLink);
				} catch (MalformedURLException e) {
					System.err.println("Malformed URL: " + urlText);
				}
			}
		}

		return links;
	}

	// TODO: fix duplicate code with previous method
	// parse links with a given host url
	public List<Link> parseLinks(final Segment segment, final String hostUrl) {

		List<Link> links = new LinkedList<Link>();
		Element element;
		String anchorText;
		String urlText;
		URL url = null;

		if (segment == null) {
			return links;
		}

		List<Element> elements = segment.getAllElements("a ");

		Iterator<Element> itr = elements.iterator();

		while (itr.hasNext()) {
			element = itr.next();
			anchorText = element.getContent().toString();
			urlText = element.getStartTag().getAttributeValue("href");

			if (urlText != null) {
				
				if (urlText.startsWith("/")) {
					urlText = hostUrl + urlText;
				}
				
				try {
					url = Utility.stringToURL(urlText);
					Link newLink = new Link(anchorText, url);
					links.add(newLink);
				} catch (MalformedURLException e) {
					System.err.println("Malformed URL: " + urlText);
				}
			}
		}
		return links;
	}
}