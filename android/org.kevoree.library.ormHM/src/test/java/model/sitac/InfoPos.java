package model.sitac;

import java.io.Serializable;

public class InfoPos implements Serializable {
	private Position position;
	
	public InfoPos(double lat, double lon)
	{
		position = new PositionGPS(lat, lon);
	}
	
	public void setPosition(double lat, double lon)
	{
		position.setLat(lat);
		position.setLong(lon);
	}
	
	public Position getPosition()
	{
		return position;
	}
}
