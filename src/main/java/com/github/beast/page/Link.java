package beast.page;

import java.net.URL;

public class Link {
	public String anchorText;
	public URL url;
	
	public Link (String anchorText, URL url) {
		this.anchorText = anchorText;
		this.url = url;
	}
}