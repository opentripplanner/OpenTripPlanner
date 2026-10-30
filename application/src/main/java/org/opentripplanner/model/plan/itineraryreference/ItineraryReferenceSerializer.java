package org.opentripplanner.model.plan.itineraryreference;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.framework.token.Token;
import org.opentripplanner.framework.token.TokenBuilder;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.LegReference;
import org.opentripplanner.model.plan.legreference.LegReferenceSerializer;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializer for {@link ItineraryReference}.
 * <p>
 * Each leg reference is encoded with the existing {@link LegReferenceSerializer} and the
 * resulting tokens are joined with {@link #LEG_REFERENCE_DELIMITER}, a character not present in
 * the URL-safe Base64 alphabet {@link LegReferenceSerializer} produces. Those tokens are already
 * Base64, so the surrounding token encodes them a second time and is roughly a third larger than
 * strictly necessary. Reusing {@link LegReferenceSerializer} is preferred over duplicating its
 * versioned leg format here, which would have to be kept in sync by hand.
 * <p>
 * {@code StreetMode} fields are deliberately encoded as explicit {@code STRING}s (via
 * {@code name()}/{@code valueOf()}), not {@code addEnum()} - see the token framework's own
 * enum-compatibility warning ({@link Token#getEnum}).
 */
public class ItineraryReferenceSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(ItineraryReferenceSerializer.class);

  private static final String LEG_REFERENCES_FIELD = "legReferences";
  private static final String LEG_REFERENCE_DELIMITER = "~";
  private static final String FROM_STOP_ID_FIELD = "fromStopId";
  private static final String FROM_LAT_FIELD = "fromLat";
  private static final String FROM_LNG_FIELD = "fromLng";
  private static final String TO_STOP_ID_FIELD = "toStopId";
  private static final String TO_LAT_FIELD = "toLat";
  private static final String TO_LNG_FIELD = "toLng";
  private static final String ACCESS_MODE_FIELD = "accessMode";
  private static final String EGRESS_MODE_FIELD = "egressMode";
  private static final String TRANSFER_MODE_FIELD = "transferMode";
  private static final String BOARD_SLACK_FIELD = "boardSlack";
  private static final String ALIGHT_SLACK_FIELD = "alightSlack";
  private static final String WALK_SPEED_FIELD = "walkSpeed";
  private static final String WALK_RELUCTANCE_FIELD = "walkReluctance";
  private static final String WHEELCHAIR_FIELD = "wheelchair";

  /**
   * {@link org.opentripplanner.routing.api.request.preference.TransferPreferences}'s own hard
   * ceiling on the number of transfers a search can make; one more leg than that is already more
   * than any real plan response could contain. Caps the work a single {@code itinerary(id:)} call
   * can do, since the refetch does a lookup, and possibly a transfer street search, per leg.
   */
  private static final int MAX_LEG_REFERENCES = 31;

  private static final TokenSchema SCHEMA = TokenSchema.ofVersion(1)
    .addString(LEG_REFERENCES_FIELD)
    .addString(FROM_STOP_ID_FIELD)
    .addString(FROM_LAT_FIELD)
    .addString(FROM_LNG_FIELD)
    .addString(TO_STOP_ID_FIELD)
    .addString(TO_LAT_FIELD)
    .addString(TO_LNG_FIELD)
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

  /**
   * Encodes {@code ref} as an opaque, versioned token. Returns {@code null} for a {@code null}
   * input, never for a valid reference.
   */
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

    withLocation(builder, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LNG_FIELD, ref.from());
    withLocation(builder, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LNG_FIELD, ref.to());

    return builder.build();
  }

  /**
   * Decodes {@code itineraryReference} back into an {@link ItineraryReference}. The input is
   * untrusted client-supplied data: returns {@code null} - rather than throwing - for a
   * {@code null} or empty input, a malformed or truncated token, a token from a different schema
   * version, too many leg references, or a value this method rejects as out of bounds for
   * untrusted input (a non-finite number, or a slack value a client could never have set).
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
      if (rawLegReferences.length > MAX_LEG_REFERENCES) {
        throw new IllegalArgumentException("Too many leg references: " + rawLegReferences.length);
      }

      List<LegReference> legReferences = Arrays.stream(rawLegReferences)
        .map(LegReferenceSerializer::decode)
        .map(Objects::requireNonNull)
        .toList();

      var from = decodeLocation(token, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LNG_FIELD);
      var to = decodeLocation(token, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LNG_FIELD);

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
      LOG.debug("Unable to decode itinerary reference", e);
      return null;
    }
  }

  /**
   * Rejects non-finite values ({@code NaN}, {@code Infinity}, {@code -Infinity}) that
   * {@link Double#parseDouble} would otherwise accept - the token is untrusted client input.
   */
  private static double finiteDouble(String value) {
    double result = Double.parseDouble(value);
    if (!Double.isFinite(result)) {
      throw new IllegalArgumentException("Expected a finite number");
    }
    return result;
  }

  /**
   * Bounds the decoded value to what a real client could ever have asked for, since the id is
   * untrusted input - see {@link ItineraryReference}'s class doc.
   */
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
    String lngField,
    @Nullable GenericLocation location
  ) {
    String stopId = null;
    String lat = null;
    String lng = null;

    if (location != null) {
      var locationStopId = location.stopId();
      if (locationStopId != null) {
        stopId = locationStopId.toString();
      }

      var coordinate = location.wgsCoordinate();
      if (coordinate != null) {
        lat = Double.toString(coordinate.latitude());
        lng = Double.toString(coordinate.longitude());
      }
    }

    builder.withString(stopIdField, stopId).withString(latField, lat).withString(lngField, lng);
  }

  /**
   * Reconstructs a {@code stop}, {@code coordinate}, or {@code stop with fallback coordinate}
   * {@link GenericLocation}, or {@code null} if neither a stop id nor a coordinate is present.
   * On-board {@link org.opentripplanner.routing.api.request.TripLocation}s are never encoded, so
   * are never decoded either.
   */
  @Nullable
  private static GenericLocation decodeLocation(
    Token token,
    String stopIdField,
    String latField,
    String lngField
  ) {
    var stopId = token.getString(stopIdField).map(FeedScopedId::parseStrict).orElse(null);
    var lat = token.getString(latField).map(ItineraryReferenceSerializer::finiteDouble);
    var lng = token.getString(lngField).map(ItineraryReferenceSerializer::finiteDouble);

    if (lat.isPresent() != lng.isPresent()) {
      throw new IllegalArgumentException("Location must contain both latitude and longitude");
    }

    if (lat.isPresent()) {
      double latitude = lat.orElseThrow();
      double longitude = lng.orElseThrow();

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
