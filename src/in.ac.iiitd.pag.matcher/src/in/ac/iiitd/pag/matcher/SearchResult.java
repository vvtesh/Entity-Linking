package in.ac.iiitd.pag.matcher;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;

public class SearchResult {
	public List<VocabularyEntity> vocabulary = new ArrayList<VocabularyEntity>();
	public List<Document> docs = new ArrayList<Document>();
	public List<Integer> complexities = new ArrayList<Integer>();
	public List<Integer> structuralElementsMatched = new ArrayList<Integer>();
	public String inputMethod = "";
	public String algo = "";	
	public int inputComplexity;
}
