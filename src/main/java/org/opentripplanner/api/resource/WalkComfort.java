package org.opentripplanner.api.resource;

import com.fasterxml.jackson.databind.JsonNode;
import org.json.simple.JSONObject;
import org.opentripplanner.common.walk.WalkComfortCalculator;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.standalone.CommandLineParameters;
import org.opentripplanner.standalone.OTPMain;
import org.opentripplanner.standalone.OTPServer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import javax.xml.bind.annotation.XmlRootElement;

import java.io.File;

/**
 * Created by demory on 11/21/17.
 */


@Path("/routers/{routerId}/walk_comfort")
@XmlRootElement
public class WalkComfort {

    @Context
    OTPServer otpServer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public JSONObject recalculateWalkComfort() {

        long startTime = System.currentTimeMillis();

        JsonNode walkConfig = OTPMain.loadJson(new File(otpServer.params.build, WalkComfortCalculator.WALK_CONFIG_FILENAME));
        WalkComfortCalculator gen = new WalkComfortCalculator(walkConfig);

        int edgeCount = 0;
        for (Edge edge : otpServer.getGraphService().getRouter().graph.getEdges()) {
            if(!(edge instanceof StreetEdge)) continue;
            StreetEdge streetEdge = (StreetEdge) edge;
            if(streetEdge.getOsmTags() == null) continue;

            streetEdge.setWalkComfortScore(gen.computeScore(streetEdge.getOsmTags()));
            edgeCount++;
        }
        JSONObject response = new JSONObject();
        response.put("success", true);
        response.put("edgesUpdated", edgeCount);
        response.put("time", System.currentTimeMillis() - startTime);
        return response;
    }
}