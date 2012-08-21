package org.opentripplanner.analyst.parameter;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import lombok.Delegate;

public class StyleList {

    @Delegate
    List<Style> styles = new ArrayList<Style>(); 

    public StyleList(String v) {
        for (String s : v.split(",")) {
            if (s.isEmpty())
                s = "COLOR30";
            if (s.toUpperCase().equals("GREY"))
                s = "GRAY";
            try {
                styles.add(Style.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer style: " + s)
                    .build());
            }
        }
    }
    
}

