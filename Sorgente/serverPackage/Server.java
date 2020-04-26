package serverPackage;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.Timer;

import commonPackage.*;

public class Server extends UnicastRemoteObject implements ServerRmiIF
{	
	private static final long serialVersionUID = 1L;
	protected static String serverAddress;
	protected static String rmiName;
	protected static int rmiPort;
	protected static int tcpPort;
	protected static int multiCastPort;
	static ConcurrentHashMap<String, ClientRmiIF> onlineUsers;
	private static ConcurrentHashMap<String, String> usersList;
	static ConcurrentHashMap<String, String> registeredMulticast;
	static ConcurrentHashMap<String, Game> gamesList;		
	static ArrayList<String> dictionary;
	static Rankings rankings;	
	private static ReentrantLock fileLock;
	private static File usersListFile;
	private static File dictionaryFile;	
	private static File configFile;
	protected static File rankingsFile;	
	
	public Server() throws RemoteException {
	}
	
	//SEZIONE 1 DELLA RELAZIONE

	public static void main(String[] args) throws RemoteException 
	{	
		System.out.println("Server started");
		fileLock=new ReentrantLock();
		ObjectInputStream fileIn;
		ObjectOutputStream fileOut;	
		registeredMulticast=new ConcurrentHashMap<String,String>();
		onlineUsers= new ConcurrentHashMap<String,ClientRmiIF>();
		gamesList=new ConcurrentHashMap<String,Game>();
		usersListFile = new File("usersListFile.txt");
		dictionaryFile=new File("dictionary.txt");	
		rankingsFile=new File("rankings.txt");
		configFile=new File("config.properties");		
		
		//CARICAMENTO E/O CREAZIONE DEI VARI FILE
		try {
			InputStream confIn=new FileInputStream(configFile);
			Properties configuration=new Properties();
			configuration.load(confIn);
			serverAddress=configuration.getProperty("SERVER_ADDRESS");
			rmiName=configuration.getProperty("RMI_NAME");
			rmiPort=Integer.parseInt(configuration.getProperty("RMI_PORT"));
			tcpPort=Integer.parseInt(configuration.getProperty("TCP_PORT"));
			multiCastPort=Integer.parseInt(configuration.getProperty("MULTICAST_PORT"));
			confIn.close();
			System.out.println("Server configured");
			
			Scanner scanner=new Scanner(dictionaryFile);
			dictionary=new ArrayList<String>();
			while(scanner.hasNext())
			{
				dictionary.add(scanner.next());
			}
			scanner.close();
			System.out.println("Dictionary loaded");
			
			if(!rankingsFile.exists())
			{
				rankingsFile.createNewFile();
				ArrayList<Result> results=new ArrayList<Result>();
				rankings=new Rankings(results, rankingsFile);
				fileOut=new ObjectOutputStream(new FileOutputStream(rankingsFile));
				fileOut.writeObject(results);
				fileOut.close();
				System.out.println("Rankings File created");
			}
			else
			{
				fileIn=new ObjectInputStream(new FileInputStream(rankingsFile));
				ArrayList<Result> results=(ArrayList<Result>) fileIn.readObject();
				fileIn.close();
				rankings=new Rankings(results, rankingsFile);
				System.out.println("Rankings loaded from File");
			}
			
			if(!usersListFile.exists())
			{
				usersListFile.createNewFile();				
				usersList=new ConcurrentHashMap<String,String>();
				fileOut=new ObjectOutputStream(new FileOutputStream(usersListFile));
				fileOut.writeObject(usersList);
				fileOut.close();
				System.out.println("Users list file created");
			}
			else
			{
				fileIn=new ObjectInputStream(new FileInputStream(usersListFile));
				usersList=(ConcurrentHashMap<String, String>) fileIn.readObject();
				fileIn.close();
				System.out.println("Users List loaded from File");				
			}
		}		
		catch (FileNotFoundException g) {
			System.out.println("Errore! File non trovato");			
		}
		catch (Exception e) {			
			e.printStackTrace();
		}
		
		//REGISTRAZIONE RMI
		Server RMIServer = new Server();		
		Registry reg = LocateRegistry.createRegistry(rmiPort);
		reg.rebind(rmiName, RMIServer);
		System.out.println("Rmi registered");
		
		System.out.println("Server Ready");
		ServerSocket serverSocket;
		ExecutorService clientHandlers = Executors.newCachedThreadPool();
		try
		{
			serverSocket=new ServerSocket();
			serverSocket.bind(new InetSocketAddress(serverAddress,tcpPort));
			System.out.println("Socket open");
			//SEZIONE 2.2 DELLA RELAZIONE -Classe Server
			ActionListener onlineChecker=new ActionListener()
			{				
				public void actionPerformed(ActionEvent e)
				{
					if(!onlineUsers.isEmpty())
					{						
						onlineUsers.forEach((k,v) -> {
							try
							{
								v.isOnline();
							}
							catch (RemoteException r)
							{
								onlineUsers.remove(k);
							}
						});
					}					
				}
			};
			new Timer(10000,onlineChecker).start();
			
			while(true)
			{				
				Socket clientSocket=serverSocket.accept();
				System.out.println("Client Connected");
				clientHandlers.submit(new ClientHandler(clientSocket,onlineUsers,registeredMulticast,rankings,rankingsFile,serverAddress,multiCastPort,gamesList,dictionary));			
			}
		}
		catch (IOException e)
		{
			System.out.println("Error on server socket");
		}		
	}
	
	public boolean signIn(Object callback,String username, String password)
	{
		if(!usersList.containsKey(username))
		{			
			usersList.put(username, password);			
			
			onlineUsers.put(username,(ClientRmiIF) callback);			
			try
			{
				fileLock.lock();
				ObjectOutputStream fOut=new ObjectOutputStream(new FileOutputStream(usersListFile));
				fOut.writeObject(usersList);
				fOut.flush();							
				fOut.close();
				fileLock.unlock();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			
			System.out.println("New user "+username+" registered");
			return true;
			
		}
		else
		{
			return false;
		}
	}
	
	public boolean logIn(Object callback,String username, String password)
	{
		//Errore già loggato
		if(onlineUsers.containsKey(username))
		{
			return false;
		}
		String pass=usersList.get(username);		
		if(pass!=null && pass.equals(password))
		{
			onlineUsers.put(username,(ClientRmiIF)callback);
			System.out.println("User "+username+" logged in");
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public boolean logOut(String username) throws RemoteException
	{
		onlineUsers.remove(username);
		return true;
	}
}
