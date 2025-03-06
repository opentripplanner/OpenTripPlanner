package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.model.Cost;

class VehicleParkingPreferencesTest {

  private static final Set<String> PREFERRED_TAGS = Set.of("foo");
  private static final Set<String> NOT_PREFERRED_TAGS = Set.of("bar");
  private static final int UNPREFERRED_COST = 360;
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
  void cost() {
    assertEquals(PARKING_COST, subject.cost());
  }

  @Test
  void time() {
    assertEquals(PARKING_TIME, subject.time());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withCost(10).build();
    var same = other.copyOf().withCost(PARKING_COST.toSeconds()).build();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("VehicleParkingPreferences{}", VehicleParkingPreferences.DEFAULT.toString());
    assertEquals(
      "VehicleParkingPreferences{" +
      "unpreferredVehicleParkingTagCost: $360, " +
      "filter: VehicleParkingFilter{not: [tags=[not]], select: [tags=[bar]]}, " +
      "preferred: VehicleParkingFilter{not: [tags=[bar]], select: [tags=[foo]]}, " +
      "cost: $240, " +
      "time: PT2M}",
      subject.toString()
    );
  }

  private static String tagsToString(Set<String> tags) {
    return "[tags=" + tags + "]";
  }

  private VehicleParkingPreferences createPreferences() {
    return VehicleParkingPreferences.of()
      .withPreferredVehicleParkingTags(PREFERRED_TAGS)
      .withNotPreferredVehicleParkingTags(NOT_PREFERRED_TAGS)
      .withUnpreferredVehicleParkingTagCost(UNPREFERRED_COST)
      .withRequiredVehicleParkingTags(REQUIRED_TAGS)
      .withBannedVehicleParkingTags(BANNED_TAGS)
      .withCost(PARKING_COST.toSeconds())
      .withTime(PARKING_TIME)
      .build();
  }
}
