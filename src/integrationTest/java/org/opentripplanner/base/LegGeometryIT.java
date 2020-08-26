package org.opentripplanner.base;

import com.google.common.collect.ImmutableList;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.IntegrationTest;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.Leg;
import org.opentripplanner.api.resource.Response;
import org.opentripplanner.util.PolylineEncoder;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class LegGeometryIT extends IntegrationTest {

    @Test
    public void testLegGeometryContinuityWithRenting() {
        List<String> destinations = ImmutableList.of("53.134889, 18.035789", "53.142835, 18.018029", "53.130221, 18.039692", "53.111929, 18.035472", "53.120262, 17.977395");

        for(String toPlace: destinations) {
            javax.ws.rs.core.Response response = target("/routers/bydgoszcz/plan")
                    .queryParam("fromPlace", "53.119934, 17.997763")
                    .queryParam("toPlace", toPlace)
                    .queryParam("locale", "pl")
                    .queryParam("mode", "WALK,TRANSIT,CAR,BICYCLE")
                    .queryParam("vehicleTypesAllowed", "KICKSCOOTER", "MOTORBIKE")
                    .queryParam("startingMode", "WALK")
                    .queryParam("rentingAllowed", "true")
                    .queryParam("time", "15:00:00")
                    .queryParam("date", "07-22-2020")
                    .request().get();

            Response body = response.readEntity(Response.class);


            assertThat(response.getStatus(), equalTo(200));
            for (Itinerary i : body.getPlan().itinerary) {
                assertLegGeometryContinuity(i);
            }
        }
    }

    @Test
    public void testLegGeometryContinuityNoRenting() {
        List<String> destinations = ImmutableList.of("53.1348584,18.0358261", "53.142835, 18.018029", "53.130221, 18.039692", "53.111929, 18.035472", "53.120262, 17.977395");

        for(String toPlace: destinations) {
            javax.ws.rs.core.Response response = target("/routers/bydgoszcz/plan")
                    .queryParam("fromPlace", "53.1146638,17.9870732")
                    .queryParam("toPlace", toPlace)
                    .queryParam("locale", "pl")
                    .queryParam("mode", "WALK,BUS")
                    .queryParam("startingMode", "WALK")
                    .queryParam("rentingAllowed", "false")
                    .queryParam("time", "15:43:35")
                    .queryParam("date", "07-27-2020")
                    .request().get();

            Response body = response.readEntity(Response.class);


            assertThat(response.getStatus(), equalTo(200));
            for (Itinerary i : body.getPlan().itinerary) {
                assertLegGeometryContinuity(i);
            }
        }
    }

    private void assertLegGeometryContinuity(Itinerary itinerary) {
        Coordinate prev, next;
        List<Coordinate> coordinates = PolylineEncoder.decode(itinerary.legs.get(0).legGeometry);
        assertThat(coordinates.isEmpty(), is(false));
        prev = coordinates.get(coordinates.size() - 1);
        if (itinerary.legs.size() >= 2) {
            for (int i = 1; i < itinerary.legs.size(); ++i) {
                coordinates = PolylineEncoder.decode(itinerary.legs.get(i).legGeometry);
                assertThat(coordinates.isEmpty(), is(false));
                next = coordinates.get(0);
                assertThat(prev.distance(next), closeTo(0, 0.001));
                prev = coordinates.get(coordinates.size()-1);
            }
        }
    }
}
