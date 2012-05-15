package model.sitac;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class InfoLignePos  implements Serializable {
	private List<Position> positions;
	
	public InfoLignePos()
	{
		positions = new ArrayList<Position>();
	}
	
	public void addPosition(double lat, double lon)
	{
		positions.add(new PositionGPS(lat, lon));
	}
	
	public List<Position> getPositions()
	{
		return positions;
	}
	
	public void setPositions(List<Position> pos)
	{
		positions = pos;
	}
}
