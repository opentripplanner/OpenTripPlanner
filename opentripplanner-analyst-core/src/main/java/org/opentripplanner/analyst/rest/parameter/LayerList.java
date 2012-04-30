package org.opentripplanner.analyst.rest.parameter;

import java.util.ArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 *  WMS allows several layers and styles to be specified. We parse these parameters as if they 
 *  might contain a comma-separated list, but only use the first one in the WMS resource.
 *  This class also uppercases the query parameters to make sure they match enum constants.
 */
public class LayerList extends ArrayList<Layer> {

    private static final long serialVersionUID = 1L;

    public LayerList(String v) {
        super();
        for (String s : v.split(",")) {
            if (s.isEmpty())
                s = "TRAVELTIME";
                    
            try {
                this.add(Layer.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer name: " + s)
                    .build());
            }
        }
    }

}

