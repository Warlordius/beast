package com.github.beast.page;

import java.net.URL;

import net.htmlparser.jericho.Source;

import com.github.beast.parser.ParserSme;

public class PageSme extends Page {

	String heading;
	StringBuffer text;

	public PageSme(URL url) {

		super(url);
		this.setURL(url);
	}

	public Source parse() {

		ParserSme parser = new ParserSme();
		Source source = parser.parsePage(this);
		return source;
	}	
}