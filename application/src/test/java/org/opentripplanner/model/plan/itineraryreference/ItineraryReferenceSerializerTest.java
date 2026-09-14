package org.opentripplanner.model.plan.itineraryreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.core.model.id.FeedScopedIdForTestFactory;
import org.opentripplanner.framework.token.TokenBuilder;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class ItineraryReferenceSerializerTest {

  private static final FeedScopedId TRIP_X_ID = FeedScopedIdForTestFactory.id("Trip X");
  private static final FeedScopedId TRIP_Y_ID = FeedScopedIdForTestFactory.id("Trip Y");
  private static final LocalDate SERVICE_DATE = LocalDate.of(2022, 1, 31);
  private static final FeedScopedId STOP_A_ID = FeedScopedIdForTestFactory.id("Stop A");
  private static final FeedScopedId STOP_B_ID = FeedScopedIdForTestFactory.id("Stop B");
  private static final FeedScopedId STOP_C_ID = FeedScopedIdForTestFactory.id("Stop C");

  private static final ScheduledTransitLegReference LEG_A_TO_B = new ScheduledTransitLegReference(
    TRIP_X_ID,
    SERVICE_DATE,
    0,
    1,
    STOP_A_ID,
    STOP_B_ID,
    null
  );

  private static final ScheduledTransitLegReference LEG_B_TO_C = new ScheduledTransitLegReference(
    TRIP_Y_ID,
    SERVICE_DATE,
    0,
    1,
    STOP_B_ID,
    STOP_C_ID,
    null
  );

  /** A reference with plain, no-override defaults - used where the other fields don't matter. */
  private static ItineraryReference defaultReference(List<LegReference> legReferences) {
    return new ItineraryReference(
      legReferences,
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      DurationForEnum.of(TransitMode.class).build(),
      DurationForEnum.of(TransitMode.class).build(),
      1.3,
      2.0,
      DurationForEnum.of(StreetMode.class).build(),
      false
    );
  }

  @Test
  void roundTripSingleLeg() {
    var ref = defaultReference(List.of(LEG_A_TO_B));

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  @Test
  void roundTripMultipleLegs() {
    var ref = defaultReference(List.of(LEG_A_TO_B, LEG_B_TO_C));

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  @Test
  void roundTripNonWalkTransferModeAndWheelchair() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.BIKE,
      DurationForEnum.of(TransitMode.class).build(),
      DurationForEnum.of(TransitMode.class).build(),
      1.3,
      2.0,
      DurationForEnum.of(StreetMode.class).build(),
      true
    );

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  @Test
  void roundTripFullFieldSet() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B, LEG_B_TO_C),
      GenericLocation.fromStopIdWithFallback(STOP_A_ID, 1.0, 2.0, null),
      GenericLocation.fromCoordinate(3.0, 4.0),
      StreetMode.BIKE,
      StreetMode.CAR,
      StreetMode.WALK,
      DurationForEnum.of(TransitMode.class)
        .withDefaultSec(60)
        .with(TransitMode.BUS, Duration.ofSeconds(90))
        .with(TransitMode.RAIL, Duration.ofSeconds(120))
        .build(),
      DurationForEnum.of(TransitMode.class)
        .withDefaultSec(30)
        .with(TransitMode.FERRY, Duration.ofSeconds(180))
        .build(),
      2.5,
      3.25,
      DurationForEnum.of(StreetMode.class)
        .withDefault(Duration.ofMinutes(20))
        .with(StreetMode.BIKE, Duration.ofMinutes(15))
        .build(),
      true
    );

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  @Test
  void roundTripWithoutFromOrTo() {
    var ref = defaultReference(List.of(LEG_A_TO_B));

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertNotNull(decoded);
    assertEquals(ref, decoded);
    assertNull(decoded.from());
    assertNull(decoded.to());
  }

  @Test
  void roundTripStopOnlyLocation() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B),
      GenericLocation.fromStopId(STOP_A_ID),
      GenericLocation.fromStopId(STOP_B_ID),
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      DurationForEnum.of(TransitMode.class).build(),
      DurationForEnum.of(TransitMode.class).build(),
      1.3,
      2.0,
      DurationForEnum.of(StreetMode.class).build(),
      false
    );

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  @Test
  void nullInputEncodesToNull() {
    assertNull(ItineraryReferenceSerializer.encode(null));
  }

  @Test
  void nullTokenDecodesToNull() {
    assertNull(ItineraryReferenceSerializer.decode(null));
  }

  @Test
  void emptyTokenDecodesToNull() {
    assertNull(ItineraryReferenceSerializer.decode(""));
  }

  @Test
  void malformedTokenDecodesToNull() {
    assertNull(ItineraryReferenceSerializer.decode("this-is-not-a-valid-token::"));
  }

  @Test
  void truncatedTokenDecodesToNull() {
    var encoded = Objects.requireNonNull(
      ItineraryReferenceSerializer.encode(defaultReference(List.of(LEG_A_TO_B)))
    );

    var truncated = encoded.substring(0, encoded.length() / 2);

    assertNull(ItineraryReferenceSerializer.decode(truncated));
  }

  /**
   * A trailing delimiter in the joined leg-references field must not be silently dropped by
   * {@code split}, which would otherwise let a corrupted value decode as if the trailing empty
   * segment didn't exist. Crafts a token with the exact same field shape production uses, with
   * valid values for every other field, so this isolates the delimiter defense rather than
   * failing for an unrelated "missing field" reason.
   */
  @Test
  void malformedNestedLegReferenceDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);
    String joinedWithTrailingDelimiter = validLegToken + "~";

    String craftedToken = craftedToken(joinedWithTrailingDelimiter, null, null, "1.3");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  /**
   * A location with only one of latitude/longitude present must be rejected as malformed, not
   * silently treated as "no coordinate" or "stop only".
   */
  @Test
  void latitudeWithoutLongitudeDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);

    String craftedToken = craftedToken(validLegToken, "1.0", null, "1.3");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  @Test
  void longitudeWithoutLatitudeDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);

    String craftedToken = craftedToken(validLegToken, null, "2.0", "1.3");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  @Test
  void nonFiniteWalkSpeedDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);

    String craftedToken = craftedToken(validLegToken, null, null, "NaN");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  @Test
  void nonFiniteLatitudeDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);

    String craftedToken = craftedToken(validLegToken, "Infinity", "2.0", "1.3");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  /**
   * Builds a token with otherwise-valid values for every field production uses, and no
   * {@code toStopId}/{@code toLat}/{@code toLng} location - so a test only needs to pass in the
   * one or two fields it wants to make malformed. Field names/order must match
   * {@code ItineraryReferenceSerializer}'s schema exactly. {@code fromLat}/{@code fromLng}/
   * {@code walkSpeed} are parameters (not later overrides) because {@link TokenBuilder} rejects
   * setting the same field twice.
   */
  private static String craftedToken(
    String legReferencesValue,
    String fromLat,
    String fromLng,
    String walkSpeed
  ) {
    var schema = TokenSchema.ofVersion(1)
      .addString("legReferences")
      .addString("fromStopId")
      .addString("fromLat")
      .addString("fromLng")
      .addString("toStopId")
      .addString("toLat")
      .addString("toLng")
      .addString("accessMode")
      .addString("egressMode")
      .addString("transferMode")
      .addDuration("boardSlackDefault")
      .addString("boardSlackOverrides")
      .addDuration("alightSlackDefault")
      .addString("alightSlackOverrides")
      .addString("walkSpeed")
      .addString("walkReluctance")
      .addDuration("maxAccessEgressDurationDefault")
      .addString("maxAccessEgressDurationOverrides")
      .addBoolean("wheelchair")
      .build();

    return schema
      .encode()
      .withString("legReferences", legReferencesValue)
      .withString("fromStopId", null)
      .withString("fromLat", fromLat)
      .withString("fromLng", fromLng)
      .withString("toStopId", null)
      .withString("toLat", null)
      .withString("toLng", null)
      .withString("accessMode", StreetMode.WALK.name())
      .withString("egressMode", StreetMode.WALK.name())
      .withString("transferMode", StreetMode.WALK.name())
      .withDuration("boardSlackDefault", Duration.ZERO)
      .withString("boardSlackOverrides", "")
      .withDuration("alightSlackDefault", Duration.ZERO)
      .withString("alightSlackOverrides", "")
      .withString("walkSpeed", walkSpeed)
      .withString("walkReluctance", "2.0")
      .withDuration("maxAccessEgressDurationDefault", Duration.ZERO)
      .withString("maxAccessEgressDurationOverrides", "")
      .withBoolean("wheelchair", false)
      .build();
  }
}
