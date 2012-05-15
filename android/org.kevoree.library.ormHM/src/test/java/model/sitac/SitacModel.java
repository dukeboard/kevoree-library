package model.sitac;

import java.util.ArrayList;
import java.util.List;

public class SitacModel
{
	private List<Intervention> interventions = new ArrayList<Intervention>();
	private List<InterventionType> interventionTypes = new ArrayList<InterventionType>();
	private List<CIS> cis = new ArrayList<CIS>();
	
	public SitacModel()
	{
		
	}
	
	public List<Intervention> getInterventions()
	{
		return interventions;
	}
	
	public List<InterventionType> getInterventionTypes()
	{
		return interventionTypes;
	}
	
	public List<CIS> getCIS()
	{
		return cis;
	}
}
