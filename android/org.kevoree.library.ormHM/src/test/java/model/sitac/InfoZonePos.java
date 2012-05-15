package model.sitac;

import java.io.Serializable;

public class InfoZonePos extends InfoLignePos implements Serializable {
	private String name;
	
	public InfoZonePos(String name)
	{
		super();
		this.name = name;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String name)
	{
		this.name = name;
	}
}