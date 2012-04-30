package org.opentripplanner.analyst.parameter;

import java.util.ArrayList;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class StyleList extends ArrayList<Style> {

    private static final long serialVersionUID = 1L;

    public StyleList(String v) {
        super();
        for (String s : v.split(",")) {
            if (s.isEmpty())
                s = "COLOR30";
            if (s.toUpperCase().equals("GREY"))
                s = "GRAY";
                    
            try {
                this.add(Style.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer style: " + s)
                    .build());
            }
        }
    }

}

