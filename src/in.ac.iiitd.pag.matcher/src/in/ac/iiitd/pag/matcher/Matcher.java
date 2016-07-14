package in.ac.iiitd.pag.matcher;

import in.ac.iiitd.pag.structureextractor.StructureExtractor;
import in.ac.iiitd.pag.util.ASTUtil;
import in.ac.iiitd.pag.util.CodeFragmentInspector;
import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.LuceneUtil;
import in.ac.iiitd.pag.util.MatchResult;
import in.ac.iiitd.pag.util.MatchResults;
import in.ac.iiitd.pag.util.StringUtil;
import in.ac.iiitd.pag.util.StructureUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.lucene.document.Document;

/**
 * Match methods in given code with repo of algorithms
 * @author Venkatesh
 *
 */
public class Matcher {
	
	
	static List<String> algosFromMatchInputFolder = new ArrayList<String>();
	static Map<String, String> methodSnippets = new HashMap<String,String>();
	static int count = 0;
	static String output = "";
	static String topicDistribution = "";
	static Set<String> allVocabItems = new HashSet<String>();
	static Set<String> addedMethods = new HashSet<String>();
	
	public static void main(String[] args) {
		long startTime = (new Date()).getTime();
		
		Properties props = FileUtil.loadProps();
		if (props == null) {
			props = FileUtil.loadProps(true);
		}
		if (props == null) return;
		String luceneIndexFilePath = props.getProperty("ALGO_REPO_INDEX_FILE_PATH");
		List<MatchResult> mrList =  LuceneUtil.getAllVariants(luceneIndexFilePath, "factorial");
		for(MatchResult mr: mrList) {
			System.out.println(mr.title);
		}
		/*
		String operatorsFile = props.getProperty("CANONICALIZED_OPERATORS_FILE"); 
		List<String> operators = FileUtil.readFromFileAsList(operatorsFile);		
		List<String> stops = FileUtil.readFromFileAsList("c:\\temp\\javastops.txt");		
		List<String> prefixStops = FileUtil.readFromFileAsList("c:\\temp\\prefixStops.txt");
		
		String filePath = props.getProperty("MATCH_PARENT_FOLDER2");
		String code1 = FileUtil.readFromFile("c:\\temp\\javaclassinput.java");
		//matchFromFolder(luceneIndexFilePath, filePath, operators, stops, prefixStops);
		MatchResults mr = match(luceneIndexFilePath, code1, operators, stops, prefixStops, 4, 10);
		
		System.out.println(mr.topicWiseResults.size());
		
		mr = match(luceneIndexFilePath, code1, operators, stops, prefixStops, 4, 10);
		
		System.out.println(mr.topicWiseResults.size());
		
		
		try {
			FileUtil.writeListToFile(allVocabItems, "c:\\temp\\matchervocab.txt");
			FileUtil.saveFile(new File("c:\\temp\\"), "matcheroutput1.txt", output, "");
			FileUtil.saveFile(new File("c:\\temp\\"), "matcheroutput2.txt", topicDistribution, "");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		*/
		long finishTime = (new Date()).getTime();
		System.out.println("Took " + (finishTime - startTime)/60000 + " minutes.");
	}
	
	public static String test() {
		return "test success";
	}
	
	public static String getOutput() {
		return output;
	}

	public static MatchResults match(String luceneIndexFilePath, String code, List<String> operators, List<String> stops, List<String> prefixStops, int minLoc, int maxLoc) {
		MatchResults mr = null;
		try {
				reset();
				extractMethods(code, operators);
				mr = processMethods(luceneIndexFilePath, stops, prefixStops, minLoc, maxLoc);				
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mr;
	}

	private static void reset() {
		algosFromMatchInputFolder = new ArrayList<String>();
		methodSnippets = new HashMap<String,String>();
		count = 0;
		output = "";
		topicDistribution = "";
		allVocabItems = new HashSet<String>();
		addedMethods = new HashSet<String>();		
	}

	public static MatchResults matchFromFolder(String luceneIndexFilePath, String filePath, List<String> operators, List<String> stops, List<String> prefixStops, int minLoc, int maxLoc) {
		MatchResults mr = null;
		
		//LuceneUtil.printAll(luceneIndexFilePath, "algo");
		try {
			reset();		
			processFiles(filePath, operators);
			
			//FileUtil.writeListToFile(algosFromMatchInputFolder, "c:\\temp\\algosFromEclipse.txt");
			//algosFromMatchInputFolder = FileUtil.readFromFileAsList("c:\\temp\\algosFromEclipse.txt");
			mr = processMethods(luceneIndexFilePath, stops, prefixStops, minLoc, maxLoc);
			//System.out.println(output);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//save algos
		return mr;
	}
	
	private static MatchResults processMethods(String luceneIndexFilePath, List<String> stops, List<String> prefixStops, int minLoc, int maxLoc) {
		MatchResults resultsMatched = new MatchResults();
		
		int found = 0;
		for(String row: algosFromMatchInputFolder) {
			//System.out.print(".");
			found++;
			String[] items = row.split(",");
			if (items.length < 3) continue;
			String methodName = items[1];
			String algo = items[2].toLowerCase();
			if (algo.split(" ").length < 3) continue;
			String method = methodSnippets.get(methodName + algo);
			resultsMatched.input = method;
			if (!((StringUtil.countLines(method) > minLoc) && (StringUtil.countLines(method) < maxLoc)))  continue;
					
			SearchResult results = SimpleLuceneSearch.search(algo, "algo", luceneIndexFilePath, methodSnippets.get(methodName + items[2]), 400, stops, prefixStops);
			/*if (result.trim().length() > 0) {
				if (!result.toLowerCase().contains("c#")) {
					//System.out.println(++found + ". " + methodName + ":" + result);
				}
			}*/
			String outputStr = "";
			outputStr += "\n=============Begin============\n";
			outputStr += "Input: (" + algo + ")\n";
			outputStr += "Complexity: " + results.inputComplexity + "\n";
			outputStr += methodSnippets.get(methodName + algo) + "\n";
			
			
			String topicDistributionStr = "";
			int resultCount = 0;
			if ((results.docs != null) && (results.docs.size() > 0)) {
				outputStr += "Output:\n";
				
				HashSet<String> topicsFound = new HashSet<String>();
				topicDistributionStr = methodName + ",";
				for(int i=0; i<results.docs.size(); i++) {
					int structuralMatchCount = results.structuralElementsMatched.get(i);
					Document result = results.docs.get(i);
					String code = result.get("code");
					int complexity = ASTUtil.getStructuralComplexity(code);
					
					if (complexity > Math.max(results.inputComplexity * 3, 50)) continue;
					if (complexity > 200) continue;
					resultCount++;
					VocabularyEntity vocab = results.vocabulary.get(i);
					topicDistributionStr += result.get("topic") + ",";
					topicsFound.add(result.get("topic"));
					
					MatchResult mr = new MatchResult();
					mr.snippet = code;
					mr.title = result.get("title");
					mr.postId = result.get("id");
					if (resultsMatched.topicWiseResults.containsKey(result.get("topic"))) {
						List<MatchResult> topicWiseResults = resultsMatched.topicWiseResults.get(result.get("topic"));
						topicWiseResults.add(mr);						
					} else {
						List<MatchResult> mrList = new ArrayList<MatchResult>();
						mrList.add(mr);
						resultsMatched.topicWiseResults.put(result.get("topic"), mrList);
					}
					
					outputStr += "Topic Found: ";
					outputStr += result.get("topic") + "(" + result.get("algo") + ")"+ "\n";
					outputStr += "\tPost id" + result.get("id") + ". SO Title: (" +  result.get("title")  + ")"+ "\n"; 
					outputStr += "\t Structures Matched = " + structuralMatchCount + "\n";
					for(String item: vocab.vocabulary) {
						allVocabItems.add(item);
					}
					outputStr += "\tVocabulary Stats: " + vocab.score + " " +  StringUtil.getAsCSV(vocab.vocabulary) + "\n";
										
					outputStr += "\tComplexity: " + complexity + "\n";	
					//outputStr += code + "\n";					
				}
				outputStr += "------------------------------\n"; 
				if ((resultCount > 0)&&(topicsFound.size() < 3)) {
					output += outputStr;
					topicDistribution += resultCount + "," + topicsFound.size() + "," + topicDistributionStr + "\n";					
				}
				topicDistributionStr = "";
				topicsFound.clear();				
			}
			
		}
		return resultsMatched;
	}

	public static void processFiles( String path, List<String> operators ) throws IOException {

        File root = new File( path );
        File[] list = root.listFiles();

        if (list == null) return;

        for ( File f : list ) {
            if ( f.isDirectory() ) {
            	processFiles( f.getAbsolutePath(), operators);                
            }
            else {
                processFile(f,operators);                
            }
        }
    }
	public static void processFile(File f, List<String> operators) throws IOException {
		if (!f.getName().toLowerCase().endsWith(".java")) return;
		
		String code = FileUtil.readFromFile(f.getAbsolutePath());
		extractMethods(code,operators);
	}

	private static void extractMethods(String code, List<String> operators) {
		Set<String> methods = grabMethods(code);
		//System.out.println( "File:" + f.getAbsoluteFile() );
		
		for(String method: methods) {	
			try {
				String methodName = StructureUtil.getMethodName(method).toLowerCase();
				String algo = StructureExtractor.extract(method, operators).toLowerCase();
				String row = ++count + "," + methodName + "," + algo;
				algosFromMatchInputFolder.add(row);
				if (!addedMethods.contains(method)) {
					addedMethods.add(method);
					methodSnippets.put(methodName + algo, method);
				}
				/*
				System.out.print(".");
				i++;
				if (i % 100 == 0) System.out.println("");
				//System.out.println("checking " + f.getName().replace(".java", "") + "." + methodName +  "[" + algo + "] ...");
				String result = SimpleLuceneSearch.search(algo, "algo", luceneIndexFilePath, 2);
				if (result.trim().length() > 0) {
					System.out.println(methodName + "\n" + method + "\n\n");
				}*/
			}  catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}
	
	private static Set<String> grabMethods(String code) {
		Set<String> methods = new HashSet<String>();
		methods.addAll(in.ac.iiitd.pag.util.ASTUtil.getMethods(code));		
		return methods;
	}
	
	
}
