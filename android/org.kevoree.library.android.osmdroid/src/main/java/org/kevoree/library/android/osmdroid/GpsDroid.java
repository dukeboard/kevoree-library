package org.kevoree.library.android.osmdroid;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import org.kevoree.android.framework.helper.UIServiceHandler;
import org.kevoree.android.framework.service.KevoreeAndroidService;
import org.kevoree.annotation.*;
import org.kevoree.common.gps.impl.GpsPoint;
import org.kevoree.framework.AbstractComponentType;
import org.kevoree.framework.MessagePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by jed
 * User: jedartois@gmail.com
 * Date: 10/04/12
 * Time: 11:09
 */

@Library(name = "Android")
@ComponentType
@Requires({
        @RequiredPort(name = "position", type = PortType.MESSAGE,optional = true),
        @RequiredPort(name = "speed", type = PortType.MESSAGE,optional = true),
        @RequiredPort(name = "bearing", type = PortType.MESSAGE,optional = true)
})

public class GpsDroid   extends AbstractComponentType{

    private Logger logger = LoggerFactory.getLogger(this.getClass());
    private LocationManager locationManager = null;
    private KevoreeAndroidService uiService = null;

    @Start
    public void start()
    {
        uiService = UIServiceHandler.getUIService();
        locationManager= (LocationManager)uiService.getRootActivity().getSystemService(Context.LOCATION_SERVICE);

        uiService.getRootActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Criteria criteria = new Criteria();
                criteria.setAccuracy(Criteria.ACCURACY_FINE);
                criteria.setAltitudeRequired(false);
                criteria.setBearingRequired(false);
                criteria.setCostAllowed(true);
                criteria.setPowerRequirement(Criteria.POWER_MEDIUM);

                locationManager.requestLocationUpdates(locationManager.getBestProvider(criteria, true), 100, 1, gpsListener);
            }
        });
    }

    LocationListener gpsListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {

            MessagePort port_position   = (MessagePort) getPortByName("position");
            MessagePort port_speed      = (MessagePort) getPortByName("speed");
            MessagePort port_bearing    = (MessagePort) getPortByName("bearing");

            port_position.process(new GpsPoint(location.getLatitude(), location.getLongitude()));
            port_speed.process(location.getSpeed());
            port_bearing.process(location.getBearing());
        }

        @Override
        public void onProviderDisabled(String arg0) {
        }

        @Override
        public void onProviderEnabled(String arg0) {
        }

        @Override
        public void onStatusChanged(String arg0, int arg1, Bundle arg2) {
        }
    };



    @Stop
    public void stop() {


    }

    @Update
    public void update() {

    }



}
