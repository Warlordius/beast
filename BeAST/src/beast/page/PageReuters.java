package beast.page;

import java.net.URL;

import net.htmlparser.jericho.Source;

import beast.parser.ParserReuters;

public class PageReuters extends Page{
	
	String heading;
			
	public PageReuters(URL url) {
		super(url);
		this.setURL(url);
	}
	
	public Source parse(){
		ParserReuters parser = new ParserReuters();
		parser.parsePage(this);		
		
		return this.getSource();	
	}
}