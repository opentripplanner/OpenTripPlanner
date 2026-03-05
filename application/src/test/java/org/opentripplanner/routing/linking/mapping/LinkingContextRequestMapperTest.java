package org.opentripplanner.routing.linking.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.api.request.via.VisitViaLocation;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.StreetMode;

class LinkingContextRequestMapperTest {

  private static final GenericLocation FROM = new GenericLocation("from", null, 1.0, 0.0);
  private static final GenericLocation TO = new GenericLocation("to", null, 0.0, 1.0);
  private static final List<ViaLocation> VIA = List.of(
    new VisitViaLocation("via", null, List.of(), WgsCoordinate.GREENWICH)
  );
  private static final StreetMode DIRECT_MODE = StreetMode.CAR;
  private static final StreetMode ACCESS_MODE = StreetMode.BIKE_TO_PARK;
  private static final StreetMode EGRESS_MODE = StreetMode.BIKE_RENTAL;
  private static final StreetMode TRANSFER_MODE = StreetMode.WALK;
  private static final RouteRequest REQUEST = RouteRequest.of()
    .withFrom(FROM)
    .withTo(TO)
    .withViaLocations(VIA)
    .withJourney(journey ->
      journey
        .withDirect(new StreetRequest(DIRECT_MODE))
        .withAccess(new StreetRequest(ACCESS_MODE))
        .withEgress(new StreetRequest(EGRESS_MODE))
        .withTransfer(new StreetRequest(TRANSFER_MODE))
    )
    .buildRequest();

  @Test
  void map() {
    var subject = LinkingContextRequestMapper.map(REQUEST);
    assertEquals(FROM, subject.from());
    assertEquals(TO, subject.to());
    assertThat(subject.viaLocationsWithCoordinates()).hasSize(1);
    assertEquals(
      VIA.getFirst().coordinate().map(WgsCoordinate::asJtsCoordinate).get(),
      subject.viaLocationsWithCoordinates().getFirst().getCoordinate()
    );
    assertEquals(DIRECT_MODE, subject.directMode());
    assertEquals(ACCESS_MODE, subject.accessMode());
    assertEquals(EGRESS_MODE, subject.egressMode());
    assertEquals(TRANSFER_MODE, subject.transferMode());
  }
}
