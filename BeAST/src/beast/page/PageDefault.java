package beast.page;

import java.net.URL;

import beast.parser.Parser;

import net.htmlparser.jericho.Source;

public class PageDefault extends Page {
	
	public PageDefault(URL url) {
		super(url);
		this.setURL(url);
	}

	public Source parse() {
		Parser parser = new Parser();
		Source source = parser.parsePage(this);
		
		return source;
	}
	

}