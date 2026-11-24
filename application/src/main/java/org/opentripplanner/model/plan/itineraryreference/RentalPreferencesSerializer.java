package org.opentripplanner.model.plan.itineraryreference;

import static org.opentripplanner.model.plan.itineraryreference.ItineraryReferenceSerializer.finiteDouble;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.framework.token.Token;
import org.opentripplanner.framework.token.TokenBuilder;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.utils.text.CharacterEscapeFormatter;
import org.opentripplanner.utils.time.DurationUtils;

/**
 * Converts the bike, car and scooter rental preferences of an {@link ItineraryReference} to and
 * from tokens of their own. The itinerary reference token holds each of them in one field, so a
 * reference without rental preferences only grows by three empty fields.
 */
final class RentalPreferencesSerializer {

  private static final String SPEED = "speed";
  private static final String RELUCTANCE = "reluctance";
  private static final String OPTIMIZE_TYPE = "optimizeType";
  private static final String TRIANGLE_TIME = "triangleTime";
  private static final String TRIANGLE_SLOPE = "triangleSlope";
  private static final String TRIANGLE_SAFETY = "triangleSafety";
  private static final String WALKING_SPEED = "walkingSpeed";
  private static final String WALKING_RELUCTANCE = "walkingReluctance";
  private static final String MOUNT_DISMOUNT_TIME = "mountDismountTime";
  private static final String MOUNT_DISMOUNT_COST = "mountDismountCost";
  private static final String ALLOWED_NETWORKS = "allowedNetworks";
  private static final String BANNED_NETWORKS = "bannedNetworks";
  private static final String ALLOW_KEEPING = "allowKeeping";
  private static final String KEEPING_COST = "keepingCost";

  private static final TokenSchema BIKE_SCHEMA = TokenSchema.ofVersion(1)
    .addString(SPEED)
    .addString(RELUCTANCE)
    .addString(OPTIMIZE_TYPE)
    .addString(TRIANGLE_TIME)
    .addString(TRIANGLE_SLOPE)
    .addString(TRIANGLE_SAFETY)
    .addString(WALKING_SPEED)
    .addString(WALKING_RELUCTANCE)
    .addDuration(MOUNT_DISMOUNT_TIME)
    .addInt(MOUNT_DISMOUNT_COST)
    .addString(ALLOWED_NETWORKS)
    .addString(BANNED_NETWORKS)
    .addBoolean(ALLOW_KEEPING)
    .addInt(KEEPING_COST)
    .build();

  private static final TokenSchema CAR_SCHEMA = TokenSchema.ofVersion(1)
    .addString(RELUCTANCE)
    .addString(ALLOWED_NETWORKS)
    .addString(BANNED_NETWORKS)
    .build();

  private static final TokenSchema SCOOTER_SCHEMA = TokenSchema.ofVersion(1)
    .addString(SPEED)
    .addString(RELUCTANCE)
    .addString(OPTIMIZE_TYPE)
    .addString(TRIANGLE_TIME)
    .addString(TRIANGLE_SLOPE)
    .addString(TRIANGLE_SAFETY)
    .addString(ALLOWED_NETWORKS)
    .addString(BANNED_NETWORKS)
    .addBoolean(ALLOW_KEEPING)
    .addInt(KEEPING_COST)
    .build();

  /**
   * Network names are joined into one field. Each name ends with a terminator, so an empty set and
   * a set with one empty name are encoded differently.
   */
  private static final char NETWORK_TERMINATOR = ',';
  private static final CharacterEscapeFormatter NETWORK_FORMATTER = new CharacterEscapeFormatter(
    '\\',
    NETWORK_TERMINATOR,
    '+'
  );

  private RentalPreferencesSerializer() {}

  @Nullable
  static String encodeBike(@Nullable BikeRentalPreferences bike) {
    if (bike == null) {
      return null;
    }
    var builder = BIKE_SCHEMA.encode();
    withRiding(builder, bike.riding());
    withWalking(builder, bike.walking());
    withNetworks(builder, bike.networks());
    withDestinationPolicy(builder, bike.destinationPolicy());
    return builder.build();
  }

  static BikeRentalPreferences decodeBike(String value) {
    var token = BIKE_SCHEMA.decode(value);
    return new BikeRentalPreferences(
      decodeRiding(token),
      decodeWalking(token),
      decodeNetworks(token),
      decodeDestinationPolicy(token)
    );
  }

  @Nullable
  static String encodeCar(@Nullable CarRentalPreferences car) {
    if (car == null) {
      return null;
    }
    var builder = CAR_SCHEMA.encode().withString(RELUCTANCE, Double.toString(car.reluctance()));
    withNetworks(builder, car.networks());
    return builder.build();
  }

  static CarRentalPreferences decodeCar(String value) {
    var token = CAR_SCHEMA.decode(value);
    return new CarRentalPreferences(number(token, RELUCTANCE), decodeNetworks(token));
  }

  @Nullable
  static String encodeScooter(@Nullable ScooterRentalPreferences scooter) {
    if (scooter == null) {
      return null;
    }
    var builder = SCOOTER_SCHEMA.encode();
    withRiding(builder, scooter.riding());
    withNetworks(builder, scooter.networks());
    withDestinationPolicy(builder, scooter.destinationPolicy());
    return builder.build();
  }

  static ScooterRentalPreferences decodeScooter(String value) {
    var token = SCOOTER_SCHEMA.decode(value);
    return new ScooterRentalPreferences(
      decodeRiding(token),
      decodeNetworks(token),
      decodeDestinationPolicy(token)
    );
  }

  /**
   * The optimization type is stored as a plain string, not with {@code addEnum()}. See
   * {@link Token#getEnum} for why.
   */
  private static void withRiding(TokenBuilder builder, RidingPreferences riding) {
    var triangle = riding.optimizeTriangle();
    builder
      .withString(SPEED, Double.toString(riding.speed()))
      .withString(RELUCTANCE, Double.toString(riding.reluctance()))
      .withString(OPTIMIZE_TYPE, riding.optimizeType().name())
      .withString(TRIANGLE_TIME, Double.toString(triangle.time()))
      .withString(TRIANGLE_SLOPE, Double.toString(triangle.slope()))
      .withString(TRIANGLE_SAFETY, Double.toString(triangle.safety()));
  }

  private static RidingPreferences decodeRiding(Token token) {
    return new RidingPreferences(
      number(token, SPEED),
      number(token, RELUCTANCE),
      VehicleRoutingOptimizeType.valueOf(token.getString(OPTIMIZE_TYPE).orElseThrow()),
      new TimeSlopeSafetyTriangle(
        number(token, TRIANGLE_TIME),
        number(token, TRIANGLE_SLOPE),
        number(token, TRIANGLE_SAFETY)
      )
    );
  }

  private static void withWalking(TokenBuilder builder, BikeWalkingPreferences walking) {
    builder
      .withString(WALKING_SPEED, Double.toString(walking.speed()))
      .withString(WALKING_RELUCTANCE, Double.toString(walking.reluctance()))
      .withDuration(MOUNT_DISMOUNT_TIME, walking.mountDismountTime())
      .withInt(MOUNT_DISMOUNT_COST, walking.mountDismountCost().toSeconds());
  }

  /** Uses the same limit as the API does for a mount and dismount time sent by a client. */
  private static BikeWalkingPreferences decodeWalking(Token token) {
    return new BikeWalkingPreferences(
      number(token, WALKING_SPEED),
      number(token, WALKING_RELUCTANCE),
      DurationUtils.requireNonNegativeMax30minutes(
        token.getDuration(MOUNT_DISMOUNT_TIME).orElseThrow(),
        "bike mount dismount time"
      ),
      cost(token, MOUNT_DISMOUNT_COST)
    );
  }

  private static void withNetworks(TokenBuilder builder, RentalNetworks networks) {
    builder
      .withString(ALLOWED_NETWORKS, encodeNetworkNames(networks.allowed()))
      .withString(BANNED_NETWORKS, encodeNetworkNames(networks.banned()));
  }

  private static RentalNetworks decodeNetworks(Token token) {
    return new RentalNetworks(
      decodeNetworkNames(token, ALLOWED_NETWORKS),
      decodeNetworkNames(token, BANNED_NETWORKS)
    );
  }

  private static void withDestinationPolicy(
    TokenBuilder builder,
    RentalDestinationPolicy destinationPolicy
  ) {
    builder
      .withBoolean(ALLOW_KEEPING, destinationPolicy.allowKeeping())
      .withInt(KEEPING_COST, destinationPolicy.keepingCost().toSeconds());
  }

  private static RentalDestinationPolicy decodeDestinationPolicy(Token token) {
    return new RentalDestinationPolicy(
      token.getBoolean(ALLOW_KEEPING).orElseThrow(),
      cost(token, KEEPING_COST)
    );
  }

  /** Sorted, so the same networks always give the same id. */
  @Nullable
  private static String encodeNetworkNames(Set<String> networks) {
    if (networks.isEmpty()) {
      return null;
    }
    return networks
      .stream()
      .sorted()
      .map(network -> NETWORK_FORMATTER.encode(network) + NETWORK_TERMINATOR)
      .collect(Collectors.joining());
  }

  private static Set<String> decodeNetworkNames(Token token, String field) {
    var value = token.getString(field).orElse(null);
    if (value == null) {
      return Set.of();
    }
    if (value.charAt(value.length() - 1) != NETWORK_TERMINATOR) {
      throw new IllegalArgumentException("Network names must end with a terminator: " + value);
    }
    return Arrays.stream(
      value.substring(0, value.length() - 1).split(String.valueOf(NETWORK_TERMINATOR), -1)
    )
      .map(NETWORK_FORMATTER::decode)
      .collect(Collectors.toUnmodifiableSet());
  }

  private static double number(Token token, String field) {
    return finiteDouble(token.getString(field).orElseThrow());
  }

  private static Cost cost(Token token, String field) {
    return Cost.costOfSeconds(token.getInt(field).orElseThrow());
  }
}
