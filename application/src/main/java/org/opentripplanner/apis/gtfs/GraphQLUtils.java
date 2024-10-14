package org.opentripplanner.apis.gtfs;

import java.time.Instant;
import java.util.Locale;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLFilterPlaceType;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLFormFactor;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLInputField;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLRoutingErrorCode;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLTransitMode;
import org.opentripplanner.apis.gtfs.generated.GraphQLTypes.GraphQLWheelchairBoarding;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.basic.TransitMode;

public class GraphQLUtils {

  public static GraphQLWheelchairBoarding toGraphQL(Accessibility boarding) {
    if (boarding == null) return null;
    return switch (boarding) {
      case NO_INFORMATION -> GraphQLWheelchairBoarding.NO_INFORMATION;
      case POSSIBLE -> GraphQLWheelchairBoarding.POSSIBLE;
      case NOT_POSSIBLE -> GraphQLWheelchairBoarding.NOT_POSSIBLE;
    };
  }

  public static GraphQLRoutingErrorCode toGraphQL(RoutingErrorCode code) {
    if (code == null) return null;
    return switch (code) {
      case LOCATION_NOT_FOUND -> GraphQLRoutingErrorCode.LOCATION_NOT_FOUND;
      case NO_STOPS_IN_RANGE -> GraphQLRoutingErrorCode.NO_STOPS_IN_RANGE;
      case NO_TRANSIT_CONNECTION -> GraphQLRoutingErrorCode.NO_TRANSIT_CONNECTION;
      case NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW -> GraphQLRoutingErrorCode.NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW;
      case OUTSIDE_BOUNDS -> GraphQLRoutingErrorCode.OUTSIDE_BOUNDS;
      case OUTSIDE_SERVICE_PERIOD -> GraphQLRoutingErrorCode.OUTSIDE_SERVICE_PERIOD;
      case WALKING_BETTER_THAN_TRANSIT -> GraphQLRoutingErrorCode.WALKING_BETTER_THAN_TRANSIT;
    };
  }

  public static GraphQLInputField toGraphQL(InputField inputField) {
    if (inputField == null) return null;
    return switch (inputField) {
      case DATE_TIME -> GraphQLInputField.DATE_TIME;
      case FROM_PLACE -> GraphQLInputField.FROM;
      case TO_PLACE, INTERMEDIATE_PLACE -> GraphQLInputField.TO;
    };
  }

  public static GraphQLTransitMode toGraphQL(TransitMode mode) {
    if (mode == null) return null;
    return switch (mode) {
      case RAIL -> GraphQLTransitMode.RAIL;
      case COACH -> GraphQLTransitMode.COACH;
      case SUBWAY -> GraphQLTransitMode.SUBWAY;
      case BUS -> GraphQLTransitMode.BUS;
      case TRAM -> GraphQLTransitMode.TRAM;
      case FERRY -> GraphQLTransitMode.FERRY;
      case AIRPLANE -> GraphQLTransitMode.AIRPLANE;
      case CABLE_CAR -> GraphQLTransitMode.CABLE_CAR;
      case GONDOLA -> GraphQLTransitMode.GONDOLA;
      case FUNICULAR -> GraphQLTransitMode.FUNICULAR;
      case TROLLEYBUS -> GraphQLTransitMode.TROLLEYBUS;
      case MONORAIL -> GraphQLTransitMode.MONORAIL;
      case CARPOOL -> GraphQLTransitMode.CARPOOL;
      case TAXI -> GraphQLTransitMode.TAXI;
    };
  }

  public static RentalFormFactor toModel(GraphQLFormFactor formFactor) {
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

  public static PlaceType toModel(GraphQLFilterPlaceType type) {
    if (type == null) return null;
    return switch (type) {
      case BICYCLE_RENT, VEHICLE_RENT -> PlaceType.VEHICLE_RENT;
      case BIKE_PARK -> PlaceType.BIKE_PARK;
      case CAR_PARK -> PlaceType.CAR_PARK;
      case DEPARTURE_ROW -> PlaceType.PATTERN_AT_STOP;
      case STOP -> PlaceType.STOP;
      case STATION -> PlaceType.STATION;
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
