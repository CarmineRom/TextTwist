package commonPackage;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerRmiIF extends Remote {
	
	public boolean signIn(Object callback,String username, String password) throws RemoteException;
	
	public boolean logIn(Object callback,String username, String password) throws RemoteException;
	
	public boolean logOut(String username) throws RemoteException;
}