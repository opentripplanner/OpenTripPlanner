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

package org.opentripplanner.api.model;

import com.conveyal.geojson.GeometryDeserializer;
import com.conveyal.geojson.GeometrySerializer;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.vividsolutions.jts.geom.Geometry;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.vividsolutions.jts.geom.Coordinate;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.util.TravelOption;
import org.opentripplanner.util.TravelOptionsMaker;
import org.opentripplanner.util.WorldEnvelope;

@XmlRootElement(name = "RouterInfo")
public class RouterInfo {

    private final BikeRentalStationService service;

    @XmlElement
    public String routerId;
    
    @JsonSerialize(using=GeometrySerializer.class)
    @JsonDeserialize(using=GeometryDeserializer.class)
    @XmlJavaTypeAdapter(value=GeometryAdapter.class,type=Geometry.class)
    public Geometry polygon;

    @XmlElement
    public Date buildTime;

    @XmlElement
    public long transitServiceStarts;

    @XmlElement
    public long transitServiceEnds;

    public HashSet<TraverseMode> transitModes;

    private WorldEnvelope envelope;

    public double centerLatitude;

    public double centerLongitude;

    public boolean hasParkRide;

    public List<TravelOption> travelOptions;


    public RouterInfo(String routerId, Graph graph) {
        this.routerId = routerId;
        this.polygon = graph.getConvexHull();
        this.buildTime = graph.buildTime;
        this.transitServiceStarts = graph.getTransitServiceStarts();
        this.transitServiceEnds = graph.getTransitServiceEnds();
        this.transitModes = graph.getTransitModes();
        this.envelope = graph.getEnvelope();
        addCenter(graph.getCenter());
        service = graph.getService(BikeRentalStationService.class, false);
        hasParkRide = graph.hasParkRide;
        travelOptions = TravelOptionsMaker.makeOptions(graph);
    }

    public boolean getHasBikeSharing() {
        if (service == null) {
            return false;
        }

        //at least 2 bike sharing stations are needed for useful bike sharing
        return service.getBikeRentalStations().size() > 1;
    }

    public boolean getHasBikePark() {
        if (service == null) {
            return false;
        }

        return !service.getBikeParks().isEmpty();
    }

    /**
     * Set center coordinate from transit center in {@link Graph#calculateTransitCenter()} if transit is used
     * or as mean coordinate if not
     *
     * It is first called when OSM is loaded. Then after transit data is loaded.
     * So that center is set in all combinations of street and transit loading.
     * @param center
     */
    public void addCenter(Optional<Coordinate> center) {
        //Transit data was loaded and center was calculated with calculateTransitCenter
        if(center.isPresent()) {
            centerLongitude = center.get().x;
            centerLatitude = center.get().y;
        } else {
            // Does not work around 180th parallel.
            centerLatitude = (getUpperRightLatitude() + getLowerLeftLatitude()) / 2;
            centerLongitude = (getUpperRightLongitude() + getLowerLeftLongitude()) / 2;
        }
    }

    public double getLowerLeftLatitude() {
        return envelope.getLowerLeftLatitude();
    }

    public double getLowerLeftLongitude() {
        return envelope.getLowerLeftLongitude();
    }

    public double getUpperRightLatitude() {
        return envelope.getUpperRightLatitude();
    }

    public double getUpperRightLongitude() {
        return envelope.getUpperRightLongitude();
    }
}
