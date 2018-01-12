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
package org.opentripplanner.api.resource;

import org.opentripplanner.index.model.StopTimesByStop;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import java.util.List;

import static org.opentripplanner.api.resource.ServerInfo.Q;

/**
 * Lookup arrival/departure times for a group of stops, by location of interest or list of stops.
 */
@Path("/routers/{routerId}/nearby")
@XmlRootElement
public class NearbySchedulesResource {

    /**
     * latitude of center of search circle. Either circle, list of stops, or both must be supplied.
     */
    @QueryParam("lat")
    private Double lat;

    /**
     * longitude of center of search circle.
     */
    @QueryParam("lon")
    private Double lon;

    /**
     * radius of center of search circle.
     */
    @QueryParam("radius")
    private Double radius;

    /**
     * list of stops of interest. Should be a comma-separated list in the format MTA:101001,MNR:1, etc. Optional
     * if lat, lon, and radius are given.
     */
    @QueryParam("stops")
    private String stopsStr;

    /**
     * list of routes of interest. Should be in the format MTASBWY__A,MNR__1, etc. Optional.
     */
    @QueryParam("routes")
    private String routesStr;

    /**
     * direction of interest. Optional. Use GTFS direction_id (1 or 0).
     */
    @QueryParam("direction")
    private String direction;

    /**
     * date to return arrival/departure times for. Will default to the current date.
     */
    @QueryParam("date")
    private String date;

    /**
     * time to return arrival/departure times for. Will default to the current time.
     */
    @QueryParam("time")
    private String time;

    /**
     * Range, in seconds, from given time, in which to return arrival/departure results.
     */
    @QueryParam("timeRange")
    @DefaultValue("1800")
    private int timeRange;

    /**
     * Return upcoming vehicle arrival/departure times at given stops. Matches stops by lat/lon/radius,
     * and/or by list of stops. Arrival/departure times can be filtered by route and direction.
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q})
    public List<StopTimesByStop> getNearbySchedules() {
        throw new IllegalArgumentException("Not implemented.");
    }
}
