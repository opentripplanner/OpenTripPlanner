package org.opentripplanner.api.resource;

import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.analyst.scenario.ScenarioStore;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Presents saved non-destructive transit analysis scenarios as a document tree for manipulation via HTTP.
 */
@Path("/routers/{routerId}/scenarios")
@Produces(MediaType.APPLICATION_JSON)
public class ScenarioResource {

    private static final Logger LOG = LoggerFactory.getLogger(ScenarioResource.class);

    private static final String MSG_404 = "FOUR ZERO FOUR";
    private static final String MSG_400 = "FOUR HUNDRED";

    private ScenarioStore scenarioStore;

    /** Choose short or long form of results. */
    @QueryParam("detail") private boolean detail = false;

    /** Include GTFS entities referenced by ID in the result. */
    @QueryParam("refs") private boolean refs = false;

    public ScenarioResource(@Context OTPServer otpServer, @PathParam("routerId") String routerId) {
        Router router = otpServer.getRouter(routerId);
        scenarioStore = router.scenarioStore;
    }

    /** Return a list of all scenarios defined for this router. */
    @GET
    public javax.ws.rs.core.Response getScenarioDescriptions () {
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK)
                .entity(scenarioStore.getDescriptions()).build();
    }

    /** Return specific scenario defined for this router by ID. */
    @GET
    @Path("/{scenarioId}")
    public javax.ws.rs.core.Response getScenario (@PathParam("scenarioId") String scenarioId) {
        Scenario scenario = scenarioStore.scenarios.get(scenarioId);
        if (scenario == null) {
            return javax.ws.rs.core.Response.status(Response.Status.NOT_FOUND).entity(MSG_404).build();
        } else {
            return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity(scenario).build();
        }
    }

    /*@POST
    public javax.ws.rs.core.Response getScenario (
            @QueryParam("coordinates") String coordinates,
            @QueryParam("description") String description) {

        Scenario scenario = scenarioStore.getNewEmptyScenario();
        scenario.description = description;
        AddFrequencyRoute addFreq = new AddFrequencyRoute();
        addFreq.setTransitRoute(coordinates);
        scenario.modifications.add(addFreq);
        return javax.ws.rs.core.Response.status(javax.ws.rs.core.Response.Status.OK).entity(scenario).build();
    }*/

}
