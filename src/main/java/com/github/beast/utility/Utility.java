package com.github.beast.utility;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Utility class for <i>Beast</i> project. Contains static utility methods used in the project.
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
}