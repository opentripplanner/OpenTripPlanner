package org.opentripplanner.model.plan.itineraryreference;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.token.Token;
import org.opentripplanner.framework.token.TokenBuilder;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Converts an {@link ItineraryReference} to and from an opaque token.
 * <p>
 * Each leg reference is encoded with {@link LegReferenceSerializer}, and the results are joined
 * with {@link #LEG_REFERENCE_DELIMITER}. This encodes the leg tokens twice and makes the token a
 * bit longer, but we don't need to copy the leg format here.
 * <p>
 * Street modes are stored as plain strings, not with {@code addEnum()}. See {@link Token#getEnum}
 * for why.
 */
public class ItineraryReferenceSerializer {

  private static final String LEG_REFERENCES_FIELD = "legReferences";
  private static final String LEG_REFERENCE_DELIMITER = "~";
  private static final String FROM_STOP_ID_FIELD = "fromStopId";
  private static final String FROM_LAT_FIELD = "fromLat";
  private static final String FROM_LON_FIELD = "fromLon";
  private static final String TO_STOP_ID_FIELD = "toStopId";
  private static final String TO_LAT_FIELD = "toLat";
  private static final String TO_LON_FIELD = "toLon";
  private static final String ACCESS_MODE_FIELD = "accessMode";
  private static final String EGRESS_MODE_FIELD = "egressMode";
  private static final String TRANSFER_MODE_FIELD = "transferMode";
  private static final String BOARD_SLACK_FIELD = "boardSlack";
  private static final String ALIGHT_SLACK_FIELD = "alightSlack";
  private static final String WALK_SPEED_FIELD = "walkSpeed";
  private static final String WALK_RELUCTANCE_FIELD = "walkReluctance";
  private static final String WHEELCHAIR_FIELD = "wheelchair";

  private static final TokenSchema SCHEMA = TokenSchema.ofVersion(1)
    .addString(LEG_REFERENCES_FIELD)
    .addString(FROM_STOP_ID_FIELD)
    .addString(FROM_LAT_FIELD)
    .addString(FROM_LON_FIELD)
    .addString(TO_STOP_ID_FIELD)
    .addString(TO_LAT_FIELD)
    .addString(TO_LON_FIELD)
    .addString(ACCESS_MODE_FIELD)
    .addString(EGRESS_MODE_FIELD)
    .addString(TRANSFER_MODE_FIELD)
    .addDuration(BOARD_SLACK_FIELD)
    .addDuration(ALIGHT_SLACK_FIELD)
    .addString(WALK_SPEED_FIELD)
    .addString(WALK_RELUCTANCE_FIELD)
    .addBoolean(WHEELCHAIR_FIELD)
    .build();

  /** private constructor to prevent instantiating this utility class */
  private ItineraryReferenceSerializer() {}

  /** Returns the token for {@code ref}, or {@code null} if {@code ref} is {@code null}. */
  @Nullable
  public static String encode(@Nullable ItineraryReference ref) {
    if (ref == null) {
      return null;
    }

    String joinedLegReferences = ref
      .legReferences()
      .stream()
      .map(ItineraryReferenceSerializer::encodeLegReference)
      .collect(Collectors.joining(LEG_REFERENCE_DELIMITER));

    TokenBuilder builder = SCHEMA.encode()
      .withString(LEG_REFERENCES_FIELD, joinedLegReferences)
      .withString(ACCESS_MODE_FIELD, ref.accessMode().name())
      .withString(EGRESS_MODE_FIELD, ref.egressMode().name())
      .withString(TRANSFER_MODE_FIELD, ref.transferMode().name())
      .withDuration(BOARD_SLACK_FIELD, ref.boardSlack())
      .withDuration(ALIGHT_SLACK_FIELD, ref.alightSlack())
      .withString(WALK_SPEED_FIELD, Double.toString(ref.walkSpeed()))
      .withString(WALK_RELUCTANCE_FIELD, Double.toString(ref.walkReluctance()))
      .withBoolean(WHEELCHAIR_FIELD, ref.wheelchair());

    withLocation(builder, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LON_FIELD, ref.from());
    withLocation(builder, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LON_FIELD, ref.to());

    return builder.build();
  }

  /**
   * Reads a token back into an {@link ItineraryReference}. The token comes from the client, so it
   * can be anything. Returns {@code null} instead of throwing if the token is not valid.
   */
  @Nullable
  public static ItineraryReference decode(@Nullable String itineraryReference) {
    if (itineraryReference == null || itineraryReference.isEmpty()) {
      return null;
    }

    try {
      var token = SCHEMA.decode(itineraryReference);

      String joinedLegReferences = token.getString(LEG_REFERENCES_FIELD).orElseThrow();

      String[] rawLegReferences = joinedLegReferences.split(LEG_REFERENCE_DELIMITER, -1);
      if (rawLegReferences.length > ItineraryReference.MAX_LEG_REFERENCES) {
        throw new IllegalArgumentException("Too many leg references: " + rawLegReferences.length);
      }

      List<LegReference> legReferences = Arrays.stream(rawLegReferences)
        .map(LegReferenceSerializer::decode)
        .map(Objects::requireNonNull)
        .toList();

      var from = decodeLocation(token, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LON_FIELD);
      var to = decodeLocation(token, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LON_FIELD);

      var accessMode = StreetMode.valueOf(token.getString(ACCESS_MODE_FIELD).orElseThrow());
      var egressMode = StreetMode.valueOf(token.getString(EGRESS_MODE_FIELD).orElseThrow());
      var transferMode = StreetMode.valueOf(token.getString(TRANSFER_MODE_FIELD).orElseThrow());

      var boardSlack = requireValidSlack(
        token.getDuration(BOARD_SLACK_FIELD).orElseThrow(),
        "board"
      );
      var alightSlack = requireValidSlack(
        token.getDuration(ALIGHT_SLACK_FIELD).orElseThrow(),
        "alight"
      );

      var walkSpeed = finiteDouble(token.getString(WALK_SPEED_FIELD).orElseThrow());
      var walkReluctance = finiteDouble(token.getString(WALK_RELUCTANCE_FIELD).orElseThrow());

      var wheelchair = token.getBoolean(WHEELCHAIR_FIELD).orElseThrow();

      return new ItineraryReference(
        legReferences,
        from,
        to,
        accessMode,
        egressMode,
        transferMode,
        boardSlack,
        alightSlack,
        walkSpeed,
        walkReluctance,
        wheelchair
      );
    } catch (RuntimeException e) {
      return null;
    }
  }

  /** Like {@link Double#parseDouble}, but rejects {@code NaN} and infinity. */
  private static double finiteDouble(String value) {
    double result = Double.parseDouble(value);
    if (!Double.isFinite(result)) {
      throw new IllegalArgumentException("Expected a finite number");
    }
    return result;
  }

  /** Uses the same limit as the API does for slack sent by a client. */
  private static Duration requireValidSlack(Duration value, String subject) {
    return DurationUtils.requireNonNegativeMax2hours(value, subject + " slack");
  }

  private static String encodeLegReference(LegReference legReference) {
    return Objects.requireNonNull(
      LegReferenceSerializer.encode(legReference),
      "Unable to encode leg reference: " + legReference
    );
  }

  private static void withLocation(
    TokenBuilder builder,
    String stopIdField,
    String latField,
    String lonField,
    @Nullable GenericLocation location
  ) {
    String stopId = null;
    String lat = null;
    String lon = null;

    if (location != null) {
      var locationStopId = location.stopId();
      if (locationStopId != null) {
        stopId = locationStopId.toString();
      }

      var coordinate = location.wgsCoordinate();
      if (coordinate != null) {
        lat = Double.toString(coordinate.latitude());
        lon = Double.toString(coordinate.longitude());
      }
    }

    builder.withString(stopIdField, stopId).withString(latField, lat).withString(lonField, lon);
  }

  /**
   * Returns a stop, a coordinate, or a stop with a fallback coordinate. Returns {@code null} if the
   * token has neither.
   */
  @Nullable
  private static GenericLocation decodeLocation(
    Token token,
    String stopIdField,
    String latField,
    String lonField
  ) {
    var stopId = token.getString(stopIdField).map(FeedScopedId::parseStrict).orElse(null);
    var lat = token.getString(latField).map(ItineraryReferenceSerializer::finiteDouble);
    var lon = token.getString(lonField).map(ItineraryReferenceSerializer::finiteDouble);

    if (lat.isPresent() != lon.isPresent()) {
      throw new IllegalArgumentException("Location must contain both latitude and longitude");
    }

    if (lat.isPresent()) {
      double latitude = lat.orElseThrow();
      double longitude = lon.orElseThrow();

      if (stopId != null) {
        return GenericLocation.fromStopIdWithFallback(stopId, latitude, longitude, null);
      }

      return GenericLocation.fromCoordinate(latitude, longitude);
    }

    if (stopId != null) {
      return GenericLocation.fromStopId(stopId);
    }

    return null;
  }
}
