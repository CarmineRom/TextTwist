package clientPackage;

import java.io.BufferedReader;

public class ConfirmWaiter implements Runnable 
{
	BufferedReader localInput;
	
	public ConfirmWaiter(BufferedReader localInput)
	{
		this.localInput=localInput;
	}
	
	public void run()
	{
		try
		{
			System.out.println("");
			System.out.println("Game Ready, insert any key or press enter to start.");
			Boolean done=false;
			while(!Thread.currentThread().isInterrupted() && !done)
			{				
				localInput.readLine();
				done=true;				
			}		
		}
		catch (Exception e)
		{
		}		
	}
}
