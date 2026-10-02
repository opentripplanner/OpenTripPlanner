package org.opentripplanner.model.plan.itineraryreference;

import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
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
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializer for {@link ItineraryReference}.
 * <p>
 * Each leg reference is encoded with the existing {@link LegReferenceSerializer} and the
 * resulting tokens are joined with {@link #LEG_REFERENCE_DELIMITER}, a character not present in
 * the URL-safe Base64 alphabet {@link LegReferenceSerializer} produces.
 * <p>
 * {@code StreetMode} fields are deliberately encoded as explicit {@code STRING}s (via
 * {@code name()}/{@code valueOf()}), not {@code addEnum()} - see the token framework's own
 * enum-compatibility warning ({@link Token#getEnum}). {@code DurationForEnum} default values use
 * the native {@code DURATION} token type; per-mode overrides are packed into a single
 * deterministic, sorted-by-name {@code STRING} field since the token framework has no native map
 * type.
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
  private static final String BOARD_SLACK_DEFAULT_FIELD = "boardSlackDefault";
  private static final String BOARD_SLACK_OVERRIDES_FIELD = "boardSlackOverrides";
  private static final String ALIGHT_SLACK_DEFAULT_FIELD = "alightSlackDefault";
  private static final String ALIGHT_SLACK_OVERRIDES_FIELD = "alightSlackOverrides";
  private static final String WALK_SPEED_FIELD = "walkSpeed";
  private static final String WALK_RELUCTANCE_FIELD = "walkReluctance";
  private static final String MAX_ACCESS_EGRESS_DURATION_DEFAULT_FIELD =
    "maxAccessEgressDurationDefault";
  private static final String MAX_ACCESS_EGRESS_DURATION_OVERRIDES_FIELD =
    "maxAccessEgressDurationOverrides";
  private static final String WHEELCHAIR_FIELD = "wheelchair";

  private static final String OVERRIDE_ENTRY_DELIMITER = ",";
  private static final String OVERRIDE_KEY_VALUE_DELIMITER = ":";

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
    .addDuration(BOARD_SLACK_DEFAULT_FIELD)
    .addString(BOARD_SLACK_OVERRIDES_FIELD)
    .addDuration(ALIGHT_SLACK_DEFAULT_FIELD)
    .addString(ALIGHT_SLACK_OVERRIDES_FIELD)
    .addString(WALK_SPEED_FIELD)
    .addString(WALK_RELUCTANCE_FIELD)
    .addDuration(MAX_ACCESS_EGRESS_DURATION_DEFAULT_FIELD)
    .addString(MAX_ACCESS_EGRESS_DURATION_OVERRIDES_FIELD)
    .addBoolean(WHEELCHAIR_FIELD)
    .build();

  /** private constructor to prevent instantiating this utility class */
  private ItineraryReferenceSerializer() {}

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
      .withDuration(BOARD_SLACK_DEFAULT_FIELD, ref.boardSlack().defaultValue())
      .withString(BOARD_SLACK_OVERRIDES_FIELD, encodeOverrides(ref.boardSlack(), TransitMode.class))
      .withDuration(ALIGHT_SLACK_DEFAULT_FIELD, ref.alightSlack().defaultValue())
      .withString(
        ALIGHT_SLACK_OVERRIDES_FIELD,
        encodeOverrides(ref.alightSlack(), TransitMode.class)
      )
      .withString(WALK_SPEED_FIELD, Double.toString(ref.walkSpeed()))
      .withString(WALK_RELUCTANCE_FIELD, Double.toString(ref.walkReluctance()))
      .withDuration(
        MAX_ACCESS_EGRESS_DURATION_DEFAULT_FIELD,
        ref.maxAccessEgressDuration().defaultValue()
      )
      .withString(
        MAX_ACCESS_EGRESS_DURATION_OVERRIDES_FIELD,
        encodeOverrides(ref.maxAccessEgressDuration(), StreetMode.class)
      )
      .withBoolean(WHEELCHAIR_FIELD, ref.wheelchair());

    withLocation(builder, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LNG_FIELD, ref.from());
    withLocation(builder, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LNG_FIELD, ref.to());

    return builder.build();
  }

  @Nullable
  public static ItineraryReference decode(@Nullable String itineraryReference) {
    if (itineraryReference == null || itineraryReference.isEmpty()) {
      return null;
    }

    try {
      var token = SCHEMA.decode(itineraryReference);

      String joinedLegReferences = token.getString(LEG_REFERENCES_FIELD).orElseThrow();

      List<LegReference> legReferences = Arrays.stream(
        joinedLegReferences.split(LEG_REFERENCE_DELIMITER, -1)
      )
        .map(LegReferenceSerializer::decode)
        .map(Objects::requireNonNull)
        .toList();

      var from = decodeLocation(token, FROM_STOP_ID_FIELD, FROM_LAT_FIELD, FROM_LNG_FIELD);
      var to = decodeLocation(token, TO_STOP_ID_FIELD, TO_LAT_FIELD, TO_LNG_FIELD);

      var accessMode = StreetMode.valueOf(token.getString(ACCESS_MODE_FIELD).orElseThrow());
      var egressMode = StreetMode.valueOf(token.getString(EGRESS_MODE_FIELD).orElseThrow());
      var transferMode = StreetMode.valueOf(token.getString(TRANSFER_MODE_FIELD).orElseThrow());

      var boardSlack = decodeDurationForEnum(
        TransitMode.class,
        token.getDuration(BOARD_SLACK_DEFAULT_FIELD).orElseThrow(),
        token.getString(BOARD_SLACK_OVERRIDES_FIELD).orElse("")
      );

      var alightSlack = decodeDurationForEnum(
        TransitMode.class,
        token.getDuration(ALIGHT_SLACK_DEFAULT_FIELD).orElseThrow(),
        token.getString(ALIGHT_SLACK_OVERRIDES_FIELD).orElse("")
      );

      var walkSpeed = finiteDouble(token.getString(WALK_SPEED_FIELD).orElseThrow());
      var walkReluctance = finiteDouble(token.getString(WALK_RELUCTANCE_FIELD).orElseThrow());

      var maxAccessEgressDuration = decodeDurationForEnum(
        StreetMode.class,
        token.getDuration(MAX_ACCESS_EGRESS_DURATION_DEFAULT_FIELD).orElseThrow(),
        token.getString(MAX_ACCESS_EGRESS_DURATION_OVERRIDES_FIELD).orElse("")
      );

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
        maxAccessEgressDuration,
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

  private static <E extends Enum<E>> String encodeOverrides(
    DurationForEnum<E> value,
    Class<E> type
  ) {
    return Arrays.stream(type.getEnumConstants())
      .filter(value::isSet)
      .sorted(Comparator.comparing(Enum::name))
      .map(
        e -> e.name() + OVERRIDE_KEY_VALUE_DELIMITER + DurationUtils.durationToStr(value.valueOf(e))
      )
      .collect(Collectors.joining(OVERRIDE_ENTRY_DELIMITER));
  }

  private static <E extends Enum<E>> DurationForEnum<E> decodeDurationForEnum(
    Class<E> type,
    Duration defaultValue,
    String overrides
  ) {
    var builder = DurationForEnum.of(type).withDefault(defaultValue);

    if (!overrides.isEmpty()) {
      for (String entry : overrides.split(OVERRIDE_ENTRY_DELIMITER)) {
        var keyValue = entry.split(OVERRIDE_KEY_VALUE_DELIMITER, 2);
        var key = Enum.valueOf(type, keyValue[0]);
        var duration = DurationUtils.duration(keyValue[1]);
        builder.with(key, duration);
      }
    }

    return builder.build();
  }
}
