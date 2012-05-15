package model.sitac;

import java.util.List;

public class Soiec {
	private SoiecNodeInfo root;
	
	public Soiec()
	{
		root = new SoiecNodeInfo("");
		root.addChild(new SoiecNodeInfo("Ten persons locked in the building"));
		root.addChild(new SoiecNodeInfo("Heavy smoke"));
		root.addChild(new SoiecNodeInfo("Hydrant does not work"));
		root.getChild(0).addChild(new SoiecNodeInfo("objective 1.1"));
		root.getChild(0).addChild(new SoiecNodeInfo("objective 1.2"));
		root.getChild(1).addChild(new SoiecNodeInfo("objective 2.1"));
		root.getChild(2).addChild(new SoiecNodeInfo("objective 3.1"));
		root.getChild(0).getChild(0).addChild(new SoiecNodeInfo("idm 1.1.1"));
		root.getChild(2).getChild(0).addChild(new SoiecNodeInfo("idm 3.1.1"));
		root.getChild(0).getChild(0).getChild(0).addChild(new SoiecNodeInfo("exec 1.1.1.1"));
	}
	
	public SoiecNodeInfo getRoot()
	{
		return root;
	}
	
	public SoiecNodeInfo getChild(String desc)
	{
		List<SoiecNodeInfo> children = root.getChildren();
		for (int i=0; i<children.size(); i++)
			if (children.get(i).getDesc().equals(desc))
				return children.get(i);
		return null;
	}
	
}
