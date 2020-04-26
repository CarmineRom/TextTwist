package serverPackage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import javax.security.auth.callback.Callback;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import commonPackage.*;

public class ClientHandler implements Runnable
{
	protected String serverAddress;
	protected int multiCastPort;
	private Socket clientSocket;	
	private ObjectOutputStream outputStream;
	private ObjectInputStream inputStream;
	ConcurrentHashMap<String, String> registeredMulticast;
	ConcurrentHashMap<String, ClientRmiIF> onlineUsers;
	ConcurrentHashMap<String, Game> gamesList;		
	ArrayList<String> dictionary;
	Rankings rankings;
	File rankingsFile;
	
	public ClientHandler(Socket clientSocket,ConcurrentHashMap<String, ClientRmiIF> onlineUsers,ConcurrentHashMap<String, String> registeredMulticast,Rankings rankings,File rankingsFile,String serverAddress,int multiCastPort,ConcurrentHashMap<String, Game> gamesList,	
	ArrayList<String> dictionary)
	{
		this.clientSocket=clientSocket;
		this.registeredMulticast=registeredMulticast;
		this.rankings=rankings;
		this.rankingsFile=rankingsFile;
		this.multiCastPort=multiCastPort;
		this.serverAddress=serverAddress;
		this.onlineUsers=onlineUsers;
		this.gamesList=gamesList;
		this.dictionary=dictionary;
	}
	
	private final String generateLetters()
	{
		char[] vowels={'a','e','i','o','u'};
		char[] consonants ={'b','c','d','f','g','h','j','k','l','m','n','p','q','r','s','t','v','w','x','y','z'};
		
		String letters=new String();
		Random random=new Random();
		
		for(int i=0;i<5;i++)
			letters=letters+(char) vowels[random.nextInt(5)];
		
		for(int i=0;i<7;i++)
			letters=letters+(char) consonants[random.nextInt(21)];
			
		return letters;
	}
	
	private final String generateIP()
	{
		Random random=new Random();
		int a=random.nextInt(255);
		int b=random.nextInt(255);
		String IP="239.255."+a+"."+b;
		return IP;
	}
	
	//SEZIONE 2.2 DELLA RELAZIONE -Richiesta nuova partita
	private void createGameRequest() throws IOException,ClassNotFoundException
	{
		String gameName;
		String gameCreator;
		JSONArray playersList=new JSONArray();
		
		gameCreator=(String)inputStream.readObject();
		gameName=(String)inputStream.readObject();				
		playersList=(JSONArray)inputStream.readObject();
		
		boolean notAllOnline=false;
		outputStream=new ObjectOutputStream(clientSocket.getOutputStream());
		JSONArray offlinePlayers=new JSONArray();
		ArrayList<ClientRmiIF> onlinePlayers=new ArrayList<ClientRmiIF>();
		onlinePlayers.add(onlineUsers.get(gameCreator));
		
		for(Object o: playersList)
		{
			JSONObject jo=(JSONObject) o;
			String name=(String)jo.get("name");
			if(!onlineUsers.containsKey(jo.get("name")))
			{
				offlinePlayers.add(jo);
				notAllOnline=true;
			}
			else
			{
				try
				{	
					ClientRmiIF cl=onlineUsers.get(jo.get("name"));
					cl.isOnline();
					onlinePlayers.add(cl);					
				}
				catch (Exception e)
				{				
					offlinePlayers.add(jo);
					notAllOnline=true;
					onlineUsers.remove(jo.get("name"));					
				}			
			}				
		}
		
		outputStream.writeBoolean(notAllOnline);
		outputStream.flush();				
		if(notAllOnline)
		{
			outputStream.writeObject(offlinePlayers);
			outputStream.flush();
			clientSocket.close();
		}
		else
		{
			clientSocket.close();
			gameName=gameName+" by "+gameCreator;
			String letters=generateLetters();			
			String multicastIP=generateIP();
			
			while(registeredMulticast.contains(multicastIP))
				multicastIP=generateIP();
			registeredMulticast.put(gameName, multicastIP);			
			
			Game game=new Game(gameName,onlinePlayers.size(),letters,multicastIP);			
			gamesList.put(gameName, game);			
			
			System.out.println("New game '"+gameName+"' created");
			//INVITO GIOCATORI
			for(ClientRmiIF callback : onlinePlayers)
			{
				try
				{
					callback.invite(gameName,gameCreator);					
				}
				catch (RemoteException e) 
				{										
				}
			}
			
			game.getLock().lock();
			
			try
			{
				Boolean signalORtime=false;
				while(game.getGameStatus()==0 && !signalORtime)
				{
					game.getStatusChanged().await(7, TimeUnit.MINUTES); //Attesa di 7 Minuti					
					signalORtime=true;
				}
			}
			catch (InterruptedException e)
			{
			}					
			
			if(game.getGameStatus()!=1)
			{
				System.out.println("Cancelling game '"+gameName+"'");
				game.abort();
				game.getStatusChanged().signalAll();
				if(!game.getLock().hasQueuedThreads())
				{					
					game.getLock().unlock();
					gamesList.remove(gameName);
				}
				else 
					game.getLock().unlock();
			}
			else
			{			
				MulticastSocket multicastSocket=new MulticastSocket(multiCastPort);
				InetAddress multicastGroup=InetAddress.getByName(multicastIP);				
				try
				{
					System.out.println("Game '"+gameName+"' started");
					while(!game.resultsReady())
						game.getResultsReady().await();
				}
				catch (Exception e)
				{				
				}
				
				//INVIO DEI RISULTATI IN MULTICAST
				ArrayList<Result> results=game.getResults();
				game.getLock().unlock();
				gamesList.remove(gameName);
						
				rankings.getLock().lock();
				rankings.addResults(results);
				rankings.getLock().unlock();				
				
				ByteArrayOutputStream byte_stream=new ByteArrayOutputStream();
				ObjectOutputStream outputStream=new ObjectOutputStream(byte_stream);
				outputStream.writeObject(results);
				byte[] byteResults=byte_stream.toByteArray();
				DatagramPacket packet=new DatagramPacket(byteResults,byteResults.length,multicastGroup,multiCastPort);
				multicastSocket.send(packet);
				multicastSocket.close();
				registeredMulticast.remove(gameName,multicastIP);				
				//Aggiornamento classifica generale e scrittura su file
			}					
		}		
	}
	
	//SEZIONE 2.2 DELLA RELAZIONE -Setup nuova partita
	private void inviteAnswer() throws IOException,ClassNotFoundException,InterruptedException
	{	
		String gameName;
		String username;
		Boolean gameAccepted;
		outputStream=new ObjectOutputStream(clientSocket.getOutputStream());
		username=(String)inputStream.readObject();
		gameName=(String)inputStream.readObject();
		gameAccepted=inputStream.readBoolean();
		//RICEZIONE RISPOSTA INVITO				
		
		if(!gamesList.containsKey(gameName))
		{
			outputStream.writeBoolean(true);
			outputStream.flush();
			clientSocket.close();
		}
		else
		{
			Game game=gamesList.get(gameName);
			game.getLock().lock();
			
			if(!gameAccepted)
			{
				clientSocket.close();			
				game.abort();
				game.getStatusChanged().signalAll();
				if(!game.getLock().hasQueuedThreads())
				{
					game.getLock().unlock();
					gamesList.remove(gameName);
				}
				else
					game.getLock().unlock();
			}
			else
			{
				if(game.getGameStatus()==-1)
				{
					outputStream.writeBoolean(true);
					outputStream.flush();
					clientSocket.close();
					System.out.println("Game '"+gameName+"' canceled");
					if(!game.getLock().hasQueuedThreads())
					{
						game.getLock().unlock();
						gamesList.remove(gameName);
					}
					else
						game.getLock().unlock();
				}
				else
				{
					outputStream.writeBoolean(false);	
					outputStream.flush();
					startGame(game,username);		
				}
			}
		}				
	}
	
	//SEZIONE 2.2 DELLA RELAZIONE -Avvio nuova partita
	private void startGame(Game game,String username) throws IOException,InterruptedException
	{
		//Modalità attesa
		Boolean go;
		String gameName=game.getGameName();
		String letters=game.getLetters();
		String multicastIP=game.getMulticastIP();
		go=game.addPlayer();
		if(go)
			game.getStatusChanged().signalAll();
		
		while(game.getGameStatus()==0)
			game.getStatusChanged().await();
		
		if(game.getGameStatus()==-1)
		{
			outputStream.writeBoolean(false);
			outputStream.flush();
			clientSocket.close();
			if(!game.getLock().hasQueuedThreads())
			{
				game.getLock().unlock();
				gamesList.remove(gameName);
			}
			else
				game.getLock().unlock();
		}
		else
		{			
			game.getLock().unlock();
			outputStream.writeBoolean(true);
			outputStream.flush();			
			DatagramSocket udpSocket=new DatagramSocket();
			SocketAddress udpSocketAddress=new InetSocketAddress(serverAddress, udpSocket.getLocalPort());
			udpSocket.setSoTimeout(300000); // Attesa di 5 MINUTI per la ricezione delle parole
			
			DatagramPacket words=new DatagramPacket(new byte[512], 512);
			
			//INVIO LETTERE E INDIRIZZI UDP
			outputStream.writeObject(letters);
			outputStream.flush();
			outputStream.writeObject(udpSocketAddress);
			outputStream.flush();
			outputStream.writeObject(multicastIP);
			outputStream.flush();
			clientSocket.close();	
			
			Boolean socketTimeOut=false;
			try
			{									
				udpSocket.receive(words);
			}
			catch (Exception e)
			{
				socketTimeOut=true;
			}		
			udpSocket.close();
			
			//CALCOLO RISULTATI			
			String noSplittedList=new String(words.getData(),0,words.getLength());
			String[] wordsList=noSplittedList.split(",");
			ArrayList<String> checkedWords=new ArrayList<String>();			
			int score=0;
			for(String s: wordsList)
			{
				char[] stringChars=s.toCharArray();
				int i=0;
				Boolean badWord=checkedWords.contains(s);
				checkedWords.add(s);
				while(i<stringChars.length && !badWord)
				{
					if(letters.indexOf(stringChars[i])<0)
						badWord=true;
					i++;
				}
				
				if(!badWord)
					if(dictionary.contains(s))						
						score=score+s.length();					
			}	
			
			
			if(socketTimeOut)
			{
				Thread.sleep(5000);
				score= -1;
			}
			game.getLock().lock();
			Boolean resultsReady=game.addResult(username, score);
			if(resultsReady)
				game.getResultsReady().signal();
			game.getLock().unlock();		
		}		
	}
	
	//SEZIONE 2.2 DELLA RELAZIONE -Visualizzazione classifica generale
	private void getRankings() throws IOException
	{
		outputStream=new ObjectOutputStream(clientSocket.getOutputStream());
		rankings.getLock().lock();
		ArrayList<Result> results=rankings.getRankings();
		rankings.getLock().unlock();		
		
		JSONArray JSONresults=new JSONArray();
		for(Result r: results)
		{
			JSONObject res=new JSONObject();
			res.put("username",r.username);
			res.put("score", r.score);
			
			JSONresults.add(res);		
		}
		outputStream.writeObject(JSONresults);
		outputStream.flush();
		clientSocket.close();		
	}
	
	//SEZIONE 2.2 DELLA RELAZIONE -Classe ClientHandler
	public void run()
	{
		System.out.println("Handling Client");
		String operation;		
		try
		{
			inputStream=new ObjectInputStream(clientSocket.getInputStream());
			operation=(String)inputStream.readObject();
			if(operation.equals("1"))
			{
				createGameRequest();
			}
			else if(operation.equals("2"))
			{
				inviteAnswer();
			}
			else
			{
				getRankings();
			}
		}
		catch (Exception e)
		{
			System.out.println("Error while getting the inputStream");
			e.printStackTrace();
		}	
	}
}















