package in.ac.iiitd.pag.oracle;

import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.StringUtil;
import in.ac.iiitd.pag.util.XMLUtil;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * Let's pick up some n irrelevant posts semi-autmatedly. 
 * 
 * @author Venkatesh
 *
 */
public class IrrelevantPostsExtractor {
	
	static int POST_COUNT = 250;
	
	public static void main(String[] args) {
		Properties props = FileUtil.loadProps();
		if (props == null) return;
		
		String filePath = props.getProperty("FILE_PATH");
		String bodyTokenFilePath = props.getProperty("BODY_TOKEN_FILE_PATH");
		BufferedWriter bw = null;
		try {
			FileWriter fw = new FileWriter(bodyTokenFilePath);
			bw = new BufferedWriter(fw);
			processFile(filePath,bw);
			bw.close();
			fw.close();
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		
	}

	private static void processFile(String filePath, BufferedWriter bw) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath), 2 * 1024 * 1024);
			String line = null;			
			int lineCount = 0;
			System.out.println("Reading file...");
			while ((line = reader.readLine()) != null) {
				lineCount++;
				if (lineCount % 100 == 0) System.out.print(".");
				if (lineCount % 5000 == 0) {System.out.println(lineCount);}
				try {
					if (!line.trim().startsWith("<row")) continue;
					
					XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();        
			        XMLEventReader xmlEventReader = xmlInputFactory.createXMLEventReader(new StringReader(line));        
			        while(xmlEventReader.hasNext()){
			           XMLEvent xmlEvent = xmlEventReader.nextEvent();
			           if (xmlEvent.isStartElement()){
			               StartElement startElement = xmlEvent.asStartElement();
			               if(startElement.getName().getLocalPart().equalsIgnoreCase("row")){			            	   
		                	   int id = XMLUtil.getIntElement(startElement, "Id");
		                	   String body = XMLUtil.getStringElement(startElement, "Body");
		                	   String title = XMLUtil.getStringElement(startElement, "Title");
		                	   processbody(id, body + " " + title, bw);
			               }
			           }
			        }
				} catch (Exception e) {
					System.out.println(e.getMessage());
				}
			}
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
	}

	private static void processbody(int id, String body, BufferedWriter bw) throws IOException {
		Set<String> tokens = StringUtil.getTokens(body);
		String csv = StringUtil.getAsCSV(tokens);
		csv = id + "," + csv;
		bw.write(csv + "\n");
	}
}
