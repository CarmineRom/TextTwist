package commonPackage;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientRmiIF extends Remote 
{
	public boolean invite(String gameName,String gameCreator) throws RemoteException;	
	public void isOnline() throws RemoteException;
}
