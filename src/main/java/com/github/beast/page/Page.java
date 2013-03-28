package com.github.beast.page;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.github.beast.Beast;
import com.github.beast.parser.Parser;
import com.github.beast.util.Configuration;
import com.github.beast.util.Utility;

/**
 * Abstract class representing a single web page in Beast project.
 * 
 * @author Štefan Sabo
 * @version 1.0
 */
public class Page {

	public static final boolean REPROCESS = true;
	public static final boolean FORCE_RETRIEVAL = true;

	/** Is <i>true</i> if the page has already been indexed. */
	private boolean indexed = false;

	/** If <i>true</i> if the page has already been processed. */
	private boolean processed = false;

	/** The absolute url of the page, serves as a unique identifier of the page. */
	private URL url;

	/** The title of the web page. */
	private String title;

	/** Location field extracted from the article content of the page. */
	private String location;

	/** Last time the page has been indexed. */
	private Date lastIndexed;

	/**
	 * Last time the page has been processed.
	 * 
	 * @see #process(boolean)
	 */
	private Date lastProcessed;

	/** Timestamp field extracted from the article content of the page. */
	private Date timestamp;

	/** List of outgoing {@link Link links} from the page. */
	private List<Link> links;

	/** Article text extracted from the page HTML code. */
	private StringBuffer text;

	/** First paragraph of the article extracted from the page HTML code. */
	private StringBuffer perex;
	
	/** HTML code of the page. */
	private StringBuffer code;

	/**
	 * A reference to local copy of page content on the disk, used if page
	 * archiving is enabled through setting {@link Configuration#pageArchive} to
	 * <i>true</i>.
	 */
	private File archiveFile;

	/**
	 * Default {@link Parser} used extract the content of the page.
	 */
	private Parser parser;

	/**
	 * Creates a new Page from a valid {@link URL}.
	 * 
	 * @param url the url of the created page, serves as an identifier of the
	 *        page and is used for future refreshing of the page
	 */
	public Page(final URL url) {

		setURL(url);
		setParser(new Parser());
	}

	/**
	 * Writes the content of a page to a archiveFile. Currently only the article
	 * text is stored from a page, other parts are omitted. The archiveFile is
	 * named according to the page title, replacing all illegal characters with
	 * blanks.
	 * 
	 * @param page the page to be writen to a archiveFile
	 * @return reference to the created archiveFile
	 */
	public static File writeToFile(final Page page) {

		final String fileExtension = "html";

		if (!page.isProcessed()) {
			page.process();
		}

		String path = new String(Beast.config.getPageArchiveDir());
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
	 * Adds a new {@link Link} to the list of outgoing links.
	 * 
	 * @param link the new link to be added
	 */
	public void addLink(final Link link) {

		links.add(link);
	}

	/**
	 * @return the file, where article text of the page is saved on the disk for
	 *         future reference.
	 * @see #archiveFile
	 */
	public File getArchiveFile() {

		return archiveFile;
	}

	/**
	 * Fetches the HTML code of a page. If the code is not available, retrieves
	 * the code through HTTP request, however does not force fresh retrieval if
	 * the code is already available. Is equivalent to calling
	 * {@link #getCode(boolean)} with parameter <i>false</i>.
	 * 
	 * @return the HTML code of a page.
	 * @see #requestCode()
	 */
	public StringBuffer getCode() {

		return getCode(false);
	}

	/**
	 * Fetches the HTML code of a page. If the code is not available, retrieves
	 * the code through HTTP request. Fresh retrieval may be forced through
	 * setting of <i>forceRetrieval = true</i>, otherwise the web page code is
	 * retrieved only if not yet available.
	 * 
	 * @param forceRetrieval <i>true</i> forces fresh retrieval of a page, even
	 *        if the code is already available
	 * @return the HTML code of a page
	 * @throws NullPointerException if returned code equals <i>null</i>
	 */
	public StringBuffer getCode(final boolean forceRetrieval) throws NullPointerException {

		if ((this.code == null) || (forceRetrieval)) {
			this.code = Utility.requestCode(this);
		}

		return this.code;
	}

	/**
	 * @return last time the page has been indexed
	 */
	public Date getLastIndexed() {

		return lastIndexed;
	}

	/**
	 * @return last time the page has been processed
	 * @see #process(boolean)
	 */
	public Date getLastProcessed() {

		return lastProcessed;
	}

	/**
	 * @return {@link List} of outgoing links from the page
	 */
	public List<Link> getLinks() {

		return links;
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
	 * @return the title of the page
	 */
	public String getTitle() {

		return title;
	}

	/**
	 * @return the {@link URL url} of the page
	 */
	public URL getUrl() {

		return url;
	}

	/**
	 * @return <i>true</i> if the page has already been indexed, otherwise
	 *         <i>false</i>
	 */
	public boolean isIndexed() {

		return indexed;
	}

	/**
	 * @return <i>true</i> if the page has been already retrieved and parsed,
	 *         othervise <i>false</i>
	 */
	public boolean isProcessed() {

		return processed;
	}

	/**
	 * Parse the content of the page using default {@link Parser}.
	 * 
	 * @see #parse(Parser)
	 */
	public void parse() {

		parse(parser);
	}

	/**
	 * Parser the content of the page using supplied {@link Parser}, extracting
	 * {@link #title} and {@link #links} from the HTML code of the page.
	 * 
	 * @param parser the {@link Parser} used to process the page
	 */
	public void parse(final Parser parser) {

		setTitle(parser.parseTitle(this));
		setLinks(parser.parseLinks(this));
	}

	/**
	 * Attempts to process a page without forced reprocessing in case the page
	 * has already been processed. Is equivalent to calling
	 * {@link #process(boolean)} with parameter <i>false</i>.
	 * 
	 * @see #process(boolean)
	 */
	public void process() {

		process(false);
	}

	/**
	 * Attempts to process a page. The processing consists of retrieving the
	 * code of the page, if not yet available, parsing the content and
	 * optionally archiving the page, if archiving is enabled in
	 * {@link Configuration}.
	 * 
	 * @param reprocess if <i>true</i>, forces reprocessing of the page, even if
	 *        it has been processed before. If <i>false</i> page will be
	 *        processed only once.
	 * @throws NullPointerException if unable to retrieve the code of the page
	 */
	public void process(final boolean reprocess) throws NullPointerException {

		setProcessed(false);

		if ((this.isProcessed()) && (!reprocess)) {
			return;
		}

		if ((reprocess) || (!this.isProcessed())) {
			code = getCode(FORCE_RETRIEVAL);
		} else {
			code = getCode();
		}

		parse();

		this.setProcessed(true);
		this.setLastProcessed(new Date());

		if (Configuration.getInstance().usePageArchive()) {
			this.setFile(writeToFile(this));
		}
	}

	/**
	 * Removes a {@link Link} from the list of outgoing links.
	 * 
	 * @param link the link to be removed
	 */
	public void removeLink(final Link link) {

		links.remove(link);
	}

	/**
	 * @param archiveFile the file containing the archived version of the page
	 * @see #archiveFile
	 */
	public void setFile(final File archiveFile) {

		this.archiveFile = archiveFile;
	}

	/**
	 * @param indexed boolean value determining, whether the page has already
	 *        been indexed
	 */
	public void setIndexed(final boolean indexed) {

		this.indexed = indexed;
	}

	/**
	 * @param lastIndexed represents the last time the page has been indexed
	 */
	public void setLastIndexed(final Date lastIndexed) {

		this.lastIndexed = lastIndexed;
	}

	/**
	 * @param lastProcessed represents the last time the page has been processed
	 * @see #process(boolean)
	 */
	public void setLastProcessed(final Date lastProcessed) {

		this.lastProcessed = lastProcessed;
	}

	/**
	 * @param links {@link List} of outgoing links from the page
	 */
	public void setLinks(final List<Link> links) {

		this.links = links;
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

	/**
	 * @param title the new title field of the page
	 */
	public void setTitle(final String title) {

		this.title = title;
	}

	/**
	 * @return the {@link Parser} assigned to process the page
	 */
	protected Parser getParser() {

		return this.parser;
	}

	/**
	 * @param parser the {@link Parser} to be used as default when processing
	 *        the page
	 */
	protected void setParser(final Parser parser) {

		this.parser = parser;
	}

	/**
	 * @param url the new URL of the page
	 */
	protected void setURL(final URL url) {

		this.url = url;
	}

	/**
	 * @param processed boolean value determining, whether the page has already
	 *        been processed
	 */
	private void setProcessed(final boolean processed) {

		this.processed = processed;
	}
}