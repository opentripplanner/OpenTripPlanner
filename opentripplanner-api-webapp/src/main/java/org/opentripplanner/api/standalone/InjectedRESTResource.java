package org.opentripplanner.api.standalone;

import javax.annotation.Resource;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.sun.jersey.spi.resource.Singleton;

@Path("/inject")
@Singleton
public class InjectedRESTResource {
    
    // @Resource Thing t;
    
    @GET
    @Produces({ MediaType.TEXT_PLAIN })
    public String get(
            @QueryParam("x") @DefaultValue("4") double x,
            @QueryParam("y") @DefaultValue("6") double y) {
        return "x=" + x + " y=" + y;
    }

}
