package com.github.beast.page;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.List;

import com.github.beast.parser.Parser;
import com.github.beast.util.Utility;

/**
 * Represents a single web page identified by its {@link URL}. The URL needs to
 * be supplied at the construction of the Page.
 * <p>
 * Initialized Page holds only the URL. The HTML code of the Page is obtained
 * through calling {@link #getCode()} for the first time. In order to extract
 * outgoing links and fields such as {@link #title}, the {@link #parse()} method
 * needs to be called. Parsing of the Page is handled by appropriate
 * {@link Parser}, assigned to the Page at construction.
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

	/** Last time the page has been indexed. */
	private Date lastIndexed;

	/**
	 * Last time the page has been processed.
	 * 
	 * @see #process(boolean)
	 */
	private Date lastProcessed;

	/** List of outgoing {@link Link links} from the page. */
	private List<Link> links;

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
	 * Creates a new Page from a valid {@link URL} and assigns a new
	 * {@link Parser}, that will be used to process the code of the page.
	 * 
	 * @param url the url of the created page, serves as an identifier of the
	 *        page and is used for future refreshing of the page
	 */
	public Page(final URL url) {

		setURL(url);
		setParser(Parser.getInstance());
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

		parse(this.parser);
	}

	/**
	 * Parse the content of the page using supplied {@link Parser}, extracting
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
	 * code of the page if not yet available and parsing the content of the
	 * page.
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

		this.parse();
		this.setProcessed(true);
		this.setLastProcessed(new Date());
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
	public void setArchiveFile(final File archiveFile) {

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