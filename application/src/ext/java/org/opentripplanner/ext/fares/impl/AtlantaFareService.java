package org.opentripplanner.ext.fares.impl;

import static org.opentripplanner.transit.model.basic.Money.ZERO_USD;
import static org.opentripplanner.transit.model.basic.Money.usDollars;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.fare.ItineraryFare;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;

public class AtlantaFareService extends DefaultFareService {

  private static final ZoneId NEW_YORK_ZONE_ID = ZoneId.of("America/New_York");
  public static final String COBB_AGENCY_ID = "2";
  public static final String XPRESS_AGENCY_ID = "6";
  public static final String MARTA_AGENCY_ID = "5";
  public static final String GCT_AGENCY_ID = "4";
  public static final Set<String> COBB_FREE_RIDE_SHORT_NAMES = Set.of("blue", "green");
  private static final String FEED_ID = "atlanta";

  private enum TransferType {
    END_TRANSFER, // Ends this transfer entirely.
    NO_TRANSFER, // Effectively no transfer, but don't invalidate this transfer
    FREE_TRANSFER, // Transfer is free
    TRANSFER_WITH_UPCHARGE, // Transfer has a set upcharge
    TRANSFER_PAY_DIFFERENCE, // Transfer pays difference between default fares
  }

  private enum RideType {
    FREE_RIDE,
    MARTA,
    COBB_LOCAL,
    COBB_EXPRESS("100", "101", "102"),
    GCT_LOCAL,
    GCT_EXPRESS_Z1("102", "103a", "110", "swpr"),
    GCT_EXPRESS_Z2("101", "103"),
    XPRESS_MORNING,
    XPRESS_AFTERNOON,
    STREETCAR("atlsc");

    private final Set<String> routeNames;

    RideType(String... members) {
      this.routeNames = Arrays.stream(members).map(String::toLowerCase).collect(Collectors.toSet());
    }

    public boolean routeNamesContains(@Nullable String s) {
      if (s == null) {
        return false;
      }
      return routeNames.contains(s.toLowerCase());
    }
  }

  private static class ATLTransfer {

    List<Leg> legs = new ArrayList<>();
    Multimap<FareType, Money> fares = ArrayListMultimap.create();
    final FareType fareType;
    final Currency currency;
    Money lastFareWithTransfer;
    int maxRides;
    Duration transferWindow;

    public ATLTransfer(Currency currency, FareType fareType) {
      this.fareType = fareType;
      this.currency = currency;
    }

    /**
     * Adds a leg to this transfer.
     * @param leg Ride to be added
     * @param defaultFare Default fare to use for transfer calculations (usually from GTFS)
     * @return Whether the added ride is valid or not. If invalid, then this transfer has ended and a new one is needed for the ride.
     */
    public boolean addLeg(Leg leg, Money defaultFare) {
      // A transfer will always contain at least one ride.
      RideType toRideType = classify(leg);
      if (legs.size() == 0) {
        legs.add(leg);
        fares.put(fareType, defaultFare);
        lastFareWithTransfer = defaultFare;
        maxRides = getMaxTransfers(toRideType);
        transferWindow = getTransferWindow(toRideType);
        return true;
      }

      Leg latestRide = legs.get(legs.size() - 1);
      // TODO: Potential problem if the first trip of a transfer is a pay on exit?
      var transferStartTime = legs.get(0).startTime();
      RideType fromRideType = classify(latestRide);
      TransferMeta transferClassification = classifyTransfer(
        toRideType,
        fromRideType,
        this.fareType
      );

      var transferUseTime = transferClassification.payOnExit ? leg.endTime() : leg.startTime();

      // If transfer is NO_TRANSFER, it will not have a window or maxTransfers set,
      // so we only check if it's valid if the transfer is going to be used.
      if (!transferClassification.type.equals(TransferType.NO_TRANSFER)) {
        // Consider the conditions under which this transfer will no longer be valid.
        if (transferClassification.type.equals(TransferType.END_TRANSFER)) {
          return false;
        } else if (transferUseTime.isAfter(transferStartTime.plus(transferWindow))) {
          return false;
        } else if (legs.size() > maxRides) {
          return false;
        }
      }

      if (transferClassification.type.equals(TransferType.NO_TRANSFER)) {
        fares.put(fareType, defaultFare);
        // Full fare is charged, but transfer is still valid.
        // Ride is not added to rides list since it doesn't count towards transfer limit.
        // NOTE: Rides and fares list will not always be in sync because of this.
        return true;
      }

      // All conditions below this point "use" the transfer, so we add the ride.
      legs.add(leg);
      if (transferClassification.type.equals(TransferType.FREE_TRANSFER)) {
        fares.put(fareType, Money.ofFractionalAmount(currency, 0));
        lastFareWithTransfer = defaultFare;
        return true;
      } else if (transferClassification.type.equals(TransferType.TRANSFER_PAY_DIFFERENCE)) {
        Money newCost = Money.ZERO_USD;
        if (defaultFare.greaterThan(lastFareWithTransfer)) {
          newCost = defaultFare.minus(lastFareWithTransfer);
        }
        fares.put(fareType, newCost);
        lastFareWithTransfer = defaultFare;
        return true;
      } else if (transferClassification.type.equals(TransferType.TRANSFER_WITH_UPCHARGE)) {
        fares.put(fareType, transferClassification.upcharge);
        lastFareWithTransfer = defaultFare;
        return true;
      }
      return true;
    }

    public Money getTotal() {
      return fares.get(fareType).stream().reduce(ZERO_USD, Money::plus);
    }
  }

  /**
   * Get the leg price for a single leg. If testing, this class is being called directly so the required agency cash
   * values are not available therefore the default test price is used instead.
   */
  protected Money getLegPrice(Leg leg, FareType fareType, Collection<FareRuleSet> fareRules) {
    return calculateCost(fareType, Lists.newArrayList(leg), fareRules).orElse(Money.ZERO_USD);
  }

  private static class TransferMeta {

    public final TransferType type;
    public final Money upcharge;
    public final boolean payOnExit;

    /**
     * Create a TransferMeta
     * @param type Type of transfer
     * @param upcharge Upcharge for the transfer in cents
     * @param payOnExit Whether the fare is charged at end of leg
     */
    public TransferMeta(TransferType type, Money upcharge, boolean payOnExit) {
      this.type = type;
      this.upcharge = upcharge;
      this.payOnExit = payOnExit;
    }

    public TransferMeta(TransferType type) {
      this(type, Money.ZERO_USD, false);
    }
  }

  private static RideType classify(Leg ride) {
    Route getRoute = ride.route();
    String shortName = getRoute.getShortName();
    if (shortName != null) {
      shortName = shortName.toLowerCase();
    }

    switch (getRoute.getAgency().getId().getId()) {
      case COBB_AGENCY_ID -> {
        if (RideType.COBB_EXPRESS.routeNamesContains(shortName)) {
          return RideType.COBB_EXPRESS;
        } else if (COBB_FREE_RIDE_SHORT_NAMES.contains(shortName)) {
          return RideType.FREE_RIDE;
        }
        return RideType.COBB_LOCAL;
      }
      case XPRESS_AGENCY_ID -> {
        // Get hour of trip start
        long hours = ride.startTime().withZoneSameInstant(NEW_YORK_ZONE_ID).getHour();
        if (hours >= 12) {
          return RideType.XPRESS_AFTERNOON;
        } else {
          return RideType.XPRESS_MORNING;
        }
      }
      case GCT_AGENCY_ID -> {
        if (RideType.GCT_EXPRESS_Z1.routeNamesContains(shortName)) {
          return RideType.GCT_EXPRESS_Z1;
        } else if (RideType.GCT_EXPRESS_Z2.routeNamesContains(shortName)) {
          return RideType.GCT_EXPRESS_Z2;
        }
        return RideType.GCT_LOCAL;
      }
      // Also catches MARTA_AGENCY_ID
      default -> {
        // Streetcar GTFS published by MARTA
        if (RideType.STREETCAR.routeNamesContains(shortName)) {
          return RideType.STREETCAR;
        }
        return RideType.MARTA;
      }
    }
  }

  private static int getMaxTransfers(RideType rideType) {
    return switch (rideType) {
      // GCT only allows 3 transfers.
      case GCT_EXPRESS_Z1, GCT_LOCAL, GCT_EXPRESS_Z2 -> 3;
      default -> 4;
    };
  }

  private static Duration getTransferWindow(RideType ignored) {
    return Duration.ofHours(3);
  }

  private static TransferMeta classifyTransfer(
    RideType toRideType,
    RideType fromRideType,
    FareType fareType
  ) {
    switch (toRideType) {
      case STREETCAR:
      case FREE_RIDE:
        return new TransferMeta(TransferType.NO_TRANSFER);
      case COBB_LOCAL:
        if (!isElectronicPayment(fareType)) {
          if (fromRideType == RideType.COBB_LOCAL || fromRideType == RideType.COBB_EXPRESS) {
            return new TransferMeta(TransferType.FREE_TRANSFER);
          }
          return new TransferMeta(TransferType.END_TRANSFER);
        }
        return switch (fromRideType) {
          case COBB_LOCAL, COBB_EXPRESS, MARTA -> new TransferMeta(TransferType.FREE_TRANSFER);
          default -> new TransferMeta(TransferType.END_TRANSFER);
        };
      case COBB_EXPRESS:
        if (!isElectronicPayment(fareType)) {
          return switch (fromRideType) {
            case COBB_EXPRESS -> new TransferMeta(TransferType.FREE_TRANSFER);
            case COBB_LOCAL -> new TransferMeta(TransferType.TRANSFER_PAY_DIFFERENCE);
            default -> new TransferMeta(TransferType.END_TRANSFER);
          };
        }
        // Electronic payment
        return switch (fromRideType) {
          case COBB_EXPRESS, MARTA -> new TransferMeta(TransferType.FREE_TRANSFER);
          case COBB_LOCAL -> new TransferMeta(TransferType.TRANSFER_PAY_DIFFERENCE);
          default -> new TransferMeta(TransferType.NO_TRANSFER);
        };
      case MARTA:
        if (!isElectronicPayment(fareType)) return new TransferMeta(TransferType.END_TRANSFER);
        return switch (fromRideType) {
          case MARTA,
            XPRESS_AFTERNOON,
            XPRESS_MORNING,
            COBB_LOCAL,
            COBB_EXPRESS,
            GCT_EXPRESS_Z1,
            GCT_EXPRESS_Z2,
            GCT_LOCAL -> new TransferMeta(TransferType.FREE_TRANSFER);
          default -> new TransferMeta(TransferType.END_TRANSFER);
        };
      case XPRESS_MORNING:
      case XPRESS_AFTERNOON:
        boolean payOnExit = toRideType == RideType.XPRESS_AFTERNOON;
        if (!isElectronicPayment(fareType)) return new TransferMeta(TransferType.END_TRANSFER);
        return switch (fromRideType) {
          case MARTA,
            COBB_EXPRESS,
            GCT_EXPRESS_Z1,
            GCT_EXPRESS_Z2,
            XPRESS_AFTERNOON,
            XPRESS_MORNING -> new TransferMeta(
            TransferType.FREE_TRANSFER,
            Money.ZERO_USD,
            payOnExit
          );
          case COBB_LOCAL -> new TransferMeta(
            TransferType.TRANSFER_WITH_UPCHARGE,
            usDollars(1.50f),
            payOnExit
          );
          case GCT_LOCAL -> new TransferMeta(
            TransferType.TRANSFER_WITH_UPCHARGE,
            usDollars(1),
            payOnExit
          );
          default -> new TransferMeta(TransferType.END_TRANSFER);
        };
      case GCT_LOCAL:
        if (!isElectronicPayment(fareType)) return new TransferMeta(TransferType.END_TRANSFER);
        return switch (fromRideType) {
          case MARTA, GCT_LOCAL, GCT_EXPRESS_Z1, GCT_EXPRESS_Z2 -> new TransferMeta(
            TransferType.FREE_TRANSFER
          );
          default -> new TransferMeta(TransferType.END_TRANSFER);
        };
      case GCT_EXPRESS_Z1:
      case GCT_EXPRESS_Z2:
        if (!isElectronicPayment(fareType)) return new TransferMeta(TransferType.END_TRANSFER);
        return switch (fromRideType) {
          case MARTA -> new TransferMeta(TransferType.FREE_TRANSFER);
          case GCT_LOCAL, GCT_EXPRESS_Z1, GCT_EXPRESS_Z2 -> new TransferMeta(
            TransferType.TRANSFER_PAY_DIFFERENCE
          );
          default -> new TransferMeta(TransferType.END_TRANSFER);
        };
      default:
        return new TransferMeta(TransferType.END_TRANSFER);
    }
  }

  private static boolean isElectronicPayment(FareType fareType) {
    return (
      fareType.equals(FareType.electronicRegular) ||
      fareType.equals(FareType.electronicSenior) ||
      fareType.equals(FareType.electronicSpecial) ||
      fareType.equals(FareType.electronicYouth)
    );
  }

  public AtlantaFareService(Collection<FareRuleSet> regularFareRules) {
    addFareRules(FareType.regular, regularFareRules);
    addFareRules(FareType.senior, regularFareRules);
    addFareRules(FareType.youth, regularFareRules);
    addFareRules(FareType.electronicRegular, regularFareRules);
    addFareRules(FareType.electronicYouth, regularFareRules);
    addFareRules(FareType.electronicSpecial, regularFareRules);
    addFareRules(FareType.electronicSenior, regularFareRules);
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

  @Override
  public ItineraryFare calculateFaresForType(
    Currency currency,
    FareType fareType,
    List<Leg> legs,
    Collection<FareRuleSet> fareRules
  ) {
    List<ATLTransfer> transfers = new ArrayList<>();
    for (var ride : legs) {
      Money defaultFare = getLegPrice(ride, fareType, fareRules);
      if (transfers.isEmpty()) {
        transfers.add(new ATLTransfer(currency, fareType));
      }
      ATLTransfer latestTransfer = transfers.get(transfers.size() - 1);
      if (!latestTransfer.addLeg(ride, defaultFare)) {
        // Transfer is invalid, create a new one.
        ATLTransfer newXfer = new ATLTransfer(currency, fareType);
        newXfer.addLeg(ride, defaultFare);
        transfers.add(newXfer);
      }
    }

    Money cost = Money.ZERO_USD;
    for (ATLTransfer transfer : transfers) {
      cost = cost.plus(transfer.getTotal());
    }
    var fareProduct = new FareProduct(
      new FeedScopedId(FEED_ID, fareType.name()),
      fareType.name(),
      cost,
      null,
      null,
      null
    );
    var fare = ItineraryFare.empty();
    fare.addItineraryProducts(List.of(fareProduct));
    return fare;
  }
}
