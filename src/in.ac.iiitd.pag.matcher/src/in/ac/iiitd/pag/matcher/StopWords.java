package in.ac.iiitd.pag.matcher;

import in.ac.iiitd.pag.structureextractor.StructureExtractor;
import in.ac.iiitd.pag.util.ASTUtil;
import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.LuceneUtil;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class StopWords {
	
	static HashMap<String, Integer> wordFreqs = new HashMap<String,Integer>();
	
	public static void main(String[] args) {
		Properties props = FileUtil.loadProps();
		if (props == null) return;
		
		String filePath = props.getProperty("MATCH_PARENT_FOLDER");
		
		//prepareStops(filePath);
		prepareStopsInSO();
		dumpStops();
	}

	private static void prepareStopsInSO() {
		Properties props = FileUtil.loadProps();
		if (props == null) return;
		
		//String filePath = props.getProperty("MATCH_PARENT_FOLDER");
				
		String luceneIndexFilePath = props.getProperty("INDEX_FILE_PATH");
		try {
			
			Version v = Version.LUCENE_48;
			Analyzer analyzer = new WhitespaceAnalyzer(v);
			Directory fsDir = FSDirectory.open(new File(luceneIndexFilePath));

			IndexReader reader = IndexReader.open(fsDir);
			
			for (int i = 0; i < reader.maxDoc(); i++) {
				Document d = reader.document(i);
				String method = d.get("code").toLowerCase();
				String[] snippet1Variables = ASTUtil.getVariableNames(method);
				for(String var: snippet1Variables) {
					int count = 1;
					if (wordFreqs.containsKey(var)) {
						count = wordFreqs.get(var) + 1;
					}
					wordFreqs.put(var,count);
					//System.out.println(var + "," + count);
				}
			}		

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
	}
	
	private static void dumpStops() {
		
		try {
			FileUtil.writeMapToFile(wordFreqs, "c:\\temp\\stops.txt",0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void prepareStops(String filePath) {
		
		try {
			processFiles(filePath);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void processFiles( String path ) throws IOException {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
            	processFiles( f.getAbsolutePath() );                
            }
            else {
                processFile(f);                
            }
        }
    }
	
	public static void processFile(File f) throws IOException {
		if (!f.getName().endsWith(".java")) return;
		
		String code = FileUtil.readFromFile(f.getAbsolutePath());
		Set<String> methods = grabMethods(code);
		//System.out.println( "File:" + f.getAbsoluteFile() );
		
		for(String method: methods) {	
			String[] snippet1Variables = ASTUtil.getVariableNames(method);
			for(String var: snippet1Variables) {
				int count = 1;
				if (wordFreqs.containsKey(var)) {
					count = wordFreqs.get(var) + 1;
				}
				wordFreqs.put(var,count);
				//System.out.println(var + "," + count);
			}
		}
	}
	
	private static Set<String> grabMethods(String code) {
		Set<String> methods = new HashSet<String>();
		methods.addAll(in.ac.iiitd.pag.util.ASTUtil.getMethods(code));		
		return methods;
	}
}
