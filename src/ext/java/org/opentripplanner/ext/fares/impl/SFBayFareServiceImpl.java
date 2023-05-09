package org.opentripplanner.ext.fares.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.ext.fares.model.FareRuleSet;
import org.opentripplanner.model.fare.ItineraryFares;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.core.FareType;
import org.opentripplanner.transit.model.basic.Money;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SFBayFareServiceImpl extends DefaultFareService {

  @SuppressWarnings("unused")
  private static final Logger LOG = LoggerFactory.getLogger(SFBayFareServiceImpl.class);

  public static final int SFMTA_TRANSFER_DURATION = 60 * 90;
  public static final int BART_TRANSFER_DURATION = 60 * 60;
  public static final Money SFMTA_BASE_FARE = Money.usDollars(2);
  public static final Money CABLE_CAR_FARE = Money.usDollars(5);
  public static final Money AIRBART_FARE = Money.usDollars(3);
  public static final Money SFMTA_BART_TRANSFER_FARE = Money.usDollars(1.75f);
  public static final Set<String> SFMTA_BART_TRANSFER_STOPS = new HashSet<>(
    Arrays.asList("EMBR", "MONT", "POWL", "CIVC", "16TH", "24TH", "GLEN", "BALB", "DALY")
  );
  public static final String SFMTA_BART_FREE_TRANSFER_STOP = "DALY";
  public static final Money ZERO_DOLLARS = Money.usDollars(0);

  public SFBayFareServiceImpl(Collection<FareRuleSet> regularFareRules) {
    addFareRules(FareType.regular, regularFareRules);
  }

  protected Money getLowestCost(
    FareType fareType,
    List<Leg> rides,
    Collection<FareRuleSet> fareRules
  ) {
    List<Leg> bartBlock = null;
    Long sfmtaTransferIssued = null;
    Long alightedBart = null;
    String alightedBartStop = null;
    Money cost = ZERO_DOLLARS;
    String agencyId = null;
    for (var ride : rides) {
      agencyId = ride.getRoute().getId().getFeedId();
      if (agencyId.equals("BART")) {
        if (bartBlock == null) {
          bartBlock = new ArrayList<>();
        }
        bartBlock.add(ride);
        alightedBart = ride.getEndTime().toEpochSecond();
        alightedBartStop = ride.getTo().stop.getId().getId();
      } else { // non-BART agency
        if (bartBlock != null) {
          // finalize outstanding bart block, if any
          cost = cost.plus(calculateCost(fareType, bartBlock, fareRules).orElse(ZERO_DOLLARS));
          bartBlock = null;
        }
        if (agencyId.equals("SFMTA")) {
          TransitMode mode = (ride instanceof TransitLeg transitLeg) ? transitLeg.getMode() : null;
          if (mode == TransitMode.CABLE_CAR) {
            // no transfers issued or accepted
            cost = cost.plus(CABLE_CAR_FARE);
          } else if (
            sfmtaTransferIssued == null ||
            sfmtaTransferIssued + SFMTA_TRANSFER_DURATION < ride.getEndTime().toEpochSecond()
          ) {
            sfmtaTransferIssued = ride.getStartTime().toEpochSecond();
            if (
              alightedBart != null &&
              alightedBart + BART_TRANSFER_DURATION > ride.getEndTime().toEpochSecond() &&
              SFMTA_BART_TRANSFER_STOPS.contains(alightedBartStop)
            ) {
              // discount for BART to Muni transfer
              if (alightedBartStop.equals(SFMTA_BART_FREE_TRANSFER_STOP)) {
                // no cost to ride Muni
              } else {
                cost = cost.plus(SFMTA_BART_TRANSFER_FARE);
              }
            } else {
              // no transfer, basic fare
              cost = cost.plus(SFMTA_BASE_FARE);
            }
          } else {
            // SFMTA-SFMTA non-cable-car transfer within time limit, no cost
          }
        } else if (agencyId.equals("AirBART")) {
          cost = cost.plus(AIRBART_FARE);
        }
      }
    }
    if (bartBlock != null) {
      // finalize outstanding bart block, if any
      cost = cost.plus(calculateCost(fareType, bartBlock, fareRules).orElse(ZERO_DOLLARS));
    }
    return cost;
  }

  @Override
  protected boolean populateFare(
    ItineraryFares fare,
    Currency currency,
    FareType fareType,
    List<Leg> rides,
    Collection<FareRuleSet> fareRules
  ) {
    Money lowestCost = getLowestCost(fareType, rides, fareRules);
    fare.addFare(fareType, lowestCost);
    return true;
  }
}
