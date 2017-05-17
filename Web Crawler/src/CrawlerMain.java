import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.jdom.Element;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;

import com.google.api.services.samples.youtube.cmdline.data.Search;

//import com.google.api.services.youtube.YouTube.Search;

public class CrawlerMain {
	// Array for seed URLs
	static ArrayList<String> seedArray;
	private static PrintWriter writer; 
	
	public static void main(String[] args) throws IOException ,IllegalArgumentException,NoSuchElementException {
		// reading the seed URLSs
		Properties configFile = new Properties();
		InputStream inputStream = new FileInputStream(
				"resources\\config.properties");
		configFile.load(inputStream);
		List<String> seedInputFile = Files.readAllLines(new File(configFile
				.getProperty("INPUT_FILE")).toPath());

		// copying seed URLs to array
		seedArray = new ArrayList<String>(seedInputFile);
		Iterator<String> seedIterator = seedArray.iterator();
		while (seedIterator.hasNext()) {
			String nextSeed = seedIterator.next();
			SeedCrawler seedThread = new SeedCrawler(nextSeed);
			seedThread.start();
		}
		// creating or overwriting output file
		writer = new PrintWriter("Output.txt");
		writer.println("Number of files generated from respective Seed URLs :");
		writer.close();

	}
}

// creating threads for each seed URls
class SeedCrawler implements Runnable {

	private String seed;
	private Thread t;
	HashSet<String> linksToBeVisited = new HashSet<String>();
	static HashSet<String> allSeedLinks = new HashSet<String>();
	// to keep track of DOCIDs
	static AtomicInteger count = new AtomicInteger(0);

	// constructor
	SeedCrawler(String nextSeed) {
		this.seed = nextSeed;
	}

	public void run() {
		int c = 0;
		Top10 topSongs=new Top10();
		try {
			count= new AtomicInteger(topSongs.crawlLatest());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SolrServerException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		try {
			InternalLinks i=new InternalLinks();
			Search search=new Search();
					
			i.createWriter();
			//obtain the links to be visited
			i.addInternalLinks(seed);
			i.closeFile();
		linksToBeVisited = i.getLinktobevisited();
			//System.out.println("Size :  "+linksToBeVisited.size());
			// Iterator to access each internal link from hashSet
			Iterator<String> linkIterator = linksToBeVisited.iterator();
			Iterator<String> contentIterator = linksToBeVisited.iterator();
			linkIterator.next();
			contentIterator.next();
			
			HttpSolrServer server = new HttpSolrServer("http://localhost:8983/solr/Music/");
			Collection<SolrInputDocument> docList = new ArrayList<SolrInputDocument>();
			
			while (linkIterator.hasNext()) {

				try {
					// Jsoup connection to each internal link and extracting the
					// required details using Document and Elements Class
					org.jsoup.nodes.Document internalLinkDoc = Jsoup
							.connect(linkIterator.next().toString()).timeout(30000).validateTLSCertificates(false)
							.get();
					org.jsoup.select.Elements meta = internalLinkDoc.select("meta");
					//Elements details = internalLinkDoc.select("div.nm");
					//System.out.println(details.first().nextSibling());

					String url = contentIterator.next();
					
					// getting last-modified date from URL
					URL dateExtract = new URL(url);
					HttpURLConnection httpCon = (HttpURLConnection) dateExtract
							.openConnection();
					long modifiedDate = httpCon.getLastModified();
					Date date = new Date(modifiedDate);
					
					// storing content in a string
					String article = internalLinkDoc.body().text();

					// counting the number of words from the content extracted
					// in string "article"
					StringTokenizer termCount = new StringTokenizer(article);
					int tokenCount = termCount.countTokens();


					if (tokenCount > 150) {
						org.jsoup.select.Elements detail=internalLinkDoc.select("div.nm");
						org.jsoup.select.Elements imageURL=internalLinkDoc.select("img.mimg");

						if(detail.size()!=0){
							// printing required details to file of respective
							// internal link whose content has more than 150 terms

							PrintWriter writer = new PrintWriter("Output/Document_"
									+ count.incrementAndGet() + ".txt", "UTF-8");

							System.out.println(seed + "	:	" + count + "	:	"
									+ tokenCount + "	:" + url);

							writer.println("URL: " + url);
							writer.println("TITLE: " + internalLinkDoc.title());
							writer.println("METADATA: " + " Name: "
									+ meta.attr("name") + " - Content: "
									+ meta.attr("content"));
							writer.println("DATE: " + date.toString());
							writer.println("DOCID:" + (count));
							Iterator itr = detail.iterator();

							Element root=new Element("doc");
							SolrInputDocument doc = new SolrInputDocument();;

							doc.addField( "url", url);
							doc.addField( "title", internalLinkDoc.title());
							
							String YoutubeLink = search.SearchResult(internalLinkDoc.title());
							System.out.println(YoutubeLink);
							doc.addField("YoutubeLink", YoutubeLink);
							
							doc.addField( "metadata", meta.attr("name")+meta.attr("content"));
							//doc.addField( "date", date.toString());
							doc.addField( "id", count.toString());


							while(itr.hasNext()) {
								String token="";
								String singerURL="";
								org.jsoup.nodes.Element fieldValue=(org.jsoup.nodes.Element) itr.next();
								if(!(fieldValue.text().equals("Song") || fieldValue.text().equals("Movie") || fieldValue.text().equals("Singer(s)") || fieldValue.text().equals("Music By") || fieldValue.text().equals("Lyricist(s)"))){
									continue;
								}
								Node node = fieldValue.nextSibling();
								org.jsoup.nodes.Element e=fieldValue.nextElementSibling();
								for(int k=0;k<node.toString().length();k++){
									String names = node.toString();
									if(names.charAt(k)==','){
										singerURL=singerURL+","+"";
									}

								}
								if(e.tagName().equals("a")){
									while(e.tagName().equals("a")){
										if(node.toString().trim().equals(":")){
											token=token+","+e.html();
											singerURL=singerURL+","+e.attr("href");

										}
										else{
											token=token+node.toString().replaceFirst(":", ",");

											if(token.charAt(token.length()-2)==','){
												token=token.substring(0, token.length()-2);
											}

											token=token+","+e.html();
											singerURL=singerURL+","+e.attr("href");
										}
										e=e.nextElementSibling();
									}
									token=token.replaceFirst(",","");
									singerURL=singerURL.replaceFirst(",","");
								}
								else{
									token=node.toString().replaceFirst(":","");
								}


								switch(fieldValue.text()){
								case "Song":
									writer.println("Song" + token);
									doc.addField( "Song", token);
									System.out.println("\n"+token);
									break;
								case "Movie":
									writer.println("Movie" + token);
									doc.addField( "Movie", token);
									System.out.println("\n"+token);
									break;
								case "Singer(s)":
									writer.println("Singers" + token);
									for (String retval: token.split(",")) {
										doc.addField( "Singers", retval);
									}
									for (String retval: singerURL.split(",")) {
										doc.addField( "singerURL",  "http://www.hindilyrics.net"+retval);
									}
									System.out.println("\n"+token);
									break;
								case "Music By":
									writer.println("Music By" + token);
									doc.addField( "MusicBy", token);
									System.out.println("\n"+token);
									break;
								case "Lyricist(s)":
									writer.println("Lyricists" + token);
									doc.addField( "Lyricist", token);
									System.out.println("\n"+token);
									break;
								default :
									break;

								}		


							}
							writer.println("LYRICS: " + article);
							doc.addField( "Lyrics", article);
							doc.addField("imageURL", "http://www.hindilyrics.net"+imageURL.attr("src"));
							docList.add( doc );
							writer.close();
							// to keep track of number of files generated from each
							// Seed URL
							c++;
							server.add(doc);
							server.commit();



						}
					}

				} catch (IOException | NoSuchElementException
						| NullPointerException | SolrServerException e ) {

				e.printStackTrace();
					linkIterator.next();
				}

			}
			// printing number of files generated from each Seed URL
			File file = new File("Output.txt");
			FileWriter writer = new FileWriter(file.getName(), true);
			writer.write(seed + ":" + c);
			writer.write("\n");
			writer.close();

		} catch (IOException | NoSuchElementException e) {

			//System.out.println(e);

		}

	}

	// creating thread for each seed URLs
	public void start() {
		System.out.println("Starting " + seed);

		if (t == null) {

			t = new Thread(this, seed);
			t.start();

		}

	}

}