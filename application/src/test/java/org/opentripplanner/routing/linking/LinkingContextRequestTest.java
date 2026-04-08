package org.opentripplanner.routing.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.street.model.StreetMode;

class LinkingContextRequestTest {

  private static final GenericLocation FROM = GenericLocation.fromCoordinate(1.0, 2.0);
  private static final GenericLocation TO = GenericLocation.fromCoordinate(3.0, 4.0);
  private static final List<GenericLocation> VIA = List.of(
    GenericLocation.fromCoordinate(5.0, 6.0),
    GenericLocation.fromCoordinate(7.0, 8.0)
  );
  private static final StreetMode ACCESS_MODE = StreetMode.WALK;
  private static final StreetMode EGRESS_MODE = StreetMode.BIKE_RENTAL;
  private static final StreetMode DIRECT_MODE = StreetMode.CAR;
  private static final StreetMode TRANSFER_MODE = StreetMode.WALK;

  private final LinkingContextRequest subject = LinkingContextRequest.of()
    .withFrom(FROM)
    .withTo(TO)
    .withViaLocationsWithCoordinates(VIA)
    .withAccessMode(ACCESS_MODE)
    .withEgressMode(EGRESS_MODE)
    .withDirectMode(DIRECT_MODE)
    .withTransferMode(TRANSFER_MODE)
    .build();

  @Test
  void from() {
    assertEquals(FROM, subject.from());
  }

  @Test
  void to() {
    assertEquals(TO, subject.to());
  }

  @Test
  void viaLocationsWithCoordinates() {
    assertEquals(VIA, subject.viaLocationsWithCoordinates());
  }

  @Test
  void accessMode() {
    assertEquals(ACCESS_MODE, subject.accessMode());
  }

  @Test
  void egressMode() {
    assertEquals(EGRESS_MODE, subject.egressMode());
  }

  @Test
  void directMode() {
    assertEquals(DIRECT_MODE, subject.directMode());
  }

  @Test
  void transferMode() {
    assertEquals(TRANSFER_MODE, subject.transferMode());
  }

  @Test
  void testEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(subject, subject.copyOf().build());

    // By changing the speed back and forth we force the builder to create a new instance
    var other = subject.copyOf().withDirectMode(StreetMode.WALK).build();
    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());

    // We modify copy to be equal to subject again
    var copy = other.copyOf().withDirectMode(DIRECT_MODE).build();
    assertEquals(subject, copy);
    assertEquals(subject.hashCode(), copy.hashCode());
  }

  @Test
  void testToString() {
    var defaultRequest = LinkingContextRequest.of().withFrom(GenericLocation.UNKNOWN).build();
    assertEquals("LinkingContextRequest{from: Unknown location}", defaultRequest.toString());
    assertEquals(
      "LinkingContextRequest{" +
        "from: (1.0, 2.0), " +
        "to: (3.0, 4.0), " +
        "viaLocationsWithCoordinates: [(5.0, 6.0), (7.0, 8.0)], " +
        "accessMode: WALK, " +
        "egressMode: BIKE_RENTAL, " +
        "directMode: CAR, " +
        "transferMode: WALK}",
      subject.toString()
    );
  }
}
