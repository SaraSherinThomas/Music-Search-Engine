import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.NoSuchElementException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


public class InternalLinks {

	public static HashSet<String> linksToBeVisited = new HashSet<String>(); 
	static int count = 0;
	static PrintWriter writer;
	
	public void createWriter() throws FileNotFoundException, UnsupportedEncodingException{
		writer = new PrintWriter("Output/InternalLinks.txt","UTF-8");
	}
	
	public HashSet<String> getLinktobevisited(){
		return linksToBeVisited;
	}
	
	public void addInternalLinks(String url){
		Document urlDoc;
		//count --> required number of url's to be crawled		
		if(linksToBeVisited.contains(url)|| count > 30){
			return;
		}else{
			linksToBeVisited.add(url);
			writer.println(url);
		    System.out.println("\n"+ count++);			
			try{
				//System.out.println(url + " : " + count);
				urlDoc = Jsoup.connect(url).validateTLSCertificates(false).timeout(30000).get();
				org.jsoup.select.Elements links = urlDoc.select("a");
				//System.out.print("\nNumber of links : " + links.size());
				
				for (org.jsoup.nodes.Element link : links) {
					if(link.absUrl("href").startsWith("http://www.hindilyrics.net/lyrics")){
						addInternalLinks(link.absUrl("href"));
					}
				}
			}
			catch(IOException | NoSuchElementException
					| NullPointerException e ) {
	
				System.out.println(e);
			}
		}

	}
	
	public void closeFile(){
		writer.close();		
	}

}
