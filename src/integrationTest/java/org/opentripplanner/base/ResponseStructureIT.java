package org.opentripplanner.base;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.opentripplanner.IntegrationTest;
import org.opentripplanner.api.model.Itinerary;
import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.api.model.VertexType;
import org.opentripplanner.api.resource.Response;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.vehicle_sharing.VehicleType;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;

public class ResponseStructureIT extends IntegrationTest {

    @Test
    public void testResponseStructure() {
        javax.ws.rs.core.Response response = target("/routers/bydgoszcz/plan")
                .queryParam("fromPlace", "53.119934, 17.997763")
                .queryParam("toPlace", "53.142835, 18.018029")
                .queryParam("locale", "pl")
                .queryParam("mode", "WALK,TRANSIT, CAR, BICYCLE")
                .queryParam("startingMode", "WALK")
                .queryParam("rentingAllowed", "true")
                .queryParam("vehicleTypesAllowed", "KICKSCOOTER", "MOTORBIKE")
                .queryParam("time", "15:00:00")
                .queryParam("date", "07-22-2020")
                .request().get();

        Response body = response.readEntity(Response.class);

        assertThat(response.getStatus(), equalTo(200));
        assertRequestParameters(body);
        assertPlan(body.getPlan());
    }

    private void assertRequestParameters(Response response) {
        assertThat(response.getRequestParameters().keySet(), Matchers.containsInAnyOrder("mode", "startingMode", "fromPlace", "toPlace", "locale", "rentingAllowed", "date", "time"));
        assertThat(response.getRequestParameters(), Matchers.hasEntry("mode", "WALK,TRANSIT"));
    }

    private void assertPlan(TripPlan tripPlan) {
        assertThat(tripPlan.from.name, equalTo("Punkt startowy"));
        assertThat(tripPlan.from.lon, equalTo(17.997763));
        assertThat(tripPlan.from.lat, equalTo(53.119934));
        assertThat(tripPlan.from.vertexType, equalTo(VertexType.NORMAL));

        assertThat(tripPlan.to.name, equalTo("Cel podróży"));
        assertThat(tripPlan.to.lon, equalTo(18.018029));
        assertThat(tripPlan.to.lat, equalTo(53.142835));
        assertThat(tripPlan.to.vertexType, equalTo(VertexType.NORMAL));

        assertThat(tripPlan.itinerary.size(), equalTo(3));
        assertItinerary0(tripPlan.itinerary.get(0));
        assertItinerary1(tripPlan.itinerary.get(1));
        assertItinerary2(tripPlan.itinerary.get(2));
    }

    private void assertItinerary2(Itinerary itinerary) {
        assertThat(itinerary.itineraryType, equalTo("WALK+KICKSCOOTER+TRANSIT"));
        assertThat(itinerary.usedNotRecommendedRoute, equalTo(true));

        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.BICYCLE, 2646.726000000001)); // MOTORBIKE is a CAR
        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 134.779));
        assertThat(itinerary.traverseDistance, equalTo(2646.726000000001 + 134.779));

        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.TRANSIT, 240));
        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 121));
        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.BICYCLE, 832));
        assertThat(itinerary.duration, equalTo((long) 240 + 832 + 121));

        assertThat(itinerary.legs.size(), equalTo(4));

        assertThat(itinerary.legs.get(0).mode, equalTo(TraverseMode.WALK));
        assertThat(itinerary.legs.get(0).agencyName, equalTo(null));

        assertThat(itinerary.legs.get(1).mode, equalTo(TraverseMode.BUS));
        assertThat(itinerary.legs.get(1).agencyName, equalTo("ZDMiKP Bydgoszcz"));
        assertThat(itinerary.legs.get(1).routeShortName, equalTo("52"));
        assertThat(itinerary.legs.get(1).routeLongName, equalTo("Błonie — Dworzec Leśne"));
        assertThat(itinerary.legs.get(1).intermediateTransitStops.size(), equalTo(3));

        assertThat(itinerary.legs.get(2).mode, equalTo(TraverseMode.WALK));
        assertThat(itinerary.legs.get(2).agencyName, equalTo(null));

        assertThat(itinerary.legs.get(3).mode, equalTo(TraverseMode.BICYCLE));
        assertThat(itinerary.legs.get(3).vehicleDescription, notNullValue());
        assertThat(itinerary.legs.get(3).vehicleDescription.getVehicleType(), equalTo(VehicleType.KICKSCOOTER));
        assertThat(itinerary.legs.get(3).vehicleDescription.getProvider().getProviderName(), equalTo("Blinkee"));
        assertThat(itinerary.legs.get(3).vehicleDescription.getLatitude(), closeTo(itinerary.legs.get(3).from.lat, 0.001));
        assertThat(itinerary.legs.get(3).vehicleDescription.getLongitude(), closeTo(itinerary.legs.get(3).from.lon, 0.001));

        for (int i = 0; i < itinerary.legs.size() - 1; ++i) {
            assertThat(itinerary.legs.get(i).to, equalTo(itinerary.legs.get(i + 1).from));
        }
    }

    private void assertItinerary1(Itinerary itinerary) {
        assertThat(itinerary.itineraryType, equalTo("WALK+MOTORBIKE"));
        assertThat(itinerary.usedNotRecommendedRoute, equalTo(false)); // the best route?

        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.CAR, 3778.4299999999994)); // MOTORBIKE is a CAR
        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 585.158));
        assertThat(itinerary.traverseDistance, equalTo(3778.4299999999994 + 585.158));

        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.CAR, 942));
        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 470));
        assertThat(itinerary.duration, equalTo((long) 942 + 470));

        assertThat(itinerary.legs.size(), equalTo(2));

        assertThat(itinerary.legs.get(0).mode, equalTo(TraverseMode.WALK));
        assertThat(itinerary.legs.get(0).agencyName, equalTo(null));

        assertThat(itinerary.legs.get(1).mode, equalTo(TraverseMode.CAR));
        assertThat(itinerary.legs.get(1).vehicleDescription, notNullValue());
        assertThat(itinerary.legs.get(1).vehicleDescription.getVehicleType(), equalTo(VehicleType.MOTORBIKE));
        assertThat(itinerary.legs.get(1).vehicleDescription.getProvider().getProviderName(), equalTo("Blinkee"));
        assertThat(itinerary.legs.get(1).vehicleDescription.getLatitude(), closeTo(itinerary.legs.get(1).from.lat, 0.001)); // vehicle position is not exactly on route
        assertThat(itinerary.legs.get(1).vehicleDescription.getLongitude(), closeTo(itinerary.legs.get(1).from.lon, 0.001));

        for (int i = 0; i < itinerary.legs.size() - 1; ++i) {
            assertThat(itinerary.legs.get(i).to, equalTo(itinerary.legs.get(i + 1).from));
        }
    }

    private void assertItinerary0(Itinerary itinerary) {
        assertThat(itinerary.itineraryType, equalTo("WALK+KICKSCOOTER+TRANSIT"));
        assertThat(itinerary.usedNotRecommendedRoute, equalTo(true));

        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.BICYCLE, 2646.726000000001)); // MOTORBIKE is a CAR
        assertThat(itinerary.distanceTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 134.779));
        assertThat(itinerary.traverseDistance, equalTo(2646.726000000001 + 134.779));

        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.BICYCLE, 832));
        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.WALK, 121));
        assertThat(itinerary.timeTraversedInMode, Matchers.hasEntry(TraverseMode.TRANSIT, 240));
        assertThat(itinerary.duration, equalTo((long) (832 + 121 + 240)));

        assertThat(itinerary.legs.size(), equalTo(4));

        assertThat(itinerary.legs.get(0).mode, equalTo(TraverseMode.WALK));
        assertThat(itinerary.legs.get(0).agencyName, equalTo(null));

        assertThat(itinerary.legs.get(1).mode, equalTo(TraverseMode.BUS));
        assertThat(itinerary.legs.get(1).agencyName, equalTo("ZDMiKP Bydgoszcz"));
        assertThat(itinerary.legs.get(1).routeShortName, equalTo("55"));
        assertThat(itinerary.legs.get(1).routeLongName, equalTo("Skorupki — Morska"));
        assertThat(itinerary.legs.get(1).intermediateTransitStops.size(), equalTo(3));

        assertThat(itinerary.legs.get(2).mode, equalTo(TraverseMode.WALK));
        assertThat(itinerary.legs.get(2).agencyName, equalTo(null));

        assertThat(itinerary.legs.get(3).mode, equalTo(TraverseMode.BICYCLE));
        assertThat(itinerary.legs.get(3).vehicleDescription, notNullValue());
        assertThat(itinerary.legs.get(3).vehicleDescription.getVehicleType(), equalTo(VehicleType.KICKSCOOTER));
        assertThat(itinerary.legs.get(3).vehicleDescription.getProvider().getProviderName(), equalTo("Blinkee"));
        assertThat(itinerary.legs.get(3).vehicleDescription.getLatitude(), closeTo(itinerary.legs.get(3).from.lat, 0.001));
        assertThat(itinerary.legs.get(3).vehicleDescription.getLongitude(), closeTo(itinerary.legs.get(3).from.lon, 0.001));

        for (int i = 0; i < itinerary.legs.size() - 1; ++i) {
            assertThat(itinerary.legs.get(i).to, equalTo(itinerary.legs.get(i + 1).from));
        }
    }


}
