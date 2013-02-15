package beast.page;

import java.net.URL;

import net.htmlparser.jericho.Source;

import beast.parser.ParserSme;

public class PageSme extends Page{
	
	String heading;
	StringBuffer text;
		
	public PageSme(URL url){
		super(url);
		this.setURL(url);
	}
	
	public Source parse(){
		ParserSme parser = new ParserSme();
		Source source = parser.parsePage(this);
		return source;
	}
	
	/*public void parse(){
	}
		
		if (code==null)
			loadCode();
		
		String lookup="<h1>";
		int pos=code.indexOf(lookup);
		int posEnd=code.indexOf("</h1>",pos);
		
		heading=code.substring(pos+lookup.length(),posEnd);
		
		lookup="itext_content\">";
		pos=code.indexOf(lookup);
		posEnd=code.indexOf("</div>",pos);
		
		text=new StringBuffer(code.substring(pos+lookup.length(),posEnd));
		text=StripTags(text,true);
		getLinks();
	}
	
	public int getLinks(){
		int openTagStart=0;
		int openTagEnd=0;
		int closeTagStart=0;
		//int closeTagEnd=0;
		int hrefStart=0;
		int hrefEnd=0;
					
		while (openTagStart>=0){
			openTagStart=text.indexOf("<a ", openTagEnd);
			if (openTagStart<0)
				continue;
			openTagEnd=text.indexOf(">",openTagStart);
			closeTagStart=text.indexOf("</a",openTagEnd);
			//closeTagEnd=text.indexOf(">",closeTagStart);
			hrefStart=text.indexOf("href=",openTagStart);
			if (text.indexOf("\"",hrefStart)<=hrefStart+5)
				hrefEnd=text.indexOf("\"",hrefStart+6);
			else if(text.indexOf("\'",hrefStart)<=hrefStart+5)
				hrefEnd=text.indexOf("\'",hrefStart+6);
			String anchorText=text.substring(openTagEnd+1,closeTagStart);
			String url=text.substring(hrefStart,hrefEnd);
			URL newURL=StringToURL(url);
			if (newURL!=null){
				Link newLink=new Link(anchorText,newURL);
				System.out.println(newLink.anchorText+"'"+newLink.url);
			}
		}
				
		return 0;
	}*/
}