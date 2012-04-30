package org.opentripplanner.analyst.rest.parameter;

import java.util.Arrays;
import java.util.Collection;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class MIMEImageFormat {
    
    public static final Collection<String> acceptedTypes =
        Arrays.asList("png", "gif", "jpeg", "geotiff");
    
    public final String type;
            
    public MIMEImageFormat(String s) {
        String[] parts = s.split("/");
        if (parts.length == 2 && parts[0].equals("image")) {
            if (acceptedTypes.contains(parts[1])) {
                type = parts[1];
            } else {
                throw new WebApplicationException(Response
                        .status(Status.BAD_REQUEST)
                        .entity("unsupported image format: " + parts[1])
                        .build());
            }
        } else {
            throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("malformed image format mime type: " + s)
                    .build());
        }
    }
 
    public String toString() {
        return "image/" + type;
    }
}
