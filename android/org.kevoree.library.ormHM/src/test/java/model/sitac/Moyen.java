package model.sitac;

import org.kevoree.library.ormHM.annotations.Id;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Moyen extends Detachement implements Serializable {

    private int number;
	private MoyenType type;
	private String name;
	private PositionGPS posTarget;
	private PositionGPS posRef;
	private List<Moyen> children = new ArrayList<Moyen>();
	
	public Moyen(MoyenType type)
	{
		this.type=type;
	}
	
	public Moyen(MoyenType type, String name)
	{
		this(type);
		this.name = name;
	}
	
	public Moyen(MoyenType type, String name, int number)
	{
		this(type, name);
		this.number = number;
	}
	
	public void setTargetPos(double lat, double lon)
	{
		posTarget = new PositionGPS(lat, lon);
	}
	
	public void setRefPos(double lat, double lon)
	{
		posRef = new PositionGPS(lat, lon);
	}
	
	public PositionGPS getTargetPos()
	{
		return posTarget;
	}
	
	public PositionGPS getRefPos()
	{
		return posRef;
	}
	
	public MoyenType getType()
	{
		return type;
	}
	
	public void setType(MoyenType newType)
	{
		type=newType;
	}
	
	public String getName()
	{
		return name;
	}
	
	public void setName(String newName)
	{
		name=newName;
	}
	
	public void setNumber(int number)
	{
		this.number = number;
	}
	@Id(attachTOCache = "Moyens")
	public int getNumber()
	{
		return number;
	}
	
	public List<Moyen> getChildren()
	{
		return children;
	}
	
	public int getHeight()
	{
		if (children.size() == 0)
			return 0;
		else
		{
			int[] val = new int[children.size()];
			for (int i=0; i<children.size(); i++)
				val[i] = children.get(i).getHeight();
			return 1 + max(val);
		}
	}
	
	private int max(int[] val)
	{
		int max = val[0];
		for (int i=1; i<val.length; i++)
			if (val[i] > max)
				max = val[i];
		return max;
	}
}
