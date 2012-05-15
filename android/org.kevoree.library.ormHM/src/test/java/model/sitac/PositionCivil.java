package model.sitac;

import java.io.Serializable;

public class PositionCivil extends Position implements Serializable {
	private String street;
	private int number;
	private String country;
	private String cp;
	
	public PositionCivil(String street, int number, String country, String cp)
	{
		this.street = street;
		this.number = number;
		this.country = country;
		this.cp = cp;
	}
	
	public void setStreet(String street)
	{
		this.street = street;
	}
	
	public String getStreet()
	{
		return street;
	}
	
	public void setNumber(int number)
	{
		this.number = number;
	}
	
	public int getNumber()
	{
		return number;
	}
	
	public void setCountry(String country)
	{
		this.country = country;
	}
	
	public String getCountry()
	{
		return country;
	}
	
	public void setCp(String cp)
	{
		this.cp = cp;
	}
	
	public String getCp()
	{
		return cp;
	}
	
	public double getLat()
	{
		return 0.0;
	}
	
	public double getLong()
	{
		return 0.0;
	}
	
	public void setLat(double lat)
	{
		
	}
	
	public void setLong(double lon)
	{
		
	}
}
