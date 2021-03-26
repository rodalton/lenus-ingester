import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.cloud.sdk.core.http.HttpMediaType;
import com.ibm.cloud.sdk.core.security.IamAuthenticator;
import com.ibm.watson.discovery.v1.Discovery;
import com.ibm.watson.discovery.v1.model.AddDocumentOptions;
import com.ibm.watson.discovery.v1.model.DocumentAccepted;

public class Lenus2Discovery {

	public static void main(String[] args) {
		try {

			//Watson Discovery auth & setup
			IamAuthenticator authenticator = new IamAuthenticator("gpTVQeabcvlLZoApevPDGOYlsJrvAGbACA3HWx5KkohX");
			Discovery discovery = new Discovery("2019-11-22", authenticator);
			discovery.setServiceUrl(
					"https://api.eu-de.discovery.watson.cloud.ibm.com/instances/174280be-5b20-44f2-bff7-c71cc10ceea3");

			String environmentId = "8a768ddf-dab0-4107-adc6-2ea547fc6011";
			String collectionId = "e3145d69-7271-4059-b32f-e22b7f15687b";
			

			// Query XML to pull URLs from Lenus
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse("/Users/rodalton/hse-covid.xml");
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			XPathExpression expr = xpath.compile(
					"/OAI-PMH/ListRecords/record/metadata/entry/triples/Description[description='ORIGINAL']/@about");

			NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);

			// For each URL, add PDF document to Discovery
			for (int i = 0; i < nl.getLength(); i++) {
				Node n = nl.item(i);
				
				String link = n.getNodeValue();
				
				//Sort out links with encoding
			    String url = java.net.URLDecoder.decode(link, StandardCharsets.UTF_8.name());
			    url = url.replaceAll(" ", "-");	
			    
			    //Get filename
			    String filename =  url.substring((url.lastIndexOf("/") + 1));
			    
			    try { 
					InputStream input = new URL(url).openStream();
					byte[] buff = new byte[8000];

					int bytesRead = 0;

					ByteArrayOutputStream bao = new ByteArrayOutputStream();

					while ((bytesRead = input.read(buff)) != -1) {
						bao.write(buff, 0, bytesRead);
					}

					byte[] data = bao.toByteArray();

					//Add document to Discovery
					InputStream documentStream = new ByteArrayInputStream(data);
					AddDocumentOptions.Builder docbuilder = new AddDocumentOptions.Builder(environmentId, collectionId);
					docbuilder.file(documentStream);
					docbuilder.filename(filename);
					docbuilder.metadata("{\"url\"" + ": \"" + url + "\"}");
					docbuilder.fileContentType(HttpMediaType.APPLICATION_PDF);
					DocumentAccepted response = discovery.addDocument(docbuilder.build()).execute().getResult();
					System.out.println("Document written to Discovery: " + filename);
			    }
			    catch (IOException ioe) { 
			    	ioe.printStackTrace();
			    }
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
