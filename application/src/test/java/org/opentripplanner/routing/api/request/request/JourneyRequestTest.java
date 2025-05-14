package org.opentripplanner.routing.api.request.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner._support.asserts.AssertString.assertEqualsIgnoreWhitespace;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.routing.api.request.RequestModes;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.transit.model.framework.FeedScopedId;

class JourneyRequestTest {

  private static final TransitRequest TRANSIT = TransitRequest.of()
    .withFilter(b ->
      b.addSelect(SelectRequest.of().withRoutes(List.of(new FeedScopedId("F", "R:1"))).build())
    )
    .build();
  private static final StreetRequest ACCESS = new StreetRequest(StreetMode.BIKE_TO_PARK);
  private static final StreetRequest EGRESS = new StreetRequest(StreetMode.SCOOTER_RENTAL);
  private static final StreetRequest TRANSFER = new StreetRequest(StreetMode.BIKE);
  private static final StreetRequest DIRECT = new StreetRequest(StreetMode.CAR);
  private static final StreetRequest WALK = new StreetRequest(StreetMode.WALK);
  private static final RequestModes REQUEST_MODES = RequestModes.of()
    .withAccessMode(StreetMode.SCOOTER_RENTAL)
    .withEgressMode(StreetMode.BIKE_RENTAL)
    .withTransferMode(StreetMode.BIKE)
    .withDirectMode(StreetMode.CAR)
    .build();
  private static final boolean WHEELCHAR = true;

  private final JourneyRequest subject = JourneyRequest.of()
    .withTransit(TRANSIT)
    .withAccess(ACCESS)
    .withEgress(EGRESS)
    .withTransfer(TRANSFER)
    .withDirect(DIRECT)
    .withWheelchair(WHEELCHAR)
    .build();

  @Test
  void transit() {
    assertEquals(TRANSIT, subject.transit());
  }

  @Test
  void access() {
    assertEquals(ACCESS, subject.access());
  }

  @Test
  void egress() {
    assertEquals(EGRESS, subject.egress());
  }

  @Test
  void transfer() {
    assertEquals(TRANSFER, subject.transfer());
  }

  @Test
  void direct() {
    assertEquals(DIRECT, subject.direct());
  }

  @Test
  void wheelchair() {
    assertEquals(WHEELCHAR, subject.wheelchair());
  }

  @Test
  void setAllModes() {
    var subject = JourneyRequest.of().setAllModes(StreetMode.BIKE).build();
    assertEquals(StreetMode.BIKE, subject.access().mode());
    assertEquals(StreetMode.BIKE, subject.egress().mode());
    assertEquals(StreetMode.BIKE, subject.transfer().mode());
    assertEquals(StreetMode.BIKE, subject.direct().mode());
  }

  @Test
  void testMappingInOutOfRequestMode() {
    var subject = JourneyRequest.of().setModes(REQUEST_MODES).build();
    assertEquals(REQUEST_MODES.accessMode, subject.access().mode());
    assertEquals(REQUEST_MODES.egressMode, subject.egress().mode());
    assertEquals(REQUEST_MODES.transferMode, subject.transfer().mode());
    assertEquals(REQUEST_MODES.directMode, subject.direct().mode());
    assertEquals(REQUEST_MODES, subject.modes());
  }

  @Test
  void testEquals() {
    AssertEqualsAndHashCode.verify(subject)
      .sameAs(subject.copyOf().withAccess(WALK).build().copyOf().withAccess(ACCESS).build())
      .differentFrom(
        subject.copyOf().withTransit(TransitRequest.DEFAULT).build(),
        subject.copyOf().withAccess(WALK).build(),
        subject.copyOf().withEgress(WALK).build(),
        subject.copyOf().withTransfer(WALK).build(),
        subject.copyOf().withAccess(WALK).build(),
        subject.copyOf().withWheelchair(!WHEELCHAR).build()
      );
  }

  @Test
  void testToString() {
    assertEqualsIgnoreWhitespace(
      """
      JourneyRequest{
        transit: TransitRequest{filters: [TransitFilterRequest{select: [SelectRequest{transportModes: [], routes: [F:R:1]}]}]},
        access: StreetRequest{mode: BIKE_TO_PARK},
        egress: StreetRequest{mode: SCOOTER_RENTAL},
        transfer:StreetRequest{mode:BIKE},
        direct:StreetRequest{mode:CAR},
        wheelchair
      }
      """,
      subject.toString()
    );
  }
}
