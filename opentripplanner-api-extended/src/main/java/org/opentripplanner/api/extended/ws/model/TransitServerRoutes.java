package org.opentripplanner.api.extended.ws.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.api.extended.fork.EncodedPolylineBean;
import org.opentripplanner.api.extended.fork.PolylineEncoder;
import org.opentripplanner.api.extended.ws.TransitServerGtfs;

@XmlRootElement(name="routes")
public class TransitServerRoutes {

    @XmlElement(name="route")
    private Collection<TransitServerRoute> routes = new ArrayList<TransitServerRoute>();

    public TransitServerRoutes() {
    }
    
    public TransitServerRoutes(TransitServerGtfs gtfs) {
        for (Route route : gtfs.getRoutes()) {
            String routeId = route.getId().toString();
            List<Stop> stops = gtfs.getStopsForRoute(routeId);
            List<ShapePoint> shapePoints = gtfs.getShapePointsForRoute(routeId);
            
            int n = shapePoints.size();
            List<ShapePoint> sortedShapePoints = new ArrayList<ShapePoint>();
            sortedShapePoints.addAll(shapePoints);
            Collections.sort(sortedShapePoints);
            double[] lat = new double[n];
            double[] lon = new double[n];
            int i = 0;
            for (ShapePoint sp : sortedShapePoints) {
                lat[i] = sp.getLat();
                lon[i] = sp.getLon();
                i++;
            }
            EncodedPolylineBean geometry = PolylineEncoder.createEncodings(lat, lon);

            routes.add(new TransitServerRoute(route, stops, geometry));
        }
    }

}
