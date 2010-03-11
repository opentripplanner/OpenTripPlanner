/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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
import org.opentripplanner.api.extended.ws.TransitServerGtfs;
import org.opentripplanner.util.PolylineEncoder;
import org.opentripplanner.util.model.EncodedPolylineBean;

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
