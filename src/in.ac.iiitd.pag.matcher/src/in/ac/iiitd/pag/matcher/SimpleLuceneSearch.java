package in.ac.iiitd.pag.matcher;

import in.ac.iiitd.pag.structuralsearch.CodeSnippet;
import in.ac.iiitd.pag.util.ASTUtil;
import in.ac.iiitd.pag.util.FileUtil;
import in.ac.iiitd.pag.util.LuceneUtil;
import in.ac.iiitd.pag.util.NGramBuilder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

/**
 * Given index path and query, return <= maxResults.
 * @author Venkatesh
 *
 */
public class SimpleLuceneSearch {
	
	public static void main(String[] args) {
		Properties props = FileUtil.loadProps();
		if (props == null) return;

		//String luceneIndexFilePath = props.getProperty("INDEX_FILE_PATH");
		String luceneIndexFilePath = props.getProperty("ALGO_REPO_INDEX_FILE_PATH");
		
		LuceneUtil.checkIndex(luceneIndexFilePath);
		//LuceneUtil.printAll(luceneIndexFilePath, "algo");
		List<String> stops = FileUtil.readFromFileAsList("c:\\temp\\javastops.txt");
		
		List<String> prefixStops = FileUtil.readFromFileAsList("c:\\temp\\prefixStops.txt");
		
		/*String field = "algo";
		String queryString = "string trafficlightsimulator trafficlightsimulator loop< loop+ loopmethod loopmethod method";
		*/
		String field = "topic";
		String queryString = "factorial";
		int maxResults = 500;
		System.out.println("Searching...");
		String methodInput = FileUtil.readFromFile("c:\\temp\\method3.txt");
		
		System.out.println(methodInput);
		search(queryString, field, luceneIndexFilePath, methodInput, maxResults, stops, prefixStops);
	}

	private static SearchResult processQueries(List<Query> finalQueries, IndexSearcher searcher, int maxResults, String methodInput, String queryString, List<String> stops, List<String> prefixStops) {
		
					
			List<Document> docs = new ArrayList<Document>();
		 	int queryCount = finalQueries.size();
	        boolean stopQuerying = false;
	        boolean resultObtained = false;
	        
	        SearchResult searchResult = new SearchResult();
			searchResult.inputMethod = methodInput;
			searchResult.algo = queryString;
			int inputComplexity = ASTUtil.getStructuralComplexity(methodInput);
			searchResult.inputComplexity = inputComplexity;
	        
	        try {
		        for(int j=0; j<queryCount; j++) {
		        	if (stopQuerying) break;
		        	Query finalQuery = finalQueries.get(j);
			        TopDocs hits = searcher.search(finalQuery,maxResults);
			        
			        ScoreDoc[] scoreDocs = hits.scoreDocs;
			        
			        int resultCount = 0;
			        int minVal = maxResults;
			        if (scoreDocs.length < maxResults) minVal = scoreDocs.length;
			        
			        for (int n = 0; n < minVal; ++n) {
			            ScoreDoc sd = scoreDocs[n];
			            float score = sd.score;
			            int docId = sd.doc;
			            Document d = searcher.doc(docId);	            
			            
			            String algo = d.get("algo");
			            String topic = d.get("topic");
			            String title = d.get("title");
			            String method = d.get("code");
			           if (method.contains("factorial")) {
			        	  // System.out.println(method); 
			           }
			           // System.out.println(title);
			           // System.out.println(method); 
			            VocabularyEntity scoreV = getVocabularyScore(methodInput, method, stops, prefixStops);
			            
			            if (scoreV.score > 0) {
			            	int complexity = ASTUtil.getStructuralComplexity(method);
			            	searchResult.complexities.add(complexity);
			            	searchResult.vocabulary.add(scoreV);
			            	searchResult.structuralElementsMatched.add(j);
			            	docs.add(d);
			            	//System.out.println(score + ". " + title + ":" +  algo + "\n" + method);
			            	stopQuerying = true;
			            	resultObtained = true;
			            }
			                       
			        }		        
		        } 
	        } catch (IOException ioException) {
	        	System.out.println(ioException.getMessage());
	        }
		        
	        searchResult.docs = docs;
	        return searchResult;
	}
	
	public static SearchResult search(String queryString, String field,
			String luceneIndexFilePath, String methodInput, int maxResults, List<String> stops, List<String> prefixStops) {
		
		//System.out.println("Searching in " + luceneIndexFilePath);
		
		
		int resultCount = 0;
		String result = "";
		
		SearchResult searchResult = new SearchResult();
		searchResult.inputMethod = methodInput;
		searchResult.algo = queryString;
		int inputComplexity = ASTUtil.getStructuralComplexity(methodInput);
		searchResult.inputComplexity = inputComplexity;
		if (inputComplexity > 200) return searchResult;
		try {
			
			
			Version v = Version.LUCENE_48;
			Analyzer analyzer = new WhitespaceAnalyzer(v);
			Directory fsDir = FSDirectory.open(new File(luceneIndexFilePath));

			IndexReader reader = IndexReader.open(fsDir);
			//System.out.println("The index " + luceneIndexFilePath + " has " + reader.maxDoc() + " documents.");
	        IndexSearcher searcher = new IndexSearcher(reader);
	        
	        QueryParser parser 
	            = new QueryParser(field,analyzer);

	        //Query finalQuery = getSpanNearQuery(queryString.toLowerCase(), field, maxResults);
	        
	        List<Query> finalQueries = LuceneUtil.getComplexQuery(queryString.toLowerCase(), field, true);
	        
	        SearchResult searchResult1 = processQueries(finalQueries, searcher, maxResults, methodInput, queryString, stops, prefixStops); 
	        searchResult = searchResult1;
	        
	       /* if (searchResult.docs.size() == 0) {
	        	List<Query> revisedQueries = LuceneUtil.getComplexQuery(queryString.toLowerCase(), field, false);
	        	SearchResult searchResult2 = processQueries(revisedQueries, searcher, maxResults, methodInput, queryString);
	        	//searchResult2.structuralElementsMatched.clear();
	        	searchResult2.structuralElementsMatched.add(500);
	        	if (searchResult2.docs.size() > 0) {
	        		HashSet<String> set = new HashSet<String>();
	        		for(Document doc: searchResult2.docs) {
	        			set.add(doc.get("topic"));
	        			System.out.println(doc.get("topic"));
	        		}
	        		if (set.size() < 4) {
	        			searchResult1 = searchResult2;
	        		}
	        	}
	        }*/
	        searchResult = searchResult1;
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
				
		return searchResult;
	}
	
	private static VocabularyEntity getVocabularyScore(String snippet1, String snippet2, List<String> stops, List<String> prefixStops) {
		VocabularyEntity vocab = new VocabularyEntity();
		float score = 0;
		int count = 0;
		String commonVocab = "";
		String[] snippet1Variables = ASTUtil.getVariableNames(snippet1);
		String[] snippet2Variables = ASTUtil.getVariableNames(snippet2);
		for(String variable1: snippet1Variables) {			
			String cleanedVar1 = cleanVariable(variable1, prefixStops);
			if (stops.contains(cleanedVar1.toLowerCase())) continue;			
			if (cleanedVar1.length() < 4) continue;	
			for (String variable2: snippet2Variables) {				
				
				String cleanedVar2 = cleanVariable(variable2, prefixStops);
				if (stops.contains(cleanedVar2.toLowerCase())) continue;			
				if (cleanedVar2.length() < 4) continue;	
				if (cleanedVar1.contains(cleanedVar2) || cleanedVar2.contains(cleanedVar1)) {
					count++;
					vocab.vocabulary.add(cleanedVar1);					
					break;
				}
			}
		}
		score = count * 1.0f/Math.max(snippet1Variables.length, snippet2Variables.length);
		vocab.score = score;
		return vocab;
	}
	
	private static String cleanVariable(String variable1, List<String> prefixStops) {
		String result = variable1;
		for(String prefix: prefixStops) {
			if (variable1.startsWith(prefix)) {
				result = variable1.substring(prefix.length());
			}
		}
		return result;
	}

	private static Query getSpanNearQuery(String queryString, String searchIn, int maxResults)  {
		
		String[] terms = queryString.split(" ");
		SpanQuery[] spanQueries = new SpanQuery[terms.length];
		for(int i=0; i<terms.length; i++) {
			spanQueries[i] = new SpanTermQuery(new Term(searchIn, terms[i]));
		}
		
		SpanQuery query = new SpanNearQuery(spanQueries,
				  100,
				  true);		
		
		return query;
	}
}
