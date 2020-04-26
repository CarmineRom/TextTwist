package clientPackage;

public class WordsList
{
	private String wordsList;
	
	public WordsList()
	{
		this.wordsList=new String();
	}
	
	public void AddWord(String word)
	{
		this.wordsList=this.wordsList+word+",";
	}
	
	public String getWordsList()
	{
		return this.wordsList;
	}
	
	public void clear()
	{
		this.wordsList="";
	}
}
