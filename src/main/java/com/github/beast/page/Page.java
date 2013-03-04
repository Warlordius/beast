package com.github.beast.page;

import java.io.*;
import java.net.*;
import java.util.*;

import net.htmlparser.jericho.Source;

import com.github.beast.Model;
import com.github.beast.parser.Parser;

public abstract class Page {

    public static final String PAGE_DIR = "\\pages";
    public static final boolean REPROCESS = true;
    public static final boolean FORCE_RETRIEVAL = true;

    public URL url;
    public File file;
    public String title;
    public Date lastIndexed;
    public Date lastProcessed;
    public List<Link> links;
    public boolean indexed = false;
    public boolean processed = false;
    public boolean linksAdded = false;
    public Date timestamp;
    public String location;
    public double quality;

    public StringBuffer text;
    public StringBuffer perex;

    private Source source;
    private StringBuffer code;
    private StringBuffer content;

    public Page(URL url) {

	this.setURL(url);
    }

    public void setURL(URL url) {

	this.url = url;
    }

    public boolean process() {

	return process(false);
    }

    public boolean process(boolean reprocess) {

	if ((this.processed) && (!reprocess)) return true;

	if ((reprocess) || (code == null)) {
	    code = getCode(FORCE_RETRIEVAL);
	}

	if (code == null) return false;

	parse();
	this.file = writeToFile();
	this.processed = true;
	this.lastProcessed = new Date();

	return true;
    }

    public boolean reprocess() {

	processed = false;
	linksAdded = false;

	code = getCode(FORCE_RETRIEVAL);

	if (code == null) return false;

	parse();
	this.file = writeToFile();
	this.processed = true;
	this.lastProcessed = new Date();

	return true;
    }

    public void index() {

	System.out.println("indexing...");

	if ((Model.index.indexPage(this)) != null) {
	    indexed = true;
	    lastIndexed = new Date();
	}
    }

    public abstract Source parse();

    public File writeToFile() {

	Source source = this.getSource();

	if (source != null) {
	    this.source = source;
	} else {
	    System.out.println("Parsing error: " + url.toString());
	    return null;
	}

	if (this.content != null) {
	    String path = new String(Model.config.getResourceDir() + PAGE_DIR);
	    String filename = this.title.replaceAll("[\\/|:*?<>\"]", " ");
	    filename = filename.trim() + ".html";
	    file = new File(path, filename);
	    try {
		FileWriter writer = new FileWriter(file);
		writer.append(this.getContent());
		writer.close();
	    } catch (IOException e) {
		System.out.println("IO error when writing: " + e.getMessage());
	    }
	}
	return file;
    }

    public StringBuffer requestCode() {

	StringBuffer newCode = new StringBuffer();

	try {
	    URLConnection connection = url.openConnection();
	    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

	    String line = null;

	    while ((line = reader.readLine()) != null) {
		newCode.append(line);
	    }
	    reader.close();
	} catch (IOException e) {
	    System.out.println("Unable to retrieve page: " + e.getMessage());

	    return null;
	}
	return newCode;
    }

    public static final StringBuffer StripTags(StringBuffer text, boolean skipLinks) {

	int maxLength = 255;
	int startPos = 0;
	int endPos = 0;
	String tmp = new String();

	while (startPos >= 0) {
	    startPos = text.indexOf("<", startPos);
	    endPos = text.indexOf(">", startPos);
	    if ((startPos >= 0) && (endPos >= 0) && (startPos < endPos) && (endPos - startPos <= maxLength)) {
		tmp = text.substring(startPos, startPos + 3);
		if ((!tmp.contentEquals("</a")) && (!tmp.contentEquals("<a "))) {
		    text.delete(startPos, endPos + 1);
		} else {
		    startPos = endPos;
		}
	    }
	}
	return text;
    }

    public static final StringBuffer StripTags(StringBuffer text) {

	return StripTags(text, false);
    }

    public static final URL StringToURL(String string) {

	URL url;

	if (!string.startsWith("http://")) string = "http://" + string;
	try {
	    url = new URL(string);
	} catch (MalformedURLException e) {
	    // System.out.println("Malformed URL error: "+e.getMessage());
	    return null;
	}
	return url;
    }

    public StringBuffer getCode() {

	return getCode(false);
    }

    public StringBuffer getCode(boolean force) {

	if (this.code == null) {
	    this.code = requestCode();
	}

	return this.code;
    }

    public StringBuffer getContent() {

	if (this.content == null) {
	    Parser parser = new Parser();
	    this.content = parser.parseContent(this);
	}
	return this.content;
    }

    public Source getSource() {

	if (this.source == null) {

	    StringBuffer code = this.getCode();

	    if (code == null)
		return null;
	    else
		this.source = new Source(code);
	}

	return this.source;
    }

    public void setContent(StringBuffer newContent) {

	if (newContent != null) {
	    this.content = newContent;
	}
    }

    public void setSource(Source newSource) {

	if (newSource != null) {
	    this.source = newSource;
	}
    }
}