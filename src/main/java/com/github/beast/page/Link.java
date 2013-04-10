package com.github.beast.page;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents a Web hyperlink.
 * 
 * Page holds the <i>URL</i> to which the link is pointing and optionally the
 * <i>anchor text</i> of the link. Link may be constructed using {@link URL}
 * object, or a {@link String} holding the target URL.
 * <p>
 * If a <i>String</i> is used for Link construction, it is necessary to use
 * well-formed URL, prefixed with protocol, otherwise
 * {@link MalformedURLException} is raised during construction.
 * 
 * @version 1.0
 * @author Štefan Sabo
 * @see URL
 */
public class Link {

	private String anchorText;
	private URL url;

	/**
	 * Creates a {@link Link} with a given url. Is equivalent to calling
	 * <code>Link(String, String)</code> with <code>null</code> as first
	 * parameter.
	 * 
	 * @param newUrl url of the link
	 * @throws MalformedURLException is thrown if a valid URL cannot be
	 *         constructed using <i>newUrl</i> parameter
	 * @see URL
	 */
	public Link(final String newUrl) throws MalformedURLException {

		new Link(null, newUrl);
	}

	/**
	 * Creates a {@link Link} with a given anchor text and url.
	 * 
	 * @param anchorText anchor text of the link
	 * @param url url of the link
	 * @throws MalformedURLException is thrown if a valid URL cannot be
	 *         constructed using <i>newUrl</i> parameter
	 * @see URL
	 */
	public Link(final String anchorText, final String url) throws MalformedURLException {

		this.setAnchorText(anchorText);
		this.setUrl(new URL(url));
	}

	/**
	 * Creates a {@link Link} with a given anchor text and url.
	 * 
	 * @param anchorText anchor text of the link
	 * @param url url of the link
	 * @see URL
	 */
	public Link(final String anchorText, final URL url) {

		this.setAnchorText(anchorText);
		this.setUrl(url);
	}

	/**
	 * Creates a {@link Link} with a given url. Is equivalent to calling
	 * <code>Link(String, URL)</code> with <code>null</code> as first parameter.
	 * 
	 * @param newUrl url of the link
	 * @see URL
	 */
	public Link(final URL newUrl) {

		new Link(null, newUrl);
	}

	/**
	 * @return the anchor text of the link
	 */
	public String getAnchorText() {

		return anchorText;
	}

	/**
	 * @return the url of the link
	 */
	public URL getUrl() {

		return url;
	}

	/**
	 * @param newAnchorText the new anchor text of the {@link Link}
	 */
	public void setAnchorText(final String newAnchorText) {

		this.anchorText = newAnchorText;
	}

	/**
	 * @param newUrl the new url of the {@link Link}
	 */
	public void setUrl(final URL newUrl) {

		url = newUrl;
	}
}