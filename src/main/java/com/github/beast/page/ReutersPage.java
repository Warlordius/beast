package com.github.beast.page;

import java.net.URL;

import com.github.beast.parser.ReutersParser;

/**
 * A specialized version of {@link ArticlePage} for processing of <i>Reuters</i>
 * articles. A {@link ReutersParser} is used for processing of article pages and
 * {@link #parse()} method is suited for extraction of features specific to
 * <i>Reuters</i> articles.
 * 
 * @author Štefan Sabo
 * @version 1.0
 * @see <a href="http://www.reuters.com">Reuters</a>
 */
public class ReutersPage extends ArticlePage {

	/**
	 * Creates a ReutersPage and assigns it a {@link ReutersParser}.
	 * 
	 * @param url the url of the created page, serves as an identifier of the
	 *        page and is used for future refreshing of the page
	 */
	public ReutersPage(final URL url) {

		super(url);
		this.setParser(ReutersParser.getInstance());
	}

	/**
	 * Parse the content of the page using default {@link Parser}. ReutesPage
	 * uses default Parser of type {@link ReutersParser}.
	 * 
	 * @see #parse(Parser)
	 */
	public void parse() {

		parse(getParser());
	}

	// TODO: fix
	
	public void process() {
		super.process();
	}
	
	public void process(boolean a) {
		super.process(a);
	}
	
	/**
	 * Parse the content of the page using supplied {@link ReutersParser}.
	 * 
	 * @param parser the {@link ReutersParser} used to process the page
	 * @see #parse(ReutersParser)
	 */
	public void parse(final ReutersParser parser) {

		setTitle(parser.parseTitle(this));
		setLinks(parser.parseLinks(this));
		setPerex(parser.parsePerex(this));
		setText(parser.parseText(this));
		setLocation(parser.parseLocation(this));
		setTimestamp(parser.parseTimestamp(this));
	}
}