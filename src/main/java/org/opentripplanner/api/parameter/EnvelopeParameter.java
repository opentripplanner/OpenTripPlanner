package org.opentripplanner.api.parameter;

import java.io.StringReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.geotools.geometry.Envelope2D;

/** order is minx,miny,maxx,maxy */
public class EnvelopeParameter {

    public Envelope2D env;


    public EnvelopeParameter(String param) {

        String[] tokens = param.split(",");
        try {
            double minx = Double.parseDouble(tokens[0]);
            double miny = Double.parseDouble(tokens[1]);
            double maxx = Double.parseDouble(tokens[2]);
            double maxy = Double.parseDouble(tokens[3]);
            // null crs, set later from another parameter
            env = new Envelope2D(null, minx, miny, maxx-minx, maxy-miny);
        } catch (Exception e) {
            throw new WebApplicationException(fail(param, e));
        }
    }

    protected Response fail(String param, Exception e) {
        return Response.status(Status.BAD_REQUEST).entity(param + ": " + e.getMessage()).build();
    }

}
