package org.opentripplanner.base;

import org.junit.Test;
import org.opentripplanner.IntegrationTest;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.resource.Response;

import static org.junit.Assert.assertEquals;

public class ResponseStructureIT extends IntegrationTest {


    @Test
    public void testWalkAndTransit() {
        javax.ws.rs.core.Response response = target("/routers/bydgoszcz/plan")
                .queryParam("fromPlace", "53.134802,17.991995")
                .queryParam("toPlace", "53.122338,18.0098715")
                .queryParam("locale", "pl")
                .queryParam("mode", "WALK,TRANSIT")
                .queryParam("startingMode", "WALK")
                .queryParam("softWalkLimit", "false")
                .queryParam("rentingAllowed", "true")
                .request().get();

        Response body = response.readEntity(Response.class);

        assertEquals("should return status 200", 200, response.getStatus());
        assertEquals("should return 3 itenaries", 3, body.getPlan().itinerary.size());
        for(Itinerary it: body.getPlan().itinerary) {
            assertEquals("test Itinerary type", "WALK+TRANSIT", it.itineraryType);
        }
    }

    @Test
    public void testOnlyWalkMode() {
        javax.ws.rs.core.Response response = target("/routers/bydgoszcz/plan")
                .queryParam("fromPlace", "53.134802,17.991995")
                .queryParam("toPlace", "53.122338,18.0098715")
                .queryParam("locale", "pl")
                .queryParam("mode", "WALK")
                .queryParam("maxWalkDistance", "999999")
                .queryParam("startingMode", "WALK")
                .queryParam("softWalkLimit", "false")
                .request().get();
        Response body = response.readEntity(Response.class);

        assertEquals("should return status 200", 200, response.getStatus());
        assertEquals("should return 3 tracks", 1, body.getPlan().itinerary.size());
        assertEquals("test Itinerary type", "WALK", body.getPlan().itinerary.get(0).itineraryType);

    }
}
