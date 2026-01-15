package org.opentripplanner.ext.fares.service.gtfs.v2;

import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.fares.model.TimeLimit;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.fare.FareProduct;
import org.opentripplanner.model.plan.TransitLeg;

/**
 * Represents an offer for a transit leg, encapsulating fare information and optional time limits.
 * This interface is used to determine fare products applicable to specific legs of transit,
 * and provide functionality for validating time-limited offers.
 */
sealed interface LegOffer {
  /**
   * Determines whether the given transit leg is within the valid time limit of the fare or product
   * associated with this instance.
   */
  boolean withinTimeLimit(TransitLeg end);

  FareOffer offer();

  FareProduct fareProduct();

  static LegOffer of(FareOffer offer) {
    return new DefaultLegOffer(offer);
  }

  static LegOffer of(FareOffer offer, TransitLeg startLeg, @Nullable TimeLimit timeLimit) {
    if (timeLimit == null) {
      return new DefaultLegOffer(offer);
    } else {
      return new TimeLimitedOffer(offer, timeLimit, startLeg);
    }
  }

  /**
   * A default implementation of the {@link LegOffer} interface that provides fare offer details
   * associated with a transit leg. This implementation does not enforce any time limit constraints.
   */
  record DefaultLegOffer(FareOffer offer) implements LegOffer {
    @Override
    public boolean withinTimeLimit(TransitLeg end) {
      return true;
    }

    @Override
    public FareProduct fareProduct() {
      return offer.fareProduct();
    }
  }

  /**
   * A time-limited implementation of the {@link LegOffer} interface that provides fare offer details.
   */
  record TimeLimitedOffer(FareOffer offer, TimeLimit timeLimit, TransitLeg startLeg)
    implements LegOffer {
    public TimeLimitedOffer {
      Objects.requireNonNull(timeLimit);
    }

    @Override
    public boolean withinTimeLimit(TransitLeg end) {
      return TimeLimitEvaluator.withinTimeLimit(timeLimit, startLeg, end);
    }

    @Override
    public FareProduct fareProduct() {
      return offer.fareProduct();
    }
  }
}
