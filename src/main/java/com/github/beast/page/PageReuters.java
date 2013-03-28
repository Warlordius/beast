package com.github.beast.page;

import java.net.URL;

import net.htmlparser.jericho.Source;

import com.github.beast.parser.ParserReuters;

public class PageReuters extends Page {

	public PageReuters(URL url) {

		super(url);
		this.setURL(url);
		this.setParser(new ParserReuters());
	}

	public void parse() {

		ParserReuters parser = (ParserReuters) getParser();
		
		setTitle(parser.parseTitle(this));		
		setLinks(parser.parseLinks(this));
		setPerex(parser.parsePerex(this));
		setText(parser.parseText(this));
		setLocation(parser.parseLocation(this));
		setTimestamp(parser.parseTimestamp(this));
	}
}