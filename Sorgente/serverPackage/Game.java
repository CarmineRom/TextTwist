package serverPackage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import commonPackage.*;

public class Game
{
	String gameName;
	String letters;
	private int gameStatus;
	private int approves;
	private int numPlayersInvited;
	private ReentrantLock gameLock;
	private Condition statusChanged,getResultsReady;
	private String multicastIP;
	private ArrayList<Result> results;
	
	public Game(String gameName,int numPlayersInvited,String letters,String multicastIP)
	{
		this.gameName=gameName;
		this.gameStatus=0;
		this.approves=0;
		this.numPlayersInvited=numPlayersInvited;
		this.gameLock=new ReentrantLock(true);
		this.statusChanged=this.gameLock.newCondition();	
		this.getResultsReady=this.gameLock.newCondition();
		this.results=new ArrayList<Result>();	
		this.letters=letters;
		this.multicastIP=multicastIP;		
	}
	
	public ReentrantLock getLock()
	{
		return gameLock;
	}
	
	public Condition getStatusChanged()
	{
		return statusChanged;
	}
	
	public Condition getResultsReady()
	{
		return getResultsReady;
	}
	
	public String getLetters()
	{
		return letters;
	}
	
	public String getGameName()
	{
		return gameName;
	}
	
	public String getMulticastIP()
	{
		return multicastIP;
	}
	
	public ArrayList<Result> getResults()
	{
		return results;
	}
	public int getGameStatus()
	{
		this.gameLock.lock();
		int gS=this.gameStatus;
		this.gameLock.unlock();
		return gS;
	}
	
	public Boolean addPlayer()
	{
		this.gameLock.lock();
		approves++;
		Boolean check=approves==numPlayersInvited;
		if(check)
			gameStatus=1;
		this.gameLock.unlock();
		return check;
	}
	
	public Boolean addResult(String username,int score)
	{
		this.gameLock.lock();
		Result res=new Result(username, score);
		results.add(res);
		Boolean check=results.size()==numPlayersInvited;
		if(check)
		{
			results.sort(new Comparator<Result>()
			{
				public int compare(Result r1,Result r2)
				{
					return -(r1.score-r2.score);
				}
			});
		}
		
		this.gameLock.unlock();
		return check;
	}
	public void abort()
	{
		this.gameLock.lock();
		this.gameStatus=-1;
		this.gameLock.unlock();
	}
	
	public Boolean resultsReady()
	{
		this.gameLock.lock();
		Boolean res=(results.size()==numPlayersInvited);
		this.gameLock.unlock();
		return res;
	}
}
