package com.github.beast.page;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import com.github.beast.parser.ReutersParser;
import com.github.beast.util.Configuration;

/**
 * An extension of {@link Page} class, suited specifically for pages containing
 * news articles. ArticlePage uses different default {@link Parser} and holds
 * multiple fields specific to news article pages, such as {@link #timestamp} of
 * the news article, {@link #location} of the news event, {@link #perex}
 * containing first paragraph of the article and {@link #text} containing the
 * text of the article within ArticlePage.
 * <p>
 * In addition, the text of the contained article may be archived to disk, using
 * {@link #writeToFile(ArticlePage)} method, for future reference.
 * 
 * @author Štefan Sabo
 * @version 1.0
 */
public class ArticlePage extends Page {

	/** Location field extracted from the article content of the page. */
	private String location;

	/** Timestamp field extracted from the article content of the page. */
	private Date timestamp;

	/** Article text extracted from the page HTML code. */
	private StringBuffer text;

	/** First paragraph of the article extracted from the page HTML code. */
	private StringBuffer perex;

	/**
	 * Creates a new ArticlePage from a valid {@link URL}.
	 * 
	 * @param url the url of the created page, serves as an identifier of the
	 *        page and is used for future refreshing of the page
	 */
	public ArticlePage(final URL url) {

		super(url);
		this.setParser(ReutersParser.getInstance());
	}

	/**
	 * Writes the content of a page to a archiveFile. Currently only the article
	 * text is stored from a page, other parts are omitted. The archiveFile is
	 * named according to the page title, replacing all illegal characters with
	 * blanks.
	 * 
	 * @param page the page to be archived
	 * @return reference to the created archive file
	 */
	public static File writeToFile(final ArticlePage page) {

		final String fileExtension = "html";

		if (!page.isProcessed()) {
			page.process();
		}

		String path = new String(Configuration.getInstance().getPageArchiveDir());
		String filename = page.getTitle().replaceAll("[\\/|:*?<>\"]", " ").trim() + "." + fileExtension;
		File pageFile = new File(path, filename);
		FileWriter writer;

		try {
			writer = new FileWriter(pageFile);
			writer.append(page.getText());
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}

		return pageFile;
	}

	/**
	 * Attempts to process a page. The processing consists of retrieving the
	 * code of the page if not yet available, parsing the content of the page
	 * and optionally archiving the page, if archiving is enabled in
	 * {@link Configuration}.
	 * 
	 * @param reprocess if <i>true</i>, forces reprocessing of the page, even if
	 *        it has been processed before. If <i>false</i> page will be
	 *        processed only once.
	 * @throws NullPointerException if unable to retrieve the code of the page
	 */
	public void process(final boolean reprocess) throws NullPointerException {

		super.process(reprocess);

		if (Configuration.getInstance().usePageArchive()) {
			this.setArchiveFile(writeToFile(this));
		}
	}

	/**
	 * @return the location field of the page
	 */
	public String getLocation() {

		return location;
	}

	/**
	 * @return the {@link #perex} of the page
	 */
	public StringBuffer getPerex() {

		return perex;
	}

	/**
	 * @return the {@link #text} content of the page
	 */
	public StringBuffer getText() {

		return text;
	}

	/**
	 * @return the {@link #timestamp} field of the page
	 */
	public Date getTimestamp() {

		return timestamp;
	}

	/**
	 * @param location the new location field of the page
	 */
	public void setLocation(final String location) {

		this.location = location;
	}

	/**
	 * @param perex the new {@link #perex} of the page
	 */
	public void setPerex(final StringBuffer perex) {

		this.perex = perex;
	}

	/**
	 * @param text new {@link #text} content of the page
	 */
	public void setText(final StringBuffer text) {

		this.text = text;
	}

	/**
	 * @param timestamp the new {@link #timestamp} of the page
	 */
	public void setTimestamp(final Date timestamp) {

		this.timestamp = timestamp;
	}
}