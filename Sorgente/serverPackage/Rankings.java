package serverPackage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.locks.ReentrantLock;

import commonPackage.*;

public class Rankings
{
	private ArrayList<Result> rankings;
	private File rankingsFile;
	private ReentrantLock rankingsLock;
	
	public Rankings(ArrayList<Result> rankings,File rankingsFile)
	{
		this.rankingsFile=rankingsFile;
		this.rankings=rankings;
		rankingsLock=new ReentrantLock();
	}
	
	public ReentrantLock getLock()
	{
		return rankingsLock;
	}
	
	public void addResults(ArrayList<Result> results)
	{
		rankingsLock.lock();
		//AAGGIORNAMENTO CLASSIFICA GENERALE
		for(Result r: results)
		{
			Boolean found=false;
			int index=0;
			while(index<rankings.size() && !found)
			{
				if(rankings.get(index).username.equals(r.username))
					found=true;
				else
					index++;
			}
			if(!found)
				rankings.add(r);
			else				
				rankings.get(index).score=rankings.get(index).score+r.score;			
		}
		
		rankings.sort(new Comparator<Result>()
		{
			public int compare(Result r1,Result r2)
			{
				return -(r1.score-r2.score);
			}
		});
		
		//SALVATAGGIO CLASSIFICA GENERALE AGGIORNATA SU FILE
		try
		{
			ObjectOutputStream fileOut=new ObjectOutputStream(new FileOutputStream(rankingsFile));
			fileOut.writeObject(rankings);
			fileOut.close();
		}
		catch (IOException e)
		{
			System.out.println("Error while wiritng rankingsFile");
		}
		rankingsLock.unlock();
	}
	
	public ArrayList<Result> getRankings()
	{
		rankingsLock.lock();
		ArrayList<Result> r=this.rankings;
		rankingsLock.unlock();
		return r;
	}
}









