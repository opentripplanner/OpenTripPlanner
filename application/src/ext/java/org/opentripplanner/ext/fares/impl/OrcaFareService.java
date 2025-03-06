package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.transit.model.basic.Money.ZERO_USD;
import static org.opentripplanner.transit.model.basic.Money.usDollars;

import com.google.common.collect.Lists;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.model.fare.FareMedium;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.fare.RiderCategory;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

public class OrcaFareService extends DefaultFareService {

  private static final Duration MAX_TRANSFER_DISCOUNT_DURATION = Duration.ofHours(2);

  public static final String COMM_TRANS_AGENCY_ID = "29";
  public static final String COMM_TRANS_FLEX_AGENCY_ID = "4969";
  public static final String KC_METRO_AGENCY_ID = "1";
  public static final String SOUND_TRANSIT_AGENCY_ID = "40";
  public static final String T_LINK_AGENCY_ID = "F1";
  public static final String EVERETT_TRANSIT_AGENCY_ID = "97";
  public static final String PIERCE_COUNTY_TRANSIT_AGENCY_ID = "3";
  public static final String SKAGIT_TRANSIT_AGENCY_ID = "e0e4541a-2714-487b-b30c-f5c6cb4a310f";
  public static final String SEATTLE_STREET_CAR_AGENCY_ID = "23";
  public static final String WASHINGTON_STATE_FERRIES_AGENCY_ID = "95";
  public static final String KITSAP_TRANSIT_AGENCY_ID = "kt";
  public static final String WHATCOM_AGENCY_ID = "14";
  public static final int ROUTE_TYPE_FERRY = 4;
  public static final String FEED_ID = "orca";
  private static final FareMedium ELECTRONIC_MEDIUM = new FareMedium(
    new FeedScopedId(FEED_ID, "electronic"),
    "electronic"
  );
  private static final FareMedium CASH_MEDIUM = new FareMedium(
    new FeedScopedId(FEED_ID, "cash"),
    "cash"
  );

  // TODO: Remove after mar 1
  private static final LocalDate CT_FARE_CHANGE_DATE = LocalDate.of(2025, 3, 1);

  protected enum TransferType {
    ORCA_INTERAGENCY_TRANSFER,
    SAME_AGENCY_TRANSFER,
    NO_TRANSFER,
  }

  protected enum RideType {
    COMM_TRANS_LOCAL_SWIFT,
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
    SOUND_TRANSIT_T_LINK,
    SOUND_TRANSIT_LINK,
    WASHINGTON_STATE_FERRIES,
    WHATCOM_LOCAL,
    WHATCOM_CROSS_COUNTY,
    SKAGIT_LOCAL,
    SKAGIT_CROSS_COUNTY,
    UNKNOWN;

    public TransferType getTransferType(FareType fareType) {
      if (usesOrca(fareType) && this.permitsFreeTransfers()) {
        return TransferType.ORCA_INTERAGENCY_TRANSFER;
      } else if (this == KC_METRO || this == KITSAP_TRANSIT) {
        return TransferType.SAME_AGENCY_TRANSFER;
      }
      return TransferType.NO_TRANSFER;
    }

    /**
     * All transit agencies permit free transfers, apart from these.
     */
    public boolean permitsFreeTransfers() {
      return switch (this) {
        case WASHINGTON_STATE_FERRIES,
          SKAGIT_TRANSIT,
          WHATCOM_LOCAL,
          WHATCOM_CROSS_COUNTY,
          SKAGIT_CROSS_COUNTY -> false;
        default -> true;
      };
    }

    public boolean agencyAcceptsOrca() {
      return switch (this) {
        case WHATCOM_LOCAL, WHATCOM_CROSS_COUNTY, SKAGIT_CROSS_COUNTY, SKAGIT_LOCAL -> false;
        default -> true;
      };
    }
  }

  static class TransferData {

    private Money transferDiscount;
    private ZonedDateTime transferStartTime;

    public Money getTransferDiscount() {
      if (this.transferDiscount == null) {
        return ZERO_USD;
      }
      return this.transferDiscount;
    }

    public Money getDiscountedLegPrice(Leg leg, Money legPrice) {
      if (transferStartTime != null) {
        var inFreeTransferWindow = inFreeTransferWindow(transferStartTime, leg.getStartTime());
        if (inFreeTransferWindow) {
          if (legPrice.greaterThan(transferDiscount)) {
            this.transferStartTime = leg.getStartTime();
            var discountedLegFare = legPrice.minus(this.transferDiscount);
            this.transferDiscount = legPrice;
            return discountedLegFare;
          }
          return Money.ZERO_USD;
        }
      }
      // Start a new transfer
      this.transferDiscount = legPrice;
      this.transferStartTime = leg.getStartTime();
      return legPrice;
    }
  }

  /**
   * Categorizes a leg based on various parameters.
   * The classifications determine the various rules and fares applied to the leg.
   * @param leg Leg to be classified.
   * @return RideType classification
   */
  static RideType getRideType(Leg leg) {
    var agencyId = leg.getAgency().getId().getId();
    var route = leg.getRoute();
    var tripId = leg.getTrip().getId().getId();
    return switch (agencyId) {
      case COMM_TRANS_AGENCY_ID, COMM_TRANS_FLEX_AGENCY_ID -> {
        try {
          int routeId = Integer.parseInt(route.getShortName());
          if (routeId >= 500 && routeId < 600) {
            yield RideType.SOUND_TRANSIT_BUS; // CommTrans operates some ST routes.
          }
          yield RideType.COMM_TRANS_LOCAL_SWIFT;
        } catch (NumberFormatException e) {
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

        if ("973".equals(route.getShortName())) {
          yield RideType.KC_WATER_TAXI_WEST_SEATTLE;
        } else if ("975".equals(route.getShortName())) {
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
          yield RideType.PIERCE_COUNTY_TRANSIT;
        }
      }
      case SOUND_TRANSIT_AGENCY_ID -> RideType.SOUND_TRANSIT;
      case EVERETT_TRANSIT_AGENCY_ID -> RideType.EVERETT_TRANSIT;
      case SKAGIT_TRANSIT_AGENCY_ID -> Set.of("80X", "90X").contains(route.getShortName())
        ? RideType.SKAGIT_CROSS_COUNTY
        : RideType.SKAGIT_LOCAL;
      case SEATTLE_STREET_CAR_AGENCY_ID -> RideType.SEATTLE_STREET_CAR;
      case WASHINGTON_STATE_FERRIES_AGENCY_ID -> RideType.WASHINGTON_STATE_FERRIES;
      case T_LINK_AGENCY_ID -> RideType.SOUND_TRANSIT_T_LINK;
      case KITSAP_TRANSIT_AGENCY_ID -> {
        if (route.getGtfsType() == ROUTE_TYPE_FERRY) {
          // Additional trip id checks are required to distinguish Kitsap fast ferry routes.
          if (tripId.contains("east")) {
            yield RideType.KITSAP_TRANSIT_FAST_FERRY_EASTBOUND;
          } else if (tripId.contains("west")) {
            yield RideType.KITSAP_TRANSIT_FAST_FERRY_WESTBOUND;
          }
        }
        yield RideType.KITSAP_TRANSIT;
      }
      case WHATCOM_AGENCY_ID -> "80X".equals(route.getShortName())
        ? RideType.WHATCOM_CROSS_COUNTY
        : RideType.WHATCOM_LOCAL;
      default -> RideType.UNKNOWN;
    };
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
   * Define which discount fare should be applied based on the fare type. If the ride type is
   * unknown the discount fare can not be applied, use the default fare.
   */
  private Optional<Money> getLegFare(
    FareType fareType,
    RideType rideType,
    Money defaultFare,
    Leg leg
  ) {
    if (rideType == null) {
      return Optional.of(defaultFare);
    }
    // Filter out agencies that don't accept ORCA from the electronic fare type
    if (usesOrca(fareType) && !rideType.agencyAcceptsOrca()) {
      return Optional.empty();
    }
    return switch (fareType) {
      case youth, electronicYouth -> Optional.of(getYouthFare());
      case electronicSpecial -> getLiftFare(rideType, defaultFare, leg);
      case electronicSenior, senior -> getSeniorFare(fareType, rideType, defaultFare, leg);
      case regular, electronicRegular -> getRegularFare(fareType, rideType, defaultFare, leg);
      default -> Optional.of(defaultFare);
    };
  }

  private static Optional<Money> optionalUSD(float amount) {
    return Optional.of(usDollars(amount));
  }

  private static Optional<Money> getCTLocalReducedFare(Leg leg) {
    if (
      leg.getStartTime().isBefore(CT_FARE_CHANGE_DATE.atStartOfDay(leg.getStartTime().getZone()))
    ) {
      return optionalUSD(1.25f);
    } else {
      return optionalUSD(1.00f);
    }
  }

  /**
   * Apply regular discount fares. If the ride type cannot be matched the default fare is used.
   */
  private Optional<Money> getRegularFare(
    FareType fareType,
    RideType rideType,
    Money defaultFare,
    Leg leg
  ) {
    Route route = leg.getRoute();
    if (route == null) {
      return Optional.of(defaultFare);
    }
    return switch (rideType) {
      case KC_WATER_TAXI_VASHON_ISLAND -> usesOrca(fareType)
        ? optionalUSD(5.75f)
        : optionalUSD(6.75f);
      case KC_WATER_TAXI_WEST_SEATTLE -> usesOrca(fareType) ? optionalUSD(5f) : optionalUSD(5.75f);
      case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND -> optionalUSD(2f);
      case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND -> optionalUSD(10f);
      case WASHINGTON_STATE_FERRIES -> Optional.of(
        getWashingtonStateFerriesFare(route.getLongName(), fareType, defaultFare)
      );
      case SOUND_TRANSIT_BUS -> optionalUSD(3.25f);
      case WHATCOM_LOCAL,
        WHATCOM_CROSS_COUNTY,
        SKAGIT_LOCAL,
        SKAGIT_CROSS_COUNTY -> fareType.equals(FareType.electronicRegular)
        ? Optional.empty()
        : Optional.of(defaultFare);
      default -> Optional.of(defaultFare);
    };
  }

  /**
   * Apply Orca lift discount fares based on the ride type.
   */
  private Optional<Money> getLiftFare(RideType rideType, Money defaultFare, Leg leg) {
    var route = leg.getRoute();
    if (route == null) {
      return Optional.of(defaultFare);
    }
    return switch (rideType) {
      case COMM_TRANS_LOCAL_SWIFT -> getCTLocalReducedFare(leg);
      case KC_WATER_TAXI_VASHON_ISLAND -> optionalUSD(4.5f);
      case KC_WATER_TAXI_WEST_SEATTLE -> optionalUSD(3.75f);
      case KC_METRO,
        SOUND_TRANSIT,
        SOUND_TRANSIT_BUS,
        SOUND_TRANSIT_LINK,
        SOUND_TRANSIT_SOUNDER,
        SOUND_TRANSIT_T_LINK,
        KITSAP_TRANSIT,
        EVERETT_TRANSIT,
        PIERCE_COUNTY_TRANSIT,
        SEATTLE_STREET_CAR -> optionalUSD(1.00f);
      case WASHINGTON_STATE_FERRIES -> Optional.of(
        getWashingtonStateFerriesFare(route.getLongName(), FareType.electronicSpecial, defaultFare)
      );
      case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND -> optionalUSD((1f));
      case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND -> optionalUSD((5f));
      case SKAGIT_LOCAL,
        SKAGIT_CROSS_COUNTY,
        WHATCOM_CROSS_COUNTY,
        WHATCOM_LOCAL -> Optional.empty();
      default -> Optional.of(defaultFare);
    };
  }

  /**
   * Apply senior discount fares based on the fare and ride types.
   */
  private Optional<Money> getSeniorFare(
    FareType fareType,
    RideType rideType,
    Money defaultFare,
    Leg leg
  ) {
    var route = leg.getRoute();
    if (route == null) {
      return Optional.of(defaultFare);
    }
    // Many agencies only provide senior discount if using ORCA
    return switch (rideType) {
      case COMM_TRANS_LOCAL_SWIFT -> getCTLocalReducedFare(leg);
      case SKAGIT_TRANSIT, WHATCOM_LOCAL, SKAGIT_LOCAL -> optionalUSD(0.5f);
      case EVERETT_TRANSIT -> optionalUSD(0.5f);
      case KITSAP_TRANSIT_FAST_FERRY_EASTBOUND,
        SOUND_TRANSIT,
        SOUND_TRANSIT_BUS,
        SOUND_TRANSIT_LINK,
        SOUND_TRANSIT_SOUNDER,
        SOUND_TRANSIT_T_LINK,
        KC_METRO,
        PIERCE_COUNTY_TRANSIT,
        SEATTLE_STREET_CAR,
        KITSAP_TRANSIT -> optionalUSD(1f);
      case KC_WATER_TAXI_VASHON_ISLAND -> optionalUSD(3f);
      case KC_WATER_TAXI_WEST_SEATTLE -> optionalUSD(2.5f);
      case KITSAP_TRANSIT_FAST_FERRY_WESTBOUND -> optionalUSD(5f);
      // Discount specific to Skagit transit and not Orca.
      case WASHINGTON_STATE_FERRIES -> Optional.of(
        getWashingtonStateFerriesFare(route.getLongName(), fareType, defaultFare)
      );
      case WHATCOM_CROSS_COUNTY, SKAGIT_CROSS_COUNTY -> Optional.of(defaultFare.half());
      default -> Optional.of(defaultFare);
    };
  }

  /**
   * Apply youth discount fares based on the ride type. Youth ride free in Washington.
   */
  private Money getYouthFare() {
    return Money.ZERO_USD;
  }

  /**
   * Get the washington state ferries fare matching the route long name and fare type. If no match
   * is found, return the default fare.
   */
  private Money getWashingtonStateFerriesFare(
    I18NString routeLongName,
    FareType fareType,
    Money defaultFare
  ) {
    if (routeLongName == null || routeLongName.toString().isEmpty()) {
      return defaultFare;
    }

    return switch (fareType) {
      case youth, electronicYouth -> Money.ZERO_USD;
      case regular, electronicRegular, electronicSpecial -> defaultFare;
      case senior, electronicSenior -> defaultFare.half().roundDownToNearestFiveMinorUnits();
    };
  }

  /**
   * Get the ride price for a single leg. If testing, this class is being called directly so the
   * required agency cash values are not available therefore the default test price is used
   * instead.
   */
  protected Optional<Money> getRidePrice(
    Leg leg,
    FareType fareType,
    Collection<FareRuleSet> fareRules
  ) {
    return calculateCost(fareType, Lists.newArrayList(leg), fareRules);
  }

  /**
   * Calculate the cost of a journey. Where free transfers are not permitted the cash price is used.
   * If free transfers are applicable, the most expensive discount fare across all legs is added to
   * the final cumulative price.
   * <p>
   * The computed fare for Orca card users takes into account real-time trip updates where available,
   * so that, for instance, when a leg on a long itinerary is delayed to begin after the initial two
   * hour window has expired, the calculated fare for that trip will be two one-way fares instead of
   * one.
   */
  @Override
  public ItineraryFares calculateFaresForType(
    Currency currency,
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    var fare = ItineraryFares.empty();
    Money cost = Money.ZERO_USD;
    var orcaFareDiscount = new TransferData();
    HashMap<String, TransferData> perAgencyTransferDiscount = new HashMap<>();

    for (Leg leg : legs) {
      RideType rideType = getRideType(leg);
      assert rideType != null;
      Optional<Money> singleLegPrice = getRidePrice(leg, FareType.regular, fareRules);
      Optional<Money> optionalLegFare = singleLegPrice.flatMap(slp ->
        getLegFare(fareType, rideType, slp, leg)
      );
      if (optionalLegFare.isEmpty()) {
        // If there is no fare for this leg then skip the rest of the logic.
        continue;
      }
      Money legFare = optionalLegFare.get();

      var transferType = rideType.getTransferType(fareType);
      if (transferType == TransferType.ORCA_INTERAGENCY_TRANSFER) {
        // Important to get transfer discount before calculating next leg price
        var transferDiscount = orcaFareDiscount.getTransferDiscount();
        var discountedFare = orcaFareDiscount.getDiscountedLegPrice(leg, legFare);
        addLegFareProduct(
          leg,
          fare,
          fareType,
          discountedFare,
          legFare.greaterThan(ZERO_USD) ? transferDiscount : ZERO_USD
        );
        cost = cost.plus(discountedFare);
      } else if (transferType == TransferType.SAME_AGENCY_TRANSFER) {
        TransferData transferData;
        if (perAgencyTransferDiscount.containsKey(leg.getAgency().getName())) {
          transferData = perAgencyTransferDiscount.get(leg.getAgency().getName());
        } else {
          transferData = new TransferData();
          perAgencyTransferDiscount.put(leg.getAgency().getName(), transferData);
        }
        var transferDiscount = transferData.getTransferDiscount();
        var discountedFare = transferData.getDiscountedLegPrice(leg, legFare);
        addLegFareProduct(
          leg,
          fare,
          fareType,
          discountedFare,
          legFare.greaterThan(ZERO_USD) ? transferDiscount : ZERO_USD
        );
        cost = cost.plus(discountedFare);
      } else {
        // If not using Orca, add the agency's default price for this leg.
        addLegFareProduct(leg, fare, fareType, legFare, Money.ZERO_USD);
        cost = cost.plus(legFare);
      }
    }
    if (cost.fractionalAmount().floatValue() < Float.MAX_VALUE) {
      var fp = FareProduct.of(
        new FeedScopedId(FEED_ID, fareType.name()),
        fareType.name(),
        cost
      ).build();
      fare.addItineraryProducts(List.of(fp));
    }
    return fare;
  }

  /**
   * Adds a leg fare product to the given itinerary fares object
   *
   * @param leg              The leg to create a fareproduct for
   * @param itineraryFares   The itinerary fares to store the fare product in
   * @param fareType         Fare type (split into container and rider category)
   * @param totalFare        Total fare paid after transfer
   * @param transferDiscount Transfer discount applied
   */
  private static void addLegFareProduct(
    Leg leg,
    ItineraryFares itineraryFares,
    FareType fareType,
    Money totalFare,
    Money transferDiscount
  ) {
    var id = new FeedScopedId(FEED_ID, "farePayment");
    var riderCategory = getRiderCategory(fareType);

    FareMedium medium;
    if (usesOrca(fareType)) {
      medium = ELECTRONIC_MEDIUM;
    } else {
      medium = CASH_MEDIUM;
    }
    var duration = Duration.ZERO;
    var fareProduct = new FareProduct(id, "rideCost", totalFare, duration, riderCategory, medium);
    itineraryFares.addFareProduct(leg, fareProduct);
    // If a transfer was used, then also add a transfer fare product.
    if (transferDiscount.isPositive()) {
      var transferFareProduct = new FareProduct(
        id,
        "transfer",
        transferDiscount,
        duration,
        riderCategory,
        medium
      );
      itineraryFares.addFareProduct(leg, transferFareProduct);
    }
  }

  /**
   * In the base class only the rules for a specific feed are selected and then passed to the
   * fare engine, however here we want to explicitly compute fares across feed boundaries.
   */
  @Nullable
  @Override
  protected Collection<FareRuleSet> fareRulesForFeed(FareType fareType, String feedId) {
    return fareRulesPerType.get(fareType);
  }

  /**
   * Disables functionality grouping legs by their feed.
   * This ensures we can calculate transfers between agencies/feeds.
   */
  @Override
  protected Map<String, List<Leg>> fareLegsByFeed(List<Leg> fareLegs) {
    return Map.of(FEED_ID, fareLegs);
  }

  /**
   * Check if trip falls within the transfer time window.
   */
  private static boolean inFreeTransferWindow(
    ZonedDateTime freeTransferStartTime,
    ZonedDateTime currentLegStartTime
  ) {
    // If there is no free transfer, then return false.
    if (freeTransferStartTime == null) return false;
    Duration duration = Duration.between(freeTransferStartTime, currentLegStartTime);
    return duration.compareTo(MAX_TRANSFER_DISCOUNT_DURATION) < 0;
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

  private static RiderCategory getRiderCategory(FareType fareType) {
    var splitFareType = fareType.toString().split("electronic");
    String name;
    if (splitFareType.length > 1) {
      name = splitFareType[1].toLowerCase();
    } else {
      name = fareType.toString();
    }
    return new RiderCategory(new FeedScopedId(FEED_ID, name), name, null);
  }
}
