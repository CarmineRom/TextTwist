package clientPackage;

import java.io.BufferedReader;

public class WordsReader implements Runnable
{
	BufferedReader localInput;	
	WordsList wordsList;
	
	public WordsReader(BufferedReader localInput,WordsList wordsList)
	{
		this.localInput=localInput;
		this.wordsList=wordsList;
	}
	
	public void run()
	{
		
		try
		{
			String temp;
			System.out.println("Word:");
			while(!Thread.currentThread().isInterrupted())
			{
				temp=localInput.readLine();
				if(!Thread.currentThread().isInterrupted())
				{
					wordsList.AddWord(temp);
					System.out.println("WORDS FOUND: "+wordsList.getWordsList());					
					System.out.println("Next Word:");
				}
			}
		}
		catch (Exception e)
		{			
		}		
	}	
}
