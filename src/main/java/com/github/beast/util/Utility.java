package com.github.beast.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.github.beast.page.Page;

/**
 * Utility class for <i>Beast</i> project. Contains static utility methods used
 * in the project.
 * 
 * @author Štefan Sabo
 * @version 1.0
 */
public abstract class Utility {

	private static final String DEFAULT_PROTOCOL = "http://";

	/**
	 * Creates an {@link URL} object from a {@link String}. Protocol prefix is
	 * automatically added, however the <i>string</i> needs to be well-formed,
	 * otherwise {@link MalformedURLException} is thrown.
	 * 
	 * @param string a <i>string</i> representing an URL
	 * @return URL object from the given <i>string</i>
	 * @throws MalformedURLException if supplied <i>string</i> does not
	 *         represent a well-formed URL
	 * @see URL
	 */
	public static final URL stringToURL(final String string) throws MalformedURLException {

		URL url;

		if (string.startsWith(DEFAULT_PROTOCOL)) {
			url = new URL(string);
		} else {
			url = new URL(DEFAULT_PROTOCOL + string);
		}
		return url;
	}

	/**
	 * Removes all HTML tags from a text, is is equivalent to calling {@link
	 * stripTags(StringBuffer, boolean)} with second parameter as <i>false</i>.
	 * 
	 * @param text the text to be stripped of HTML tags
	 * @return the input text with removed HTML tags
	 */
	public static final StringBuffer stripTags(final StringBuffer text) {

		return stripTags(text, false);
	}

	/**
	 * Removes HTML tags from a text. Anchor tags representing links may be
	 * optionally left in the text through passing <i>skipLinks = true</i>.
	 * 
	 * @param text the text to be stripped of HTML tags
	 * @param skipLinks <i>true</i> to exclude <i>anchor</i> tags from
	 *        stripping, <i>false</i> to strip all tags, without exclusions.
	 * @return the input text with removed HTML tags
	 */
	public static final StringBuffer stripTags(final StringBuffer text, final boolean skipLinks) {

		final int maxLenght = 255;
		int startPos = 0;
		int endPos = 0;
		String tmp;

		while (startPos >= 0) {
			startPos = text.indexOf("<", startPos);
			endPos = text.indexOf(">", startPos);
			if ((startPos >= 0) && (endPos >= 0) && (startPos < endPos) && (endPos - startPos <= maxLenght)) {
				tmp = text.substring(startPos);
				if (!(skipLinks) || ((!tmp.startsWith("</a")) && (!tmp.startsWith("<a ")))) {
					text.delete(startPos, endPos + 1);
				} else {
					startPos = endPos;
				}
			}
		}
		return text;
	}

	/**
	 * Fetches the HTML code of a given {@link Page page}, through HTTP request.
	 * 
	 * @param page the page, for which the code is to be fetched
	 * @return HTML code of the given page
	 * @throws NullPointerException if failed to obtain HTML code of the page
	 */
	public static StringBuffer requestCode(final Page page) throws NullPointerException {

		String line;
		URLConnection connection;
		BufferedReader reader;
		StringBuffer receivedCode = null;

		try {
			connection = page.getUrl().openConnection();
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			receivedCode = new StringBuffer();

			while ((line = reader.readLine()) != null) {
				receivedCode.append(line);
			}
			reader.close();

		} catch (IOException e) {
			e.printStackTrace();
			throw new NullPointerException();
		}

		if (receivedCode.length() == 0) {
			throw new NullPointerException();
		}

		return receivedCode;
	}
}