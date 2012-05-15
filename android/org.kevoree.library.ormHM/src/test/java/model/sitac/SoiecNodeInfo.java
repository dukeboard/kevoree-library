package model.sitac;

import java.util.ArrayList;
import java.util.List;

public class SoiecNodeInfo {
	private String desc;
	private List<SoiecNodeInfo> children;
	
	public SoiecNodeInfo(String desc)
	{
		this.desc = desc;
		children = new ArrayList<SoiecNodeInfo>();
	}
	
	public String getDesc()
	{
		return desc;
	}
	
	public void setDesc(String desc)
	{
		this.desc = desc;
	}
	
	public List<SoiecNodeInfo> getChildren()
	{
		return children;
	}
	
	public SoiecNodeInfo getChild(int index)
	{
		return children.get(index);
	}
	
	public void addChild(SoiecNodeInfo node)
	{
		children.add(node);
	}
	
	public void removeChild(SoiecNodeInfo node)
	{
		children.remove(node);
	}
	
	public String toString()
	{
		return desc;
	}
}
