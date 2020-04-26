package commonPackage;

import java.io.Serializable;

public class Result implements Serializable
{	
	static final long serialVersionUID= 1L;
	public String username;
	public int score;
	
	public Result(String username,int score)
	{
		this.username=username;
		this.score=score;
	}
}
