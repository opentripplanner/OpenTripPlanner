package org.opentripplanner.api.ws;

import java.util.Hashtable;

import javax.xml.bind.annotation.XmlRootElement;

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
    }
}