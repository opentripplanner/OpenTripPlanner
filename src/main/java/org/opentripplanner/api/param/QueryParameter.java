package org.opentripplanner.api.param;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public abstract class QueryParameter {

    public static void checkRangeInclusive(double x, double min, double max) throws Exception {
        boolean ok = (x >= min && x <= max);
        if (!ok) throw new Exception(String.format("%f is not in range [%f,%f]", x, min, max));
    }

    protected abstract void parse(String param) throws Throwable;

    public QueryParameter(String value) throws WebApplicationException {
        try {
            parse(value);
        } catch (Throwable e) {
            String message = String.format("Error parsing parameter: %s (%s)", value, e.toString());
            Response response = Response.status(Status.BAD_REQUEST).entity(message).build();
            throw new WebApplicationException(response);
        }
    }


}
