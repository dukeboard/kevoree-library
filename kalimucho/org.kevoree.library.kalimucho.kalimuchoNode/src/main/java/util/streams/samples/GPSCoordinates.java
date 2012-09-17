package util.streams.samples;

/**
 * Class of a sample to send/receive GPS coordinates.
 *
 * @author Dalmau
 */

public class GPSCoordinates extends Sample {

    private static final long serialVersionUID = 64240010100000001L; // pour sérialisation
    private double latitude, longitude, altitude;

    /**
     * Creates an empty sample
     */
    public GPSCoordinates() {
        latitude = 0;
        longitude = 0;
        altitude = 0;
    }

    /**
     * Creates a sample
     * @param lat latitude
     * @param longt longitude
     * @param alt altitude
     */
    public GPSCoordinates(double lat, double longt, double alt) {
        latitude = lat;
        longitude = longt;
        altitude = alt;
    }

    /**
     * Gets the latitude
     * @return latitude
     */
    public double getLatitude() { return latitude; }
    /**
     * Gets the longitude
     * @return the longitude
     */
    public double getLongitude() { return longitude; }
    /**
     * Gets the altitude
     * @return the altitude
     */
    public double getAltitude() { return altitude; }

}
