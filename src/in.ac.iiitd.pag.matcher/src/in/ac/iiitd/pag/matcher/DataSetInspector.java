package in.ac.iiitd.pag.matcher;

import in.ac.iiitd.pag.structureextractor.StructureExtractor;
import in.ac.iiitd.pag.util.ASTUtil;
import in.ac.iiitd.pag.util.CodeFragmentInspector;
import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.StringUtil;
import in.ac.iiitd.pag.util.StructureUtil;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class DataSetInspector {
	
	static String luceneIndexFilePath = "";
	static int count = 0;
	static HashMap<String, Integer> map = new HashMap<String, Integer>();
	static HashMap<String, Integer> locMap = new HashMap<String, Integer>();
	static HashMap<Integer, Integer> structuralElementCountMap = new HashMap<Integer, Integer>();
	static List<String> items = new ArrayList<String>();
	
	public static void main(String[] args) {
		Properties props = FileUtil.loadProps();
		if (props == null) return;
		String operatorsFile = props.getProperty("OPERATORS_FILE");
		String filePath = props.getProperty("MATCH_PARENT_FOLDER2");		 
		List<String> operators = FileUtil.readFromFileAsList(operatorsFile);
		
		try {
			processFiles(filePath, operators);
			//printMethodSummary(map);
			//printStructuralSummary(structuralElementCountMap);
			FileUtil.writeListToFile(items, "c:\\temp\\output.txt");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private static void printStructuralSummary(
			HashMap<Integer, Integer> map2) {
		System.out.println("Structural Elements Count , Frequency in Methods");
		for(int key: map2.keySet()) {
			System.out.println( key + "," + map2.get(key));
		}	
	}

	private static void printMethodSummary(HashMap<String, Integer> map2) {
		System.out.println("Count, Method Names");
		for(String key: map2.keySet()) {
			System.out.println(map2.get(key) + "," + key);
		}		
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
                processFile(f, operators);                
            }
        }
    }
	public static void processFile(File f,List<String> operators) throws IOException {
		if (!f.getName().toLowerCase().endsWith(".java")) return;
		
		String code = FileUtil.readFromFile(f.getAbsolutePath());
		Set<String> methods = grabMethods(code);
		
		for(String method: methods) {	
			try {
				String methodName = StructureUtil.getMethodName(method);
				String algo = StructureExtractor.extract(method, operators);
				int structuralElementsCount = algo.split(" ").length;
				int loc = StringUtil.countLines(method);
				Set<String> snippet1Variables = ASTUtil.getDistinctVariableNames(method);
				String vocabulary = StringUtil.getAsCSV(snippet1Variables).replaceAll(","," ");
				
				String output = MessageFormat.format("{0}. {1},{2},{3},{4},{5}", ("" + (++count)).replace(",",""), f.getName(), methodName, structuralElementsCount, loc, vocabulary);
				items.add(output);
								
				addToStringMap(methodName, map);
				addtoIntMap(structuralElementsCount, structuralElementCountMap);
				
				
			}  catch (Exception e) {
				System.out.println(e.getMessage());
			}
		}
	}

	private static void addToStringMap(String data, HashMap<String,Integer> map) {
		int count = 0;
		if (map.containsKey(data)) {
			count = map.get(data);
		}
		count++;
		map.put(data, count);
	}
	
	private static void addtoIntMap(int data, HashMap<Integer,Integer> map) {
		int count = 0;
		if (map.containsKey(data)) {
			count = map.get(data);
		}
		count++;
		map.put(data, count);
	}
	
	private static Set<String> grabMethods(String code) {
		Set<String> methods = new HashSet<String>();
		methods.addAll(in.ac.iiitd.pag.util.ASTUtil.getMethods(code));		
		return methods;
	}
}
