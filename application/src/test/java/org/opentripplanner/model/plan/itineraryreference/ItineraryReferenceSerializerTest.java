package org.opentripplanner.model.plan.itineraryreference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
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
import org.opentripplanner.street.model.StreetMode;

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
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
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
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
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
      Duration.ofSeconds(60),
      Duration.ofSeconds(30),
      2.5,
      3.25,
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
      Duration.ZERO,
      Duration.ZERO,
      1.3,
      2.0,
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
   * A board/alight slack above what the GTFS API would ever let a client set
   * ({@code requireNonNegativeMax2hours} in {@code TransitPreferencesMapper}) is only possible in
   * a hand-crafted token, and must not reach {@code RouteRequest.buildRequest()} unvalidated - see
   * {@link ItineraryReference}'s class doc.
   */
  @Test
  void boardSlackAboveTwoHoursDecodesToNull() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ofHours(2).plusSeconds(1),
      Duration.ZERO,
      1.3,
      2.0,
      false
    );

    assertNull(ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref)));
  }

  @Test
  void alightSlackAboveTwoHoursDecodesToNull() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ZERO,
      Duration.ofHours(2).plusSeconds(1),
      1.3,
      2.0,
      false
    );

    assertNull(ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref)));
  }

  @Test
  void negativeBoardSlackDecodesToNull() {
    var ref = new ItineraryReference(
      List.of(LEG_A_TO_B),
      null,
      null,
      StreetMode.WALK,
      StreetMode.WALK,
      StreetMode.WALK,
      Duration.ofSeconds(-1),
      Duration.ZERO,
      1.3,
      2.0,
      false
    );

    assertNull(ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref)));
  }

  /**
   * A single {@code itinerary(id:)} call does a lookup, and possibly a transfer street search,
   * per leg reference - an id with an unbounded number of them lets a client make one call do an
   * unbounded amount of work - see {@link ItineraryReference#MAX_LEG_REFERENCES}.
   */
  @Test
  void tooManyLegReferencesDecodesToNull() {
    String validLegToken = LegReferenceSerializer.encode(LEG_A_TO_B);
    String joinedTooManyLegs = String.join(
      "~",
      Collections.nCopies(ItineraryReference.MAX_LEG_REFERENCES + 1, validLegToken)
    );

    String craftedToken = craftedToken(joinedTooManyLegs, null, null, "1.3");

    assertNull(ItineraryReferenceSerializer.decode(craftedToken));
  }

  @Test
  void maximumNumberOfLegReferencesRoundTrips() {
    var ref = defaultReference(
      Collections.nCopies(ItineraryReference.MAX_LEG_REFERENCES, LEG_A_TO_B)
    );

    var decoded = ItineraryReferenceSerializer.decode(ItineraryReferenceSerializer.encode(ref));

    assertEquals(ref, decoded);
  }

  /**
   * Builds a token with otherwise-valid values for every field production uses, and no
   * {@code toStopId}/{@code toLat}/{@code toLon} location - so a test only needs to pass in the
   * one or two fields it wants to make malformed. Field names/order must match
   * {@code ItineraryReferenceSerializer}'s schema exactly. {@code fromLat}/{@code fromLon}/
   * {@code walkSpeed} are parameters (not later overrides) because {@link TokenBuilder} rejects
   * setting the same field twice.
   */
  private static String craftedToken(
    String legReferencesValue,
    String fromLat,
    String fromLon,
    String walkSpeed
  ) {
    var schema = TokenSchema.ofVersion(1)
      .addString("legReferences")
      .addString("fromStopId")
      .addString("fromLat")
      .addString("fromLon")
      .addString("toStopId")
      .addString("toLat")
      .addString("toLon")
      .addString("accessMode")
      .addString("egressMode")
      .addString("transferMode")
      .addDuration("boardSlack")
      .addDuration("alightSlack")
      .addString("walkSpeed")
      .addString("walkReluctance")
      .addBoolean("wheelchair")
      .build();

    return schema
      .encode()
      .withString("legReferences", legReferencesValue)
      .withString("fromStopId", null)
      .withString("fromLat", fromLat)
      .withString("fromLon", fromLon)
      .withString("toStopId", null)
      .withString("toLat", null)
      .withString("toLon", null)
      .withString("accessMode", StreetMode.WALK.name())
      .withString("egressMode", StreetMode.WALK.name())
      .withString("transferMode", StreetMode.WALK.name())
      .withDuration("boardSlack", Duration.ZERO)
      .withDuration("alightSlack", Duration.ZERO)
      .withString("walkSpeed", walkSpeed)
      .withString("walkReluctance", "2.0")
      .withBoolean("wheelchair", false)
      .build();
  }
}
