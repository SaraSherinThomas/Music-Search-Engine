import java.io.IOException;
import java.util.Iterator;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;


public class Top10 {
	public int crawlLatest() throws IOException, SolrServerException{
		int i;
		HttpSolrServer server = new HttpSolrServer("http://localhost:8983/solr/Music/");
		org.jsoup.nodes.Document link = Jsoup
				.connect("http://www.hindilyrics.net/").timeout(30000).validateTLSCertificates(false)
				.get();
		Element detail=link.getElementById("new_thumb");
		for(i=0;i<detail.childNodeSize()-1;i++){
			SolrInputDocument doc = new SolrInputDocument();
			//System.out.println(detail.child(i).child(0).child(0).absUrl("src"));
			doc.addField( "topUrl", detail.child(i).child(0).absUrl("href"));
			doc.addField( "topTitle", detail.child(i).child(0).attr("title"));
			doc.addField("topImage",detail.child(i).child(0).child(0).absUrl("src"));
			doc.addField( "id", i);
			server.add(doc);
		    server.commit();
		    
		}
		return i;
	}
	
}
