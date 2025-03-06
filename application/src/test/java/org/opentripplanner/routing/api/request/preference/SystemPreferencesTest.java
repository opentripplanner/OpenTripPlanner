package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParameters;
import org.opentripplanner.ext.dataoverlay.api.DataOverlayParametersBuilder;
import org.opentripplanner.ext.dataoverlay.api.ParameterName;
import org.opentripplanner.ext.dataoverlay.api.ParameterType;
import org.opentripplanner.routing.api.request.RoutingTag;

class SystemPreferencesTest {

  private static final Duration MAX_DIRECT = Duration.ofMinutes(10);
  public static final Duration MAX_JOURNEY_DURATION = Duration.ofHours(5);
  public static final RoutingTag TAG_RENTAL = RoutingTag.testCaseCategory("rental");
  public static final DataOverlayParameters DATA_OVERLAY = new DataOverlayParametersBuilder()
    .add(ParameterName.LEAD, ParameterType.PENALTY, 17.3)
    .build();

  private final SystemPreferences subject = SystemPreferences.of()
    .withGeoidElevation(true)
    .withMaxJourneyDuration(MAX_JOURNEY_DURATION)
    .addTags(List.of(TAG_RENTAL))
    .withDataOverlay(DATA_OVERLAY)
    .build();

  @Test
  void copyOf() {}

  @Test
  void tags() {
    assertEquals(Set.of(TAG_RENTAL), subject.tags());
  }

  @Test
  void dataOverlay() {
    assertEquals(DATA_OVERLAY, subject.dataOverlay());
  }

  @Test
  void geoidElevation() {
    assertTrue(subject.geoidElevation());
  }

  @Test
  void maxJourneyDuration() {
    assertEquals(MAX_JOURNEY_DURATION, subject.maxJourneyDuration());
  }

  @Test
  void testOfAndCopyOf() {
    // Return same object if no value is set
    assertSame(SystemPreferences.DEFAULT, SystemPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
  }

  @Test
  void testEqualsAndHashCode() {
    // Create a copy, make a change and set it back again to force creating a new object
    var other = subject.copyOf().withMaxJourneyDuration(Duration.ofHours(2)).build();
    var copy = other.copyOf().withMaxJourneyDuration(MAX_JOURNEY_DURATION).build();
    assertEqualsAndHashCode(subject, other, copy);
  }

  @Test
  void testToString() {
    assertEquals("SystemPreferences{}", SystemPreferences.DEFAULT.toString());
    assertEquals(
      "SystemPreferences{" +
      "tags: [TestCaseCategory: rental], " +
      "dataOverlay: DataOverlayParameters{LEAD_PENALTY: 17.3}, " +
      "geoidElevation, " +
      "maxJourneyDuration: 5h" +
      "}",
      subject.toString()
    );
  }
}
