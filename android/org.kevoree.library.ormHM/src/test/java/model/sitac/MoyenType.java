package model.sitac;

import java.io.Serializable;

public class MoyenType implements Serializable {

	private int code;
	
	public MoyenType(int code)
	{
		this.code = code;
	}
	
	public int getCode()
	{
		return code;
	}
	
	public void setCode(int newCode)
	{
		code = newCode;
	}
}
