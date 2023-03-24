package org.opentripplanner.ext.legacygraphqlapi;

import java.time.Instant;
import java.util.Locale;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFilterPlaceType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFormFactor;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLInputField;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLRoutingErrorCode;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLTransitMode;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLWheelchairBoarding;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;

public class LegacyGraphQLUtils {

  public static LegacyGraphQLWheelchairBoarding toGraphQL(Accessibility boarding) {
    if (boarding == null) return null;
    return switch (boarding) {
      case NO_INFORMATION -> LegacyGraphQLWheelchairBoarding.NO_INFORMATION;
      case POSSIBLE -> LegacyGraphQLWheelchairBoarding.POSSIBLE;
      case NOT_POSSIBLE -> LegacyGraphQLWheelchairBoarding.NOT_POSSIBLE;
    };
  }

  public static LegacyGraphQLRoutingErrorCode toGraphQL(RoutingErrorCode code) {
    if (code == null) return null;
    return switch (code) {
      case LOCATION_NOT_FOUND -> LegacyGraphQLRoutingErrorCode.LOCATION_NOT_FOUND;
      case NO_STOPS_IN_RANGE -> LegacyGraphQLRoutingErrorCode.NO_STOPS_IN_RANGE;
      case NO_TRANSIT_CONNECTION -> LegacyGraphQLRoutingErrorCode.NO_TRANSIT_CONNECTION;
      case NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW -> LegacyGraphQLRoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
      case OUTSIDE_BOUNDS -> LegacyGraphQLRoutingErrorCode.OUTSIDE_BOUNDS;
      case OUTSIDE_SERVICE_PERIOD -> LegacyGraphQLRoutingErrorCode.OUTSIDE_SERVICE_PERIOD;
      case SYSTEM_ERROR -> LegacyGraphQLRoutingErrorCode.SYSTEM_ERROR;
      case WALKING_BETTER_THAN_TRANSIT -> LegacyGraphQLRoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;
    };
  }

  public static LegacyGraphQLInputField toGraphQL(InputField inputField) {
    if (inputField == null) return null;
    return switch (inputField) {
      case DATE_TIME -> LegacyGraphQLInputField.DATE_TIME;
      case FROM_PLACE -> LegacyGraphQLInputField.FROM;
      case TO_PLACE, INTERMEDIATE_PLACE -> LegacyGraphQLInputField.TO;
    };
  }

  public static LegacyGraphQLTransitMode toGraphQL(TransitMode mode) {
    if (mode == null) return null;
    return switch (mode) {
      case RAIL -> LegacyGraphQLTransitMode.RAIL;
      case COACH -> LegacyGraphQLTransitMode.COACH;
      case SUBWAY -> LegacyGraphQLTransitMode.SUBWAY;
      case BUS -> LegacyGraphQLTransitMode.BUS;
      case TRAM -> LegacyGraphQLTransitMode.TRAM;
      case FERRY -> LegacyGraphQLTransitMode.FERRY;
      case AIRPLANE -> LegacyGraphQLTransitMode.AIRPLANE;
      case CABLE_CAR -> LegacyGraphQLTransitMode.CABLE_CAR;
      case GONDOLA -> LegacyGraphQLTransitMode.GONDOLA;
      case FUNICULAR -> LegacyGraphQLTransitMode.FUNICULAR;
      case TROLLEYBUS -> LegacyGraphQLTransitMode.TROLLEYBUS;
      case MONORAIL -> LegacyGraphQLTransitMode.MONORAIL;
      case CARPOOL -> LegacyGraphQLTransitMode.CARPOOL;
      case TAXI -> LegacyGraphQLTransitMode.TAXI;
    };
  }

  public static RentalFormFactor toModel(LegacyGraphQLFormFactor formFactor) {
    if (formFactor == null) return null;
    return switch (formFactor) {
      case BICYCLE -> RentalFormFactor.BICYCLE;
      case SCOOTER -> RentalFormFactor.SCOOTER;
      case CAR -> RentalFormFactor.CAR;
      case CARGO_BICYCLE -> RentalFormFactor.CARGO_BICYCLE;
      case MOPED -> RentalFormFactor.MOPED;
      case OTHER -> RentalFormFactor.OTHER;
      case SCOOTER_SEATED -> RentalFormFactor.SCOOTER_SEATED;
      case SCOOTER_STANDING -> RentalFormFactor.SCOOTER_STANDING;
    };
  }

  public static PlaceType toModel(LegacyGraphQLFilterPlaceType type) {
    if (type == null) return null;
    return switch (type) {
      case BICYCLE_RENT, VEHICLE_RENT -> PlaceType.VEHICLE_RENT;
      case BIKE_PARK -> PlaceType.BIKE_PARK;
      case CAR_PARK -> PlaceType.CAR_PARK;
      case DEPARTURE_ROW -> PlaceType.PATTERN_AT_STOP;
      case STOP -> PlaceType.STOP;
    };
  }

  /**
   * Convert the UNIX timestamp into an Instant, or return the current time if set to zero.
   */
  public static Instant getTimeOrNow(long epochSeconds) {
    return epochSeconds != 0 ? Instant.ofEpochSecond(epochSeconds) : Instant.now();
  }

  public static boolean startsWith(String str, String name, Locale locale) {
    return str != null && str.toLowerCase(locale).startsWith(name);
  }

  public static boolean startsWith(I18NString str, String name, Locale locale) {
    return str != null && str.toString(locale).toLowerCase(locale).startsWith(name);
  }
}
