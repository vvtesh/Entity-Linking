package in.ac.iiitd.pag.matcher;

import in.ac.iiitd.pag.util.ASTUtil;
import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.StringUtil;
import in.ac.iiitd.pag.util.StructureUtil;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;


/**
 * Given a code snippet, build structural signatures.
 * @author Venkatesh
 *
 */
public class StructuralSignatureBuilder {
	
	static String operatorsFile = "";
	static IndexWriter indexWriter = null;
	static int count = 0;
	
	public static void main(String[] args) {
		grab();
	}

	
	public static void grab() {
		Properties props = FileUtil.loadProps();
		if (props == null) return;
		
		String filePath = props.getProperty("MATCH_PARENT_FOLDER");
		operatorsFile = props.getProperty("OPERATORS_FILE");
		String luceneIndexFilePath = props.getProperty("MATCH_PARENT_FOLDER_INDEX_FILE_PATH");
				
		try {
			
			Version v = Version.LUCENE_4_8;
			Analyzer analyzer = new WhitespaceAnalyzer(v);
			Directory fsDir = FSDirectory.open(new File(luceneIndexFilePath));
			IndexWriterConfig iwConf 
		        = new IndexWriterConfig(v,analyzer);
		    iwConf.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		    indexWriter
		        = new IndexWriter(fsDir,iwConf);	        
	        
		    processFiles(filePath);
		    
	        indexWriter.close();
	        
		} catch (Exception e) {
			System.out.println(e.getMessage());
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
			try {
				List<String> algoElements = StructureUtil.getAlgo(method);
				List<String> flattenedAlgo = StructureUtil.flattenAlgo(algoElements);
				String methodName = StructureUtil.getMethodName(method);			
				String algo = StringUtil.getAsCSV(flattenedAlgo);
				algo = algo.replaceAll(",", " ");
				System.out.println(methodName + algo + method);	
				Document d = new Document();
				d.add(new StringField("methodname", methodName,
						Store.YES));
		        d.add(new TextField("algo",algo,
		                Store.YES));
		        d.add(new StringField("code", method,
	                    Store.YES));
		        //System.out.println(algo + "\n" + method);
		        indexWriter.addDocument(d);	   
		        System.out.println(++count + ". " + f.getName() + ":" + methodName);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private static Set<String> grabMethods(String code) {
		Set<String> methods = new HashSet<String>();
		methods.addAll(in.ac.iiitd.pag.util.ASTUtil.getMethods(code));		
		return methods;
	}
	
	public static String getMethodName(String codeFragment) {
		final List<String> methodName = new ArrayList<String>();
		ASTNode node = ASTUtil.getASTNode(codeFragment);
		node.accept(new ASTVisitor() {			
			public void endVisit(MethodDeclaration node) {
				methodName.add(node.getName().getFullyQualifiedName());				
			}

		});			
	    if (methodName.size() > 0)
	    	return methodName.get(0);
	    else
	    	return null;
	}
}
