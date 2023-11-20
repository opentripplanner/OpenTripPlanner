package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.util.Set;
import org.junit.jupiter.api.Test;

class ParkingPreferencesTest {

  private static final Set<String> PREFERRED_TAGS = Set.of("foo");
  private static final Set<String> NOT_PREFERRED_TAGS = Set.of("bar");
  private static final int UNPREFERRED_COST = 300;
  private static final boolean REALTIME = false;
  private static final Set<String> REQUIRED_TAGS = Set.of("bar");
  private static final Set<String> BANNED_TAGS = Set.of("not");

  private final ParkingPreferences subject = createPreferences();

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
    assertEquals(REALTIME, subject.useAvailabilityInformation());
  }

  @Test
  void testCopyOfEqualsAndHashCode() {
    // Return same object if no value is set
    assertSame(ParkingPreferences.DEFAULT, ParkingPreferences.of().build());

    // Create a copy, make a change and set it back again to force creating a new object
    var other = ParkingPreferences
      .of()
      .withPreferredVehicleParkingTags(Set.of())
      .withNotPreferredVehicleParkingTags(Set.of())
      .withUnpreferredVehicleParkingTagCost(0)
      .withUseAvailabilityInformation(true)
      .withRequiredVehicleParkingTags(Set.of())
      .withBannedVehicleParkingTags(Set.of())
      .build();
    var same = createPreferences();
    assertEqualsAndHashCode(subject, other, same);
  }

  @Test
  void testToString() {
    assertEquals("ParkingPreferences{}", ParkingPreferences.DEFAULT.toString());
    assertEquals(
      "ParkingPreferences{" +
      "useAvailabilityInformation: false, " +
      "filter: VehicleParkingFilterRequest{not: [tags=[not]], select: [tags=[bar]]}, " +
      "preferred: VehicleParkingFilterRequest{not: [tags=[bar]], select: [tags=[foo]]}}",
      subject.toString()
    );
  }

  private static String tagsToString(Set<String> tags) {
    return "[tags=" + tags + "]";
  }

  private ParkingPreferences createPreferences() {
    return ParkingPreferences
      .of()
      .withPreferredVehicleParkingTags(PREFERRED_TAGS)
      .withNotPreferredVehicleParkingTags(NOT_PREFERRED_TAGS)
      .withUnpreferredVehicleParkingTagCost(UNPREFERRED_COST)
      .withUseAvailabilityInformation(REALTIME)
      .withRequiredVehicleParkingTags(REQUIRED_TAGS)
      .withBannedVehicleParkingTags(BANNED_TAGS)
      .build();
  }
}
