package org.opentripplanner.ext.legacygraphqlapi;

import graphql.schema.DataFetchingEnvironment;
import java.util.Locale;
import java.util.Map;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLFormFactor;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLTypes.LegacyGraphQLWheelchairBoarding;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.vehicle_rental.RentalVehicleType.FormFactor;
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

  public static LegacyGraphQLWheelchairBoarding toGraphQL(WheelChairBoarding boarding) {
    if (boarding == null) return null;
    return switch (boarding) {
      case NO_INFORMATION -> LegacyGraphQLWheelchairBoarding.NO_INFORMATION;
      case POSSIBLE -> LegacyGraphQLWheelchairBoarding.POSSIBLE;
      case NOT_POSSIBLE -> LegacyGraphQLWheelchairBoarding.NOT_POSSIBLE;
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
}
