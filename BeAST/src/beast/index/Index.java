package beast.index;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.FieldInfo.IndexOptions;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import beast.page.Page;


public class Index{
	public static final String FILE_DIRECTORY = "pages";
	public static final String INDEX_DIRECTORY = "index";
	
	public LinkedList<Page> pages;
	
	public Index(){
		pages = new LinkedList<Page>();
	}
	
	public void createIndex(){
		createIndex((File)null, true);
	}
	
	public void createIndex(boolean create) {
		 createIndex((File)null, create);
	}
	
	public void createIndex(Page page, boolean create) {
		if (createIndex(page.file, create) == 0) {
			 page.lastIndexed = new Date();
			 page.indexed = true;			 
		 }
	}
	
	public void createIndex(Page page) {
		 createIndex(page, false);
	}
	
	// TODO: clean up to where it belongs
	public boolean containsUrl(URL url) {
		Iterator<Page> itr = pages.iterator();
		while (itr.hasNext()) {
			if (itr.next().url.equals(url)) {
				return true;
			}
		}
		return false;
	}
	
	public int createIndex(File docDir, boolean create) {
		
		final boolean announce = false;
		
		if ( (docDir == null) || (!docDir.isFile()) ) {
			docDir = new File(FILE_DIRECTORY);
		}
		
		File indexDir = new File(INDEX_DIRECTORY);
		String indexPath = indexDir.getAbsolutePath();
		Date start = new Date();
		
		if (!docDir.exists() || !docDir.canRead()) {
		      System.out.println("Document directory '" +docDir.getAbsolutePath()+ "' does not exist or is not readable, please check the path");
		      return -1;
		    }
		
		try {
		      if (announce) {
		      	System.out.println("Indexing to directory '" + indexDir+ "'...");
		      }

		      Directory dir = FSDirectory.open(new File(indexPath));
		      Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_31);
		      IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_31, analyzer);

		      if (create) {
		        // Create a new index in the directory, removing any
		        // previously indexed documents:
		        iwc.setOpenMode(OpenMode.CREATE);
		      } else {
		        // Add new documents to an existing index:
		        iwc.setOpenMode(OpenMode.CREATE_OR_APPEND);
		      }

		      // Optional: for better indexing performance, if you
		      // are indexing many documents, increase the RAM
		      // buffer.  But if you do this, increase the max heap
		      // size to the JVM (eg add -Xmx512m or -Xmx1g):
		      //
		      // iwc.setRAMBufferSizeMB(256.0);

		      IndexWriter writer = new IndexWriter(dir, iwc);
		      indexDocs(writer, docDir, announce);

		      // NOTE: if you want to maximize search performance,
		      // you can optionally call forceMerge here.  This can be
		      // a terribly costly operation, so generally it's only
		      // worth it when your index is relatively static (ie
		      // you're done adding documents to it):
		      //
		      // writer.forceMerge(1);

		      writer.close();

		      Date end = new Date();
		      if (announce) {
		      	System.out.println(end.getTime() - start.getTime() + " total milliseconds");
		      }

		    } catch (IOException e) {
		      System.out.println(" caught a " + e.getClass() +
		       "\n with message: " + e.getMessage());
		      
		      return -1;
		    }
		
		return 0;
	}
	
	static void indexDocs(IndexWriter writer, File file, boolean announce) throws IOException {
		    // do not try to index files that cannot be read
		    if (file.canRead()) {
		      if (file.isDirectory()) {
		        String[] files = file.list();
		        // an IO error could occur
		        if (files != null) {
		          for (int i = 0; i < files.length; i++) {
		            indexDocs(writer, new File(file, files[i]), announce);
		          }
		        }
		      } else {

		        FileInputStream fis;
		        try {
		          fis = new FileInputStream(file);
		        } catch (FileNotFoundException fnfe) {
		          // at least on windows, some temporary files raise this exception with an "access denied" message
		          // checking if the file can be read doesn't help
		          return;
		        }

		        try {

		          // make a new, empty document
		          Document doc = new Document();

		          // Add the path of the file as a field named "path".  Use a
		          // field that is indexed (i.e. searchable), but don't tokenize 
		          // the field into separate words and don't index term frequency
		          // or positional information:
		          Field pathField = new Field("path", file.getPath(), Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS);
		          pathField.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
		          doc.add(pathField);

		          // Add the last modified date of the file a field named "modified".
		          // Use a NumericField that is indexed (i.e. efficiently filterable with
		          // NumericRangeFilter).  This indexes to milli-second resolution, which
		          // is often too fine.  You could instead create a number based on
		          // year/month/day/hour/minutes/seconds, down the resolution you require.
		          // For example the long value 2011021714 would mean
		          // February 17, 2011, 2-3 PM.
		          NumericField modifiedField = new NumericField("modified");
		          modifiedField.setLongValue(file.lastModified());
		          doc.add(modifiedField);

		          // Add the contents of the file to a field named "contents".  Specify a Reader,
		          // so that the text of the file is tokenized and indexed, but not stored.
		          // Note that FileReader expects the file to be in UTF-8 encoding.
		          // If that's not the case searching for special characters will fail.
		          doc.add(new Field("contents", new BufferedReader(new InputStreamReader(fis, "UTF-8"))));
		          
		          if (writer.getConfig().getOpenMode() == OpenMode.CREATE) {
		            // New index, so we just add the document (no old document can be there):
		            if (announce) {
		            	System.out.println("adding " + file);
		            }
		            writer.addDocument(doc);
		          } else {
		            // Existing index (an old copy of this document may have been indexed) so 
		            // we use updateDocument instead to replace the old one matching the exact 
		            // path, if present:
		          	if (announce) {
		          		System.out.println("updating " + file);
		          	}
		            writer.updateDocument(new Term("path", file.getPath()), doc);
		          }
		          
		        } finally {
		          fis.close();
		        }
		      }
		    } 
	}	
	
}