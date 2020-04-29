package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiTripPlan;
import org.opentripplanner.api.model.ApiTripSearchMetadata;
import org.opentripplanner.api.model.error.PlannerError;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

/** Represents a trip planner response, will be serialized into XML or JSON by Jersey */
public class TripPlannerResponse {

    /** A dictionary of the parameters provided in the request that triggered this response. */
    public HashMap<String, String> requestParameters;
    private ApiTripPlan plan;
    private ApiTripSearchMetadata metadata;
    private PlannerError error = null;

    /** Debugging and profiling information */
    public DebugOutput debugOutput = null;

    public ElevationMetadata elevationMetadata = null;

    /** This no-arg constructor exists to make JAX-RS happy. */ 
    @SuppressWarnings("unused")
    private TripPlannerResponse() {};

    /** Construct an new response initialized with all the incoming query parameters. */
    public TripPlannerResponse(UriInfo info) {
        this.requestParameters = new HashMap<String, String>();
        if (info == null) { 
            // in tests where there is no HTTP request, just leave the map empty
            return;
        }
        for (Entry<String, List<String>> e : info.getQueryParameters().entrySet()) {
            // include only the first instance of each query parameter
            requestParameters.put(e.getKey(), e.getValue().get(0));
        }
    }

    // NOTE: the order the getter methods below is semi-important, in that Jersey will use the
    // same order for the elements in the JS or XML serialized response. The traditional order
    // is request params, followed by plan, followed by errors.

    /** The actual trip plan. */
    public ApiTripPlan getPlan() {
        return plan;
    }

    public void setPlan(ApiTripPlan plan) {
        this.plan = plan;
    }

    public ApiTripSearchMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(ApiTripSearchMetadata metadata) {
        this.metadata = metadata;
    }

    /** The error (if any) that this response raised. */
    public PlannerError getError() {
        return error;
    }

    public void setError(PlannerError error) {
        this.error = error;
    }
    
}