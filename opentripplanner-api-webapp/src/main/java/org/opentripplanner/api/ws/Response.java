package org.opentripplanner.api.ws;

import java.util.List;
import java.util.ArrayList;
import java.util.Hashtable;

import javax.ws.rs.Path;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;

/**
 *
 */
@XmlRootElement
public class Response {

    public Hashtable<String, String> requestParameters;
    public TripPlan plan;

    public Response() {
    }

    public Response(Request req) {
        requestParameters = req.getParameters();
        plan = new TripPlan();
    }
}