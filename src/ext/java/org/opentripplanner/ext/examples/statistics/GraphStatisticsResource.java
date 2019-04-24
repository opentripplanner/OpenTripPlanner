package org.opentripplanner.ext.examples.statistics;

import org.opentripplanner.ext.examples.statistics.api.GraphStatistics;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/statistics")
@Produces(MediaType.APPLICATION_JSON)
public class GraphStatisticsResource {

    @GET
    @Path("/graph")
    public GraphStatistics status() {
        return new GraphStatistics(12, 12);
    }
}
