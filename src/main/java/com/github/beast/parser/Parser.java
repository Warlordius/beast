package com.github.beast.parser;

import java.net.URL;
import java.util.*;

import com.github.beast.page.*;

import net.htmlparser.jericho.*;

public class Parser{
	
	public Source parsePage(Page page) {	
		page.title = parseTitle(page);
		page.setContent(parseContent(page));
		page.links = parseLinks(page);
		
		return page.getSource();
	}
	
	public String parseTitle(Page page) {
		Element title = page.getSource().getFirstElement("title");
		return title.getContent().toString().trim();
	}

	public StringBuffer parseContent(Page page) {
		StringBuffer content = new StringBuffer(page.getSource().toString());
		return content;
	}
		
	// parse all links
	public List<Link> parseLinks(Page page) {
		List<Element> elements = page.getSource().getAllElements("a ");
		List<Link> links = new LinkedList<Link>();		
		Iterator<Element> itr = elements.iterator();
		String hostUrl = page.url.getProtocol()+"://"+page.url.getHost();
										
		while (itr.hasNext()) {			
			Element element = itr.next();
			String anchorText = element.getContent().toString();
			String urlText = element.getStartTag().getAttributeValue("href");
			
			if (urlText != null) {
				if (urlText.startsWith("/")) {					
					urlText=hostUrl+urlText;		
				}
				URL url = Page.StringToURL(urlText);
				if (url != null) {
					Link newLink = new Link(anchorText, url);
					links.add(newLink);	
				}				
			}			
		}
		
		return links;
	}
	
	// parse links with a given host url
	public List<Link> parseLinks(Segment segment, String hostUrl) {
		
		List<Link> links = new LinkedList<Link>();
		
		if (segment == null) {
			return links;
		}
		
		List<Element> elements = segment.getAllElements("a ");
				
		Iterator<Element> itr = elements.iterator();
										
		while (itr.hasNext()) {			
			Element element = itr.next();
			String anchorText = element.getContent().toString();
			String urlText = element.getStartTag().getAttributeValue("href");
			
			if (urlText != null) {
				if (urlText.startsWith("/")) {					
					urlText=hostUrl+urlText;		
				}
				
				URL url = Page.StringToURL(urlText);
				Link newLink = new Link(anchorText, url);				
				links.add(newLink);	
			}
		}
		return links;
	}
}