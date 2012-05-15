package model.sitac;

import java.io.Serializable;
import java.sql.Date;

public class Affectation  implements Serializable {
	private Date horodatageDemande;
	private Date horodatageEngagement;
	private Date horodatageDesengagement;
	
	public Affectation()
	{
		
	}

	public String getDemande()
	{
		return horodatageDemande.toString();
	}
	
	public void setDemande(String date)
	{
		horodatageDemande = Date.valueOf(date);
	}
	
	public String getEngagement()
	{
		return horodatageEngagement.toString();
	}
	
	public void setEngagement(String date)
	{
		horodatageEngagement = Date.valueOf(date);
	}
	
	public String getDesengagement()
	{
		return horodatageDesengagement.toString();
	}
	
	public void setDDesengagement(String date)
	{
		horodatageDesengagement = Date.valueOf(date);
	}
}
