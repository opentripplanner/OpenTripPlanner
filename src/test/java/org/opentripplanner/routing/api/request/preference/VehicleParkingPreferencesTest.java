package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class VehicleParkingPreferencesTest {

  private static final Set<String> PREFERRED_TAGS = Set.of("foo");
  private static final Set<String> NOT_PREFERRED_TAGS = Set.of("bar");
  private static final int UNPREFERRED_COST = 360;
  private static final boolean IGNORE_REALTIME = true;
  private static final Set<String> REQUIRED_TAGS = Set.of("bar");
  private static final Set<String> BANNED_TAGS = Set.of("not");
  private static final Cost PARKING_COST = Cost.costOfMinutes(4);
  private static final Duration PARKING_TIME = Duration.ofMinutes(2);

  private final VehicleParkingPreferences subject = createPreferences();

  @Test
  void preferred() {
    assertEquals(tagsToString(PREFERRED_TAGS), subject.preferred().select().toString());
    assertEquals(tagsToString(NOT_PREFERRED_TAGS), subject.preferred().not().toString());
  }

  @Test
  void filter() {
    assertEquals(tagsToString(REQUIRED_TAGS), subject.filter().select().toString());
    assertEquals(tagsToString(BANNED_TAGS), subject.filter().not().toString());
  }

  @Test
  void unpreferredCost() {
    assertEquals(UNPREFERRED_COST, subject.unpreferredVehicleParkingTagCost().toSeconds());
  }

  @Test
  void useAvailabilityInformation() {
    assertEquals(IGNORE_REALTIME, subject.ignoreRealtimeAvailability());
  }

  @Test
  void parkCost() {
    assertEquals(PARKING_COST, subject.parkCost());
  }

  @Test
  void parkTime() {
    assertEquals(PARKING_TIME, subject.parkTime());
  }

  @Test
  void testEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(VehicleParkingPreferences.DEFAULT, VehicleParkingPreferences.of().build());

    // Create other with different values and same with the same values as the subject
    var other = VehicleParkingPreferences
      .of()
      .withPreferredVehicleParkingTags(Set.of())
      .withNotPreferredVehicleParkingTags(Set.of())
      .withUnpreferredVehicleParkingTagCost(0)
      .withIgnoreRealtimeAvailability(false)
      .withRequiredVehicleParkingTags(Set.of())
      .withBannedVehicleParkingTags(Set.of())
      .withParkCost(0)
      .withParkTime(0)
      .build();
    var same = createPreferences();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("VehicleParkingPreferences{}", VehicleParkingPreferences.DEFAULT.toString());
    assertEquals(
      "VehicleParkingPreferences{" +
      "unpreferredVehicleParkingTagCost: $360, " +
      "useAvailabilityInformation: false, " +
      "filter: VehicleParkingFilterRequest{not: [tags=[not]], select: [tags=[bar]]}, " +
      "preferred: VehicleParkingFilterRequest{not: [tags=[bar]], select: [tags=[foo]]}, " +
      "parkCost: $240, " +
      "parkTime: PT2M}",
      subject.toString()
    );
  }

  private static String tagsToString(Set<String> tags) {
    return "[tags=" + tags + "]";
  }

  private VehicleParkingPreferences createPreferences() {
    return VehicleParkingPreferences
      .of()
      .withPreferredVehicleParkingTags(PREFERRED_TAGS)
      .withNotPreferredVehicleParkingTags(NOT_PREFERRED_TAGS)
      .withUnpreferredVehicleParkingTagCost(UNPREFERRED_COST)
      .withIgnoreRealtimeAvailability(IGNORE_REALTIME)
      .withRequiredVehicleParkingTags(REQUIRED_TAGS)
      .withBannedVehicleParkingTags(BANNED_TAGS)
      .withParkCost(PARKING_COST.toSeconds())
      .withParkTime(PARKING_TIME)
      .build();
  }
}
