package org.opentripplanner.api.extended.ws;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.naming.Context;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.api.extended.ws.model.TransitServerDepartures;
import org.opentripplanner.api.extended.ws.model.TransitServerRoute;
import org.opentripplanner.api.extended.ws.model.TransitServerRoutes;
import org.opentripplanner.api.extended.ws.model.TransitServerStop;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;

@Path("/")
@Autowire
public class TransitDataServer {

    private TransitServerGtfs transitServerGtfs;

    @Autowired
    public void setTransitServerGtfs(TransitServerGtfs transitServerGtfs) {
        this.transitServerGtfs = transitServerGtfs;
    }
    
    @GET
    @Path("/")
    @Produces("text/html")
    public String getIndex() {
        return "<html><body><h2>The system works</h2></body></html>";
    }
    
    @GET
    @Path("routes")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerRoutes getRoutes() throws JSONException {
        return new TransitServerRoutes(transitServerGtfs);
    }
    
    @GET
    @Path("departures")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerDepartures getDepartures(@QueryParam("lat") String lat,
                                                 @QueryParam("lon") String lon,
                                                 @DefaultValue("3") @QueryParam("n") int n) throws JSONException {
        String latlon = lat + "," + lon;
        return new TransitServerDepartures(latlon, n, transitServerGtfs);
    }
    
    @GET
    @Path("routes/{route_id}")
    @Produces( { MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public TransitServerRoute getRoute(@PathParam("route") String routeId) throws JSONException {
        return new TransitServerRoute(transitServerGtfs, routeId);
    }
}
