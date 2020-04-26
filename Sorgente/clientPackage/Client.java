package clientPackage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import commonPackage.*;

public class Client extends UnicastRemoteObject implements ClientRmiIF
{
	private static final long serialVersionUID = 1L;
	protected static String serverAddress;
	protected static String hostname;
	protected static String rmiName;
	protected static int tcpPort;
	protected static int rmiPort;
	protected static int multiCastPort;	
	private static String username;
	private static String password;
	private static AtomicBoolean busy;
	private static ArrayList<String> invites;
	private static ReentrantLock invitesLock;
	static BufferedReader localInput;
	static ServerRmiIF serverRmi;
	static Socket tcpSocket = new Socket();
	static ObjectOutputStream outputStream;
	static ObjectInputStream inputStream;
	static File configFile;	
	
	public Client() throws RemoteException
	{	
	}
	
	public boolean invite(String gameName,String gameCreator)
	{
		if(!busy.get())
			System.out.println("Invite to the game "+gameName+" received from "+gameCreator);		
		
		invitesLock.lock();
		invites.add(gameName);
		invitesLock.unlock();
		return true;		
	}	
	
	public void isOnline()
	{
	}
	
	//SEZIONE 2.1 DELLA RELAZIONE
	public static void mainMenu() throws IOException,ClassNotFoundException
	{		
		boolean exit=false;	
		invites=new ArrayList<String>();
		String choice;
		ArrayList<String> goodchoices=new ArrayList<>();
		goodchoices.add("1");
		goodchoices.add("2");
		goodchoices.add("3");
		goodchoices.add("4");
		
		while(!exit)
		{
			System.out.println("");
			System.out.println("$$$$$$$$$$$ TEXT_TWIST $$$$$$$$$$$");
			System.out.println("1. Create Game");
			System.out.println("2. Invites");
			System.out.println("3. Ranking");
			System.out.println("4. Exit");
			
			choice=localInput.readLine();			
			
			while(!goodchoices.contains(choice))
			{
				System.out.println("Wrong selection. Retry.");
				choice=localInput.readLine();
			}
			
			if(choice.equals("1"))
			{
				createGame();
			}
			else if (choice.equals("2"))
			{
				invitesMenu();
			}
			else if (choice.equals("3"))
			{
				getRankings();
			}
			else if (choice.equals("4"))
			{
				Boolean logOut=exit=serverRmi.logOut(username);
				System.out.println("Logged Out. Bye!");
				exit=logOut;
			}
		}
	}
	
	//SEZIONE 2.1 DELLA RELAZIONE -Richiesta nuova partita 
	public static void createGame() throws IOException
	{
		busy.getAndSet(true);
		JSONArray playersList=new JSONArray();
		String playerName;
		int numPlayers=1;
		tcpSocket = new Socket();
		
		System.out.println("_____GAME CREATOR_____");
		System.out.println("Insert the name of game:");
		String gameName=localInput.readLine();					
		System.out.println("Insert the username of the players you want to challenge then leave blank and press enter when done.");
		System.out.println("Player "+(numPlayers)+":");
		playerName=localInput.readLine();
		Boolean listEmpty=true;
		while(!playerName.equals("") || listEmpty)
		{	
			if(playerName.equals(username))
				System.out.println("Error! You can't invite yourself");
			else if(playerName.equals(""))
				System.out.println("Insert at least 1 player.");
			else
			{
				JSONObject player=new JSONObject();
				player.put("name", playerName);
				playersList.add(player);
				numPlayers++;
				listEmpty=false;
			}
			System.out.println("Player "+(numPlayers)+":");
			playerName=localInput.readLine();
		}
		
		System.out.println("Registering the game on the server...");
		tcpSocket.connect(new InetSocketAddress(serverAddress,tcpPort));
		outputStream=new ObjectOutputStream(tcpSocket.getOutputStream());						
		outputStream.writeObject("1");
		outputStream.flush();
		outputStream.writeObject(username);
		outputStream.flush();
		outputStream.writeObject(gameName);
		outputStream.flush();					
		outputStream.writeObject(playersList);
		outputStream.flush();
		
		inputStream=new ObjectInputStream(tcpSocket.getInputStream());
		if(!(boolean) inputStream.readBoolean())
		{
			System.out.println("Game registered and invites sent.");
		}
		else 
		{
			try
			{
				JSONArray offlineUsers;
				offlineUsers=(JSONArray) inputStream.readObject();
				System.out.println("Error! The game has been canceled because these players are offline:");
				for(Object o: offlineUsers)
				{
					JSONObject jo=(JSONObject) o;
					System.out.println("- "+jo.get("name"));
				}
			}
			catch (ClassNotFoundException e)
			{
				System.out.println("Game canceled.Error while receiving offline users list.");
			}
		}
		tcpSocket.close();
		busy.getAndSet(false);
	}
	
	//SEZIONE 2.1 DELLA RELAZIONE -Setup nuova partita
	public static void invitesMenu() throws IOException
	{
		System.out.println("");
		System.out.println("_____INVITES MENU_____");
		invitesLock.lock();
		if(invites.isEmpty())
		{
			invitesLock.unlock();
			System.out.println("");
			System.out.println("There are no invites");
		}
		else
		{
			int gamechoosed=0;
			boolean backToMenu=false;
			int j;
			while(!backToMenu)
			{
				j=0;
				System.out.println("");
				System.out.println("0. BACK TO MAIN MENU");
				for(String s: invites)
				{
					j++;
					System.out.println(j+". "+s);								
				}
				System.out.println("Insert the number of the game:");
				gamechoosed=Integer.parseInt(localInput.readLine());
				while(gamechoosed<0 || gamechoosed>j)
				{				
					System.out.println("Wrong selection. Retry");
					gamechoosed=Integer.parseInt(localInput.readLine());	
				}
				
				if(gamechoosed==0)
				{
					invitesLock.unlock();
					backToMenu=true;
				}
				else
				{				
					busy.getAndSet(true);
					String gameName=invites.get(gamechoosed-1);
					System.out.println("Game choosed: "+gameName);
					
					System.out.println("Accept? (y/n)");
					String accepted=localInput.readLine();
					
					tcpSocket = new Socket();
					tcpSocket.connect(new InetSocketAddress(serverAddress,tcpPort));
					outputStream=new ObjectOutputStream(tcpSocket.getOutputStream());
					outputStream.writeObject("2");
					outputStream.flush();
					outputStream.writeObject(username);
					outputStream.flush();
					outputStream.writeObject(gameName);
					outputStream.flush();					
					outputStream.writeBoolean(accepted.equals("y"));
					outputStream.flush();
					inputStream=new ObjectInputStream(tcpSocket.getInputStream());
					if(!accepted.equals("y"))
					{
						tcpSocket.close();
						System.out.println("Game refused, choose an other game.");
						invites.remove(gamechoosed-1);
						busy.getAndSet(false);
					}
					else
					{
						Boolean canceled=inputStream.readBoolean();
						if(canceled)
						{
							tcpSocket.close();
							System.out.println("Game canceled by server, choose an other game.");
							invites.remove(gamechoosed-1);
							busy.getAndSet(false);
						}
						else
						{							
							invites.clear();
							invitesLock.unlock();
							try
							{
								backToMenu=true;
								startGame();
							}
							catch (ClassNotFoundException e)
							{
								System.out.println("Error during game");
							}
						}
					}					
				}
			}	
		}
	}
	
	//SEZIONE 2.1 DELLA RELAZIONE -Avvio nuova partita
	public static void startGame() throws IOException,ClassNotFoundException
	{
		System.out.println("Game accessed. Waiting for players...");
		Boolean gameReady=inputStream.readBoolean();
		if(!gameReady)
		{
			System.out.println("Game aborted because not all players accepted.");
			tcpSocket.close();
		}
		else
		{
			WordsList wordsList=new WordsList();
			DatagramSocket udpSocket=new DatagramSocket();
			Boolean serverTimeOut=false;
			Boolean confirmTimeOut=false;
			ConfirmWaiter confirmWaiter=new ConfirmWaiter(localInput);
			Thread confirmWaiterThread=new Thread(confirmWaiter);
			
			String noSplittedLetters=(String) inputStream.readObject();
			SocketAddress udpSocketAddress=(SocketAddress) inputStream.readObject();
			String multicastIP=(String)inputStream.readObject();
			tcpSocket.close();		
			
			confirmWaiterThread.start();			
			try
			{	
				confirmWaiterThread.join(180000); // Timer iniziale di 3 MINUTI
				if(confirmWaiterThread.isAlive())
				{
					confirmTimeOut=true;
					System.out.println("");
					System.out.println("Too late! Your score is 0.");
				}
				
				confirmWaiterThread.interrupt();
			}
			catch (InterruptedException e)
			{
			}
			
			if(!confirmTimeOut)
			{				
				String uppedLetters=noSplittedLetters.toUpperCase();				
				char[] letters=uppedLetters.toCharArray();
				
				
				WordsReader wordsReader=new WordsReader(localInput,wordsList);
				Thread wordsReaderThread=new Thread(wordsReader);
				System.out.println("");
				System.out.println("The letters of this game are: ");
				for(char c: letters)
					System.out.print("  "+c);
				System.out.println();				
				
				wordsReaderThread.start();
				try
				{
					wordsReaderThread.join(120000); // Timer di 2 MINUTI																	
					wordsReaderThread.interrupt();					
				}
				catch (InterruptedException e)
				{																	
				}
			}			
						
			byte[] udpTempWords=wordsList.getWordsList().getBytes();
			DatagramPacket words=new DatagramPacket(udpTempWords, udpTempWords.length,udpSocketAddress);
			MulticastSocket multicastSocket=new MulticastSocket(multiCastPort);
			InetAddress multicastGroup=InetAddress.getByName(multicastIP);				
			DatagramPacket packet=new DatagramPacket(new byte[512], 512);				
			udpSocket.send(words);
			udpSocket.close();
			if(!confirmTimeOut)
			{
				//RICEZIONE RISULTATI PARTITA
				multicastSocket.joinGroup(multicastGroup);
				System.out.println("");
				System.out.println("!!!!!!!!!!!!!TIMEOUT!!!!!!!!!!!");
				System.out.println("");
				System.out.println("Words sent to the server: "+wordsList.getWordsList());
				System.out.println("Waiting for results...");		
				multicastSocket.receive(packet);
				
				byte[] bytes=packet.getData();				
				ByteArrayInputStream in_byte_stream=new ByteArrayInputStream(bytes);
				ObjectInputStream in=new ObjectInputStream(in_byte_stream);
				
				ArrayList<Result> results=(ArrayList<Result>)in.readObject();
				
				System.out.println("");
				System.out.println("_____RESULTS_____");
				int i=1;
				for(Result res: results)
				{
					System.out.println(i+". "+res.username+"  "+res.score);
					i++;
				}
				System.out.println("Players with score -1 accepted the game too late.");
			}
			multicastSocket.close();
		}
		System.out.println("");
		System.out.println("Game ends.Insert any key or press enter to return in the main menu");
		localInput.readLine();			
		
		busy.getAndSet(false);
	}
	
	//SEZIONE 2.1 DELLA RELAZIONE -Visualizzazione classifica generale
	public static void getRankings() throws IOException,ClassNotFoundException 
	{
		tcpSocket=new Socket();
		tcpSocket.connect(new InetSocketAddress(serverAddress,tcpPort));
		outputStream=new ObjectOutputStream(tcpSocket.getOutputStream());						
		outputStream.writeObject("3");
		outputStream.flush();		
		inputStream=new ObjectInputStream(tcpSocket.getInputStream());
		JSONArray rankings=(JSONArray)inputStream.readObject();
		tcpSocket.close();
		int i=1;
		System.out.println("");
		System.out.println("_____RANKING_____");
		for(Object res :rankings)			
		{
			JSONObject r=(JSONObject) res;
			String username=(String)r.get("username");
			int score=(int) r.get("score");			
			System.out.println(i+". "+username+"  "+score);
			i++;
		}
	}
	
	//SEZIONE 1 DELLA RELAZIONE
	public static void main(String[] args) throws IOException,ClassNotFoundException
	{
		configFile=new File("config.properties");
		InputStream confIn=new FileInputStream(configFile);
		Properties configuration=new Properties();
		configuration.load(confIn);
		serverAddress=configuration.getProperty("SERVER_ADDRESS");
		rmiName=configuration.getProperty("RMI_NAME");
		rmiPort=Integer.parseInt(configuration.getProperty("RMI_PORT"));
		tcpPort=Integer.parseInt(configuration.getProperty("TCP_PORT"));
		multiCastPort=Integer.parseInt(configuration.getProperty("MULTICAST_PORT"));
		confIn.close();
		
		busy=new AtomicBoolean(false);
		invitesLock=new ReentrantLock();
		serverRmi = null;		
		
		try 
		{
			Registry registry=LocateRegistry.getRegistry(serverAddress,rmiPort);
			serverRmi=(ServerRmiIF) registry.lookup(rmiName);
		}
		catch (NotBoundException e) 
		{
			System.out.println("Error while setting Rmi");
		}
		
		System.out.println("Welcome to TextTwist! Insert the option's number and press Enter");
		String choice;
		localInput=new BufferedReader(new InputStreamReader(System.in));
		
		System.out.println("1. Login");
		System.out.println("2. SignIn");
		choice=localInput.readLine();
		while(!choice.equals("1") && !choice.equals("2"))
		{
			System.out.println("Wrong selection. Retry.");
			choice=localInput.readLine();
		}
		if(choice.equals("1"))
		{
			boolean logOk=false;
			System.out.println("___Login___");
			while(!logOk)
			{
				
				System.out.println("Username: ");
				username = localInput.readLine();
				System.out.println("Password: ");
				password=localInput.readLine();
				logOk=serverRmi.logIn(new Client(),username,password);
				if(!logOk)
					System.out.println("Wrong user or Password! Retry");
				else
					System.out.println("Login Successfull");
			}			
		}
		else
		{
			boolean regOk=false;
			System.out.println("___Create a new Account___");
			while(!regOk)
			{
				System.out.println("Username: ");
				username = localInput.readLine();
				System.out.print("Password: ");
				password = localInput.readLine();
				regOk=serverRmi.signIn(new Client(),username,password);
				if(!regOk)
					System.out.println("Username already in use. Retry");
				else
					System.out.println("User "+username+" registered.");
			}
		}
		
		mainMenu();		
		localInput.close();
		System.exit(0);
	}
}