package org.opentripplanner.ext.legacygraphqlapi;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFilterPlaceType;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFormFactor;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLInputField;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLRoutingErrorCode;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLWheelchairBoarding;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
import org.opentripplanner.transit.model.basic.WheelchairAccessibility;
import org.opentripplanner.util.I18NString;

public class LegacyGraphQLUtils {

  public static Locale getLocale(DataFetchingEnvironment environment) {
    String argLang = environment.getArgument("language");
    if (argLang != null) {
      return Locale.forLanguageTag(argLang);
    }

    Map<String, ?> localContext = environment.getLocalContext();
    if (localContext != null && localContext.get("locale") != null) {
      return (Locale) localContext.get("locale");
    }

    return environment.getLocale();
  }

  public static String getTranslation(I18NString input, DataFetchingEnvironment environment) {
    if (input == null) {
      return null;
    }
    return input.toString(getLocale(environment));
  }

  public static LegacyGraphQLWheelchairBoarding toGraphQL(WheelchairAccessibility boarding) {
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
      case LOCATION_NOT_FOUND -> LegacyGraphQLRoutingErrorCode.locationNotFound;
      case NO_STOPS_IN_RANGE -> LegacyGraphQLRoutingErrorCode.noStopsInRange;
      case NO_TRANSIT_CONNECTION -> LegacyGraphQLRoutingErrorCode.noTransitConnection;
      case NO_TRANSIT_CONNECTION_IN_SEARCH_WINDOW -> LegacyGraphQLRoutingErrorCode.noTransitConnectionInSearchWindow;
      case OUTSIDE_BOUNDS -> LegacyGraphQLRoutingErrorCode.outsideBounds;
      case OUTSIDE_SERVICE_PERIOD -> LegacyGraphQLRoutingErrorCode.outsideServicePeriod;
      case SYSTEM_ERROR -> LegacyGraphQLRoutingErrorCode.systemError;
      case WALKING_BETTER_THAN_TRANSIT -> LegacyGraphQLRoutingErrorCode.walkingBetterThanTransit;
    };
  }

  public static LegacyGraphQLInputField toGraphQL(InputField inputField) {
    return switch (inputField) {
      case DATE_TIME -> LegacyGraphQLInputField.dateTime;
      case FROM_PLACE -> LegacyGraphQLInputField.from;
      case TO_PLACE, INTERMEDIATE_PLACE -> LegacyGraphQLInputField.to;
    };
  }

  public static FormFactor toModel(LegacyGraphQLFormFactor formFactor) {
    if (formFactor == null) return null;
    return switch (formFactor) {
      case BICYCLE -> FormFactor.BICYCLE;
      case SCOOTER -> FormFactor.SCOOTER;
      case CAR -> FormFactor.CAR;
      case MOPED -> FormFactor.MOPED;
      case OTHER -> FormFactor.OTHER;
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
}
