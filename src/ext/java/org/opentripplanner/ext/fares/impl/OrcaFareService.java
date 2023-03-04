package org.opentripplanner.ext.fares.impl;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.fares.model.FareContainer;
import org.opentripplanner.ext.fares.model.FareProduct;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.ext.fares.model.RiderCategory;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.routing.core.ItineraryFares;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrcaFareService extends DefaultFareService {

  private static final Logger LOG = LoggerFactory.getLogger(OrcaFareService.class);

  private static final Duration MAX_TRANSFER_DISCOUNT_DURATION = Duration.ofHours(2);

  public static final String COMM_TRANS_AGENCY_ID = "29";
  public static final String KC_METRO_AGENCY_ID = "1";
  public static final String SOUND_TRANSIT_AGENCY_ID = "40";
  public static final String EVERETT_TRANSIT_AGENCY_ID = "97";
  public static final String PIERCE_COUNTY_TRANSIT_AGENCY_ID = "3";
  public static final String SKAGIT_TRANSIT_AGENCY_ID = "e0e4541a-2714-487b-b30c-f5c6cb4a310f";
  public static final String SEATTLE_STREET_CAR_AGENCY_ID = "23";
  public static final String WASHINGTON_STATE_FERRIES_AGENCY_ID = "WSF";
  public static final String KITSAP_TRANSIT_AGENCY_ID = "kt";
  public static final int ROUTE_TYPE_FERRY = 4;

  protected enum RideType {
    COMM_TRANS_LOCAL_SWIFT,
    COMM_TRANS_COMMUTER_EXPRESS,
    EVERETT_TRANSIT,
    KC_WATER_TAXI_VASHON_ISLAND,
    KC_WATER_TAXI_WEST_SEATTLE,
    KC_METRO,
    KITSAP_TRANSIT,
    KITSAP_TRANSIT_FAST_FERRY_EASTBOUND,
    KITSAP_TRANSIT_FAST_FERRY_WESTBOUND,
    PIERCE_COUNTY_TRANSIT,
    SKAGIT_TRANSIT,
    SEATTLE_STREET_CAR,
    SOUND_TRANSIT,
    SOUND_TRANSIT_BUS,
    SOUND_TRANSIT_SOUNDER,
    SOUND_TRANSIT_LINK,
    WASHINGTON_STATE_FERRIES,
    UNKNOWN,
  }

  static RideType getRideType(String agencyId, Route route) {
    return switch (agencyId) {
      case COMM_TRANS_AGENCY_ID -> {
        try {
          int routeId = Integer.parseInt(route.getShortName());
          if (routeId >= 500 && routeId < 600) {
            yield RideType.SOUND_TRANSIT_BUS; // CommTrans operates some ST routes.
          }
          if (routeId >= 400 && routeId <= 899) {
            yield RideType.COMM_TRANS_COMMUTER_EXPRESS;
          }
          yield RideType.COMM_TRANS_LOCAL_SWIFT;
        } catch (NumberFormatException e) {
          LOG.warn("Unable to determine comm trans route id from {}.", route.getShortName(), e);
          yield RideType.COMM_TRANS_LOCAL_SWIFT;
        }
      }
      case KC_METRO_AGENCY_ID -> {
        try {
          int routeId = Integer.parseInt(route.getShortName());
          if (routeId >= 500 && routeId < 600) {
            yield RideType.SOUND_TRANSIT_BUS;
          }
        } catch (NumberFormatException ignored) {
          // Lettered routes exist, are not an error.
        }

        if (
          route.getGtfsType() == ROUTE_TYPE_FERRY &&
          routeLongNameFallBack(route).contains("Water Taxi: West Seattle")
        ) {
          yield RideType.KC_WATER_TAXI_WEST_SEATTLE;
        } else if (
          route.getGtfsType() == ROUTE_TYPE_FERRY &&
          route.getDescription().contains("Water Taxi: Vashon Island")
        ) {
          yield RideType.KC_WATER_TAXI_VASHON_ISLAND;
        }
        yield RideType.KC_METRO;
      }
      case PIERCE_COUNTY_TRANSIT_AGENCY_ID -> {
        try {
          int routeId = Integer.parseInt(route.getShortName());
          if (routeId >= 520 && routeId < 600) {
            // PierceTransit operates some ST routes. But 500 and 501 are PT routes.
            yield RideType.SOUND_TRANSIT_BUS;
          }
          yield RideType.PIERCE_COUNTY_TRANSIT;
        } catch (NumberFormatException e) {
          LOG.warn("Unable to determine comm trans route id from {}.", route.getShortName(), e);
          yield RideType.PIERCE_COUNTY_TRANSIT;
        }
      }
      case SOUND_TRANSIT_AGENCY_ID -> RideType.SOUND_TRANSIT;
      case EVERETT_TRANSIT_AGENCY_ID -> RideType.EVERETT_TRANSIT;
      case SKAGIT_TRANSIT_AGENCY_ID -> RideType.SKAGIT_TRANSIT;
      case SEATTLE_STREET_CAR_AGENCY_ID -> RideType.SEATTLE_STREET_CAR;
      case WASHINGTON_STATE_FERRIES_AGENCY_ID -> RideType.WASHINGTON_STATE_FERRIES;
      case KITSAP_TRANSIT_AGENCY_ID -> RideType.KITSAP_TRANSIT;
      default -> RideType.UNKNOWN;
    };
  }

  private static String routeLongNameFallBack(Route route) {
    var longName = route.getLongName();
    if (longName == null) {
      return "";
    } else {
      return longName.toString();
    }
  }

  public OrcaFareService(Collection<FareRuleSet> regularFareRules) {
    addFareRules(FareType.regular, regularFareRules);
    addFareRules(FareType.senior, regularFareRules);
    addFareRules(FareType.youth, regularFareRules);
    addFareRules(FareType.electronicRegular, regularFareRules);
    addFareRules(FareType.electronicYouth, regularFareRules);
    addFareRules(FareType.electronicSpecial, regularFareRules);
    addFareRules(FareType.electronicSenior, regularFareRules);
  }

  /**
   * Checks a routeShortName against a given string after removing spaces
   */
  private static boolean checkShortName(Route route, String compareString) {
    String cleanCompareString = compareString.replaceAll("-", "").replaceAll(" ", "");
    if (route.getShortName() != null) {
      return route
        .getShortName()
        .replaceAll("-", "")
        .replaceAll(" ", "")
        .equalsIgnoreCase(cleanCompareString);
    } else {
      return false;
    }
  }

  /**
   * Cleans a station name by removing spaces and special phrases.
   */
  private static String cleanStationName(String s) {
    return s
      .replaceAll(" ", "")
      .replaceAll("(Northbound)", "")
      .replaceAll("(Southbound)", "")
      .replaceAll("Station", "")
      .toLowerCase();
  }

  /**
   * Classify the ride type based on the route information provided. In most cases the agency name is sufficient. In
   * some cases the route description and short name are needed to define inner agency ride types. For Kitsap, the
   * route data is enough to define the agency, but addition trip id checks are needed to define the fast ferry direction.
   */
  private static RideType classify(Route route, String tripId) {
    var rideType = getRideType(route.getAgency().getId().getId(), route);
    if (rideType == null) {
      return null;
    }
    if (
      rideType == RideType.KITSAP_TRANSIT &&
      route.getId().getId().equalsIgnoreCase("Kitsap Fast Ferry") &&
      route.getGtfsType() == ROUTE_TYPE_FERRY
    ) {
      // Additional trip id checks are required to distinguish Kitsap fast ferry routes.
      if (tripId.contains("east")) {
        rideType = RideType.KITSAP_TRANSIT_FAST_FERRY_EASTBOUND;
      } else if (tripId.contains("west")) {
        rideType = RideType.KITSAP_TRANSIT_FAST_FERRY_WESTBOUND;
      }
    } else if (rideType == RideType.SOUND_TRANSIT && checkShortName(route, "1 Line")) {
      rideType = RideType.SOUND_TRANSIT_LINK;
    } else if (
      rideType == RideType.SOUND_TRANSIT &&
      (checkShortName(route, "S Line") || checkShortName(route, "N Line"))
    ) {
      rideType = RideType.SOUND_TRANSIT_SOUNDER;
    } else if (rideType == RideType.SOUND_TRANSIT) { //if it isn't Link or Sounder, then...
      rideType = RideType.SOUND_TRANSIT_BUS;
    }
    return rideType;
  }

  /**
   * Define which discount fare should be applied based on the fare type. If the ride type is unknown the discount
   * fare can not be applied, use the default fare.
   */
  private float getLegFare(FareType fareType, RideType rideType, float defaultFare, Leg leg) {
    if (rideType == null) {
      return defaultFare;
    }
    return switch (fareType) {
      case youth, electronicYouth -> getYouthFare();
      case electronicSpecial -> getLiftFare(rideType, defaultFare, leg.getRoute());
      case electronicSenior, senior -> getSeniorFare(
        fareType,
        rideType,
        defaultFare,
        leg.getRoute()
      );
      case regular, electronicRegular -> getRegularFare(fareType, rideType, defaultFare, leg);
      default -> defaultFare;
    };
  }

  /**
   * Apply regular discount fares. If the ride type cannot be matched the default fare is used.
   */
  private float getRegularFare(FareType fareType, RideType rideType, float defaultFare, Leg leg) {
    Route route = leg.getRoute();
    return switch (rideType) {
      case KC_WATER_TAXI_VASHON_ISLAND -> 5.75f;
      case KC_WATER_TAXI_WEST_SEATTLE -> 5.00f;
      case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND -> 2.00f;
      case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND -> 10.00f;
      case WASHINGTON_STATE_FERRIES -> getWashingtonStateFerriesFare(
        route.getLongName(),
        fareType,
        defaultFare
      );
      case SOUND_TRANSIT_LINK, SOUND_TRANSIT_SOUNDER -> getSoundTransitFare(
        leg,
        fareType,
        defaultFare,
        rideType
      );
      case SOUND_TRANSIT_BUS -> 3.25f;
      default -> defaultFare;
    };
  }

  /**
   *  Calculate the correct Link fare from a "ride" including start and end stations.
   */
  private float getSoundTransitFare(
    Leg leg,
    FareType fareType,
    float defaultFare,
    RideType rideType
  ) {
    String start = cleanStationName(leg.getFrom().name.toString());
    String end = cleanStationName(leg.getTo().name.toString());
    // Fares are the same no matter the order of the stations
    // Therefore, the fares DB only contains each station pair once
    // If no match is found, try the reversed order
    String lookupKey = String.format("%s-%s", start, end);
    String reverseLookupKey = String.format("%s-%s", end, start);
    Map<String, Map<FareType, Float>> fareModel = (rideType == RideType.SOUND_TRANSIT_LINK)
      ? OrcaFaresData.linkFares
      : OrcaFaresData.sounderFares;
    Map<FareType, Float> fare = Optional
      .ofNullable(fareModel.get(lookupKey))
      .orElseGet(() -> fareModel.get(reverseLookupKey));

    return (fare != null) ? fare.get(fareType) : defaultFare;
  }

  /**
   * Apply Orca lift discount fares based on the ride type.
   */
  private float getLiftFare(RideType rideType, float defaultFare, Route route) {
    return switch (rideType) {
      case COMM_TRANS_LOCAL_SWIFT -> 1.25f;
      case COMM_TRANS_COMMUTER_EXPRESS -> 2.00f;
      case KC_WATER_TAXI_VASHON_ISLAND -> 4.50f;
      case KC_WATER_TAXI_WEST_SEATTLE -> 3.75f;
      case KITSAP_TRANSIT -> 1.00f;
      case KC_METRO,
        SOUND_TRANSIT,
        SOUND_TRANSIT_BUS,
        SOUND_TRANSIT_LINK,
        SOUND_TRANSIT_SOUNDER,
        EVERETT_TRANSIT,
        SEATTLE_STREET_CAR -> 1.50f;
      case WASHINGTON_STATE_FERRIES -> getWashingtonStateFerriesFare(
        route.getLongName(),
        FareType.electronicSpecial,
        defaultFare
      );
      default -> defaultFare;
    };
  }

  /**
   * Apply senior discount fares based on the fare and ride types.
   */
  private float getSeniorFare(
    FareType fareType,
    RideType rideType,
    float defaultFare,
    Route route
  ) {
    return switch (rideType) {
      case COMM_TRANS_LOCAL_SWIFT -> 1.25f;
      case COMM_TRANS_COMMUTER_EXPRESS -> 2.00f;
      case EVERETT_TRANSIT, SKAGIT_TRANSIT -> 0.50f;
      case PIERCE_COUNTY_TRANSIT, SEATTLE_STREET_CAR, KITSAP_TRANSIT -> fareType.equals( // Pierce, Seattle Streetcar, and Kitsap only provide discounted senior fare for orca.
          FareType.electronicSenior
        )
        ? 1.00f
        : defaultFare;
      case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND -> fareType.equals(FareType.electronicSenior) // Kitsap only provide discounted senior fare for orca.
        ? 1.00f
        : 2.00f;
      case KC_WATER_TAXI_VASHON_ISLAND -> 3.00f;
      case KC_WATER_TAXI_WEST_SEATTLE -> 2.50f;
      case KC_METRO,
        SOUND_TRANSIT,
        SOUND_TRANSIT_BUS,
        SOUND_TRANSIT_LINK,
        SOUND_TRANSIT_SOUNDER -> 1.00f;
      case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND -> fareType.equals(FareType.electronicSenior)
        ? 5.00f
        : 10.00f;
      // Discount specific to Skagit transit and not Orca.
      case WASHINGTON_STATE_FERRIES -> getWashingtonStateFerriesFare(
        route.getLongName(),
        fareType,
        defaultFare
      );
      default -> defaultFare;
    };
  }

  /**
   * Apply youth discount fares based on the ride type.
   * Youth ride free in Washington.
   */
  private float getYouthFare() {
    return 0f;
  }

  /**
   * Get the washington state ferries fare matching the route long name and fare type. If no match is found, return
   * the default fare.
   */
  private float getWashingtonStateFerriesFare(
    I18NString routeLongName,
    FareType fareType,
    float defaultFare
  ) {
    if (routeLongName == null || routeLongName.toString().isEmpty()) {
      return defaultFare;
    }

    var longName = routeLongName.toString().replaceAll(" ", "");

    Map<FareType, Float> fares = OrcaFaresData.washingtonStateFerriesFares.get(longName);
    // WSF doesn't support transfers so we only care about cash fares.
    FareType wsfFareType;
    if (fareType == FareType.electronicRegular) {
      wsfFareType = FareType.regular;
    } else if (fareType == FareType.electronicSenior) {
      wsfFareType = FareType.senior;
    } else if (fareType == FareType.electronicYouth) {
      wsfFareType = FareType.youth;
    } else if (fareType == FareType.electronicSpecial) {
      wsfFareType = FareType.regular;
    } else {
      wsfFareType = fareType;
    }
    // WSF is free in one direction on each route
    // If a fare is not found in the map, we can assume it's free.
    // Route long name is reversed for the reverse direction on a single WSF route
    return (fares != null && fares.get(wsfFareType) != null) ? fares.get(wsfFareType) : 0;
  }

  /**
   * Get the ride price for a single leg. If testing, this class is being called directly so the required agency cash
   * values are not available therefore the default test price is used instead.
   */
  protected float getRidePrice(Leg leg, FareType fareType, Collection<FareRuleSet> fareRules) {
    return calculateCost(fareType, Lists.newArrayList(leg), fareRules);
  }

  /**
   * Calculate the cost of a journey. Where free transfers are not permitted the cash price is used. If free transfers
   * are applicable, the most expensive discount fare across all legs is added to the final cumulative price.
   *
   * The computed fare for Orca card users takes into account realtime trip updates where available, so that, for
   * instance, when a leg on a long itinerary is delayed to begin after the initial two hour window has expired,
   * the calculated fare for that trip will be two one-way fares instead of one.
   */
  @Override
  public boolean populateFare(
    ItineraryFares fare,
    Currency currency,
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    ZonedDateTime freeTransferStartTime = null;
    float cost = 0;
    float orcaFareDiscount = 0;
    for (Leg leg : legs) {
      RideType rideType = classify(leg.getRoute(), leg.getTrip().getId().getId());
      boolean ridePermitsFreeTransfers = permitsFreeTransfers(rideType);
      if (freeTransferStartTime == null && ridePermitsFreeTransfers) {
        // The start of a free transfer must be with a transit agency that permits it!
        freeTransferStartTime = leg.getStartTime();
      }
      float singleLegPrice = getRidePrice(leg, fareType, fareRules);
      float legFare = getLegFare(fareType, rideType, singleLegPrice, leg);
      boolean inFreeTransferWindow = inFreeTransferWindow(
        freeTransferStartTime,
        leg.getStartTime()
      );
      if (hasFreeTransfers(fareType, rideType) && inFreeTransferWindow) {
        // If using Orca (free transfers), the total fare should be equivalent to the
        // most expensive leg of the journey.
        // If the new fare is more than the current ORCA amount, the transfer is extended.
        if (legFare > orcaFareDiscount) {
          freeTransferStartTime = leg.getStartTime();
          // Note: on first leg, discount will be 0 meaning no transfer was applied.
          addLegFareProduct(
            leg,
            fare,
            fareType,
            currency,
            legFare - orcaFareDiscount,
            orcaFareDiscount
          );
          orcaFareDiscount = legFare;
        } else {
          // Ride is free, counts as a transfer if legFare is NOT free
          addLegFareProduct(leg, fare, fareType, currency, 0, legFare != 0 ? orcaFareDiscount : 0);
        }
      } else if (usesOrca(fareType) && !inFreeTransferWindow) {
        // If using Orca and outside of the free transfer window, add the cumulative Orca fare (the maximum leg
        // fare encountered within the free transfer window).
        cost += orcaFareDiscount;

        // Reset the free transfer start time and next Orca fare as needed.
        if (ridePermitsFreeTransfers) {
          // The leg is using a ride type that permits free transfers.
          // The next free transfer window begins at the start time of this leg.
          freeTransferStartTime = leg.getStartTime();
          // Reset the Orca fare to be the fare of this leg.
          orcaFareDiscount = legFare;
        } else {
          // The leg is not using a ride type that permits free transfers.
          // Since there are no free transfers for this leg, increase the total cost by the fare for this leg.
          cost += legFare;
          // The current free transfer window has expired and won't start again until another leg is
          // encountered that does have free transfers.
          freeTransferStartTime = null;
          // The previous Orca fare has been applied to the total cost. Also, the non-free transfer cost has
          // also been applied to the total cost. Therefore, the next Orca cost for the next free-transfer
          // window needs to be reset to 0 so that it is not applied after looping through all rides.
          orcaFareDiscount = 0;
        }
        addLegFareProduct(leg, fare, fareType, currency, legFare, 0);
      } else {
        // If not using Orca, add the agency's default price for this leg.
        addLegFareProduct(leg, fare, fareType, currency, legFare, 0);
        cost += legFare;
      }
    }
    cost += orcaFareDiscount;
    if (cost < Float.POSITIVE_INFINITY) {
      fare.addFare(fareType, getMoney(currency, cost));
    }
    return cost < Float.POSITIVE_INFINITY;
  }

  /**
   * Adds a leg fare product to the given itinerary fares object
   * @param leg The leg to create a fareproduct for
   * @param itineraryFares The itinerary fares to store the fare product in
   * @param fareType Fare type (split into container and rider category)
   * @param currency Fare currency
   * @param totalFare Total fare paid after transfer
   * @param transferDiscount Transfer discount applied
   */
  private static void addLegFareProduct(
    Leg leg,
    ItineraryFares itineraryFares,
    FareType fareType,
    Currency currency,
    float totalFare,
    float transferDiscount
  ) {
    var id = new FeedScopedId("orcaFares", "farePayment");
    var riderCategory = new RiderCategory(
      new FeedScopedId("orca", "orcaFares"),
      getFareCategory(fareType),
      null
    );
    var fareContainer = new FareContainer(
      new FeedScopedId("orca", "orcaFares"),
      usesOrca(fareType) ? "electronic" : "cash"
    );
    var duration = Duration.ZERO;
    var money = new Money(currency, (int) (totalFare * 100));
    var fareProduct = new FareProduct(
      id,
      "rideCost",
      money,
      duration,
      riderCategory,
      fareContainer
    );
    itineraryFares.addFareProduct(leg, fareProduct);
    // If a transfer was used, then also add a transfer fare product.
    if (transferDiscount > 0) {
      var transferDiscountMoney = new Money(currency, (int) (transferDiscount * 100));
      var transferFareProduct = new FareProduct(
        id,
        "transfer",
        transferDiscountMoney,
        duration,
        riderCategory,
        fareContainer
      );
      itineraryFares.addFareProduct(leg, transferFareProduct);
    }
  }

  /**
   * Check if trip falls within the transfer time window.
   * @param freeTransferStartTime
   * @param currentLegStartTime
   */
  private boolean inFreeTransferWindow(
    ZonedDateTime freeTransferStartTime,
    ZonedDateTime currentLegStartTime
  ) {
    // If there is no free transfer, then return false.
    if (freeTransferStartTime == null) return false;
    Duration duration = Duration.between(freeTransferStartTime, currentLegStartTime);
    return duration.compareTo(MAX_TRANSFER_DISCOUNT_DURATION) < 0;
  }

  /**
   * A free transfer can be applied if using Orca and the transit agency permits free transfers.
   */
  private boolean hasFreeTransfers(FareType fareType, RideType rideType) {
    // King County Metro allows transfers on cash fare
    return (
      (permitsFreeTransfers(rideType) && usesOrca(fareType)) ||
      (rideType == RideType.KC_METRO && !usesOrca(fareType))
    );
  }

  /**
   * All transit agencies permit free transfers, apart from these.
   */
  private boolean permitsFreeTransfers(RideType rideType) {
    return switch (rideType) {
      case WASHINGTON_STATE_FERRIES, SKAGIT_TRANSIT -> false;
      default -> true;
    };
  }

  /**
   * Define Orca fare types.
   */
  private static boolean usesOrca(FareType fareType) {
    return (
      fareType.equals(FareType.electronicSpecial) ||
      fareType.equals(FareType.electronicSenior) ||
      fareType.equals(FareType.electronicRegular) ||
      fareType.equals(FareType.electronicYouth)
    );
  }

  private static String getFareCategory(FareType fareType) {
    var splitFareType = fareType.toString().split("electronic");
    if (splitFareType.length > 1) {
      return splitFareType[1].toLowerCase();
    } else {
      return fareType.toString();
    }
  }
}
