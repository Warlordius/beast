package com.github.beast.parser;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;

import com.github.beast.page.Link;
import com.github.beast.page.Page;
import com.github.beast.util.Utility;

/**
 * A singleton class representing a parser to extract information from a HTML
 * document. The parsing engine provided by Jericho Parser. <code>Parser</code>
 * provides general <i>title</i> and <i>link</i> extracting capabilities and may
 * be used for any HTML document.
 * 
 * @author Štefan Sabo
 * @version 1.0
 * 
 * @see <a href="http://jerichohtml.sourceforge.net">Jericho parser</a>
 */
public class Parser {

	/** Instance of a singleton class. */
	private static Parser instance;

	/**
	 * Constructor of the <code>Parser</code> class. Is private because
	 * <code>Parser</code> is a singleton class.
	 */
	protected Parser() {

	}

	/**
	 * Returns instance of the <code>Parser</code> singleton class. If no
	 * instance exists, new instance is created.
	 * 
	 * @return instance of the singleton class
	 */
	public static Parser getInstance() {

		if (instance == null) {
			instance = new Parser();
		}
		return instance;
	}

	/**
	 * Extracts all HTML {@link Link links} from a given {@link Page}. All
	 * relative links are changed to absolute and protocol is prefixed in order
	 * to obtain valid absolute URLs.
	 * 
	 * @param page the page to be processed
	 * @return list of outgoing links in the processed page
	 * @see #parseLinks(Segment)
	 */
	public List<Link> parseLinks(final Page page) {

		Source source = new Source(page.getCode());
		String hostUrl = page.getUrl().getProtocol() + "://" + page.getUrl().getHost();
		List<Link> links = parseLinks(source, hostUrl);

		return links;
	}

	/**
	 * Extracts all HTML {@link Link links} from a given {@link Segment} of a
	 * page. All relative links are changed to absolute and protocol is prefixed
	 * in order to obtain valid absolute URLs.
	 * 
	 * @param segment the segment of a HTML document to be processed
	 * @param hostUrl the URL of containing document, needed in order to handle
	 *        relative links
	 * @return list of outgoing links in the processed segment
	 */
	protected List<Link> parseLinks(final Segment segment, final String hostUrl) {

		List<Link> links = new LinkedList<Link>();
		String anchorText;
		String urlText;
		URL url = null;
		List<Element> elements;

		if (segment == null) {
			return links;
		} else {
			elements = segment.getAllElements("a ");
		}

		for (Element element : elements) {

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

	/**
	 * Extracts a <i>title</i> attribute of the given {@link Page}.
	 * 
	 * @param page the page to be processed
	 * @return the title of the given page
	 */
	public String parseTitle(final Page page) {

		Source source = new Source(page.getCode());
		Element title = source.getFirstElement("title");
		return title.getContent().toString().trim();
	}
}