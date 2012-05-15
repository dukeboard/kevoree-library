package model.sitac;

import java.io.Serializable;

public class InterventionType implements Serializable {
	private String code;
	
	public InterventionType(String code)
	{
		this.code = code;
	}
	
	public void setCode(String code)
	{
		this.code = code;
	}
	
	public String getCode()
	{
		return code;
	}
}
