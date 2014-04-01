package org.opentripplanner.api.parameter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.onebusaway.gtfs.model.AgencyAndId;

public class FeedScopedId {
    
    String feedId;
    String entityId;

    private static void err (String message) {
        throw new WebApplicationException(Response
                .status(Status.BAD_REQUEST)
                .entity(message)
                .build());
    }
    
    public FeedScopedId (String s) {
        try {
            /* We do not use split in case the entity ID contains a slash. */
            int idx = s.indexOf('/');
            if (idx == -1) {
                feedId = "none";
                entityId = s;
            } else {
                feedId = s.substring(0, idx);
                entityId = s.substring(idx + 1);            
            }
        } catch (Exception ex) {
            err ("Unable to parse feed-scoped ID: " + ex.getMessage());
        }        
    }
    
    public AgencyAndId toAgencyAndId () {
        return new AgencyAndId(feedId, entityId);
    }

}
