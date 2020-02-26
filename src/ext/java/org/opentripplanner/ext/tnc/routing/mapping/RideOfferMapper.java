package org.opentripplanner.ext.tnc.routing.mapping;

import org.opentripplanner.ext.tnc.routing.model.ArrivalTime;
import org.opentripplanner.ext.tnc.routing.model.RideEstimate;
import org.opentripplanner.ext.tnc.routing.model.TransportationNetworkCompany;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.plan.RideOffer;
import org.opentripplanner.routing.core.Money;
import org.opentripplanner.routing.core.WrappedCurrency;

import java.util.function.Function;

public class RideOfferMapper {

    private final Function<TransportationNetworkCompany, Operator> tncOperators;

    public RideOfferMapper(Function<TransportationNetworkCompany, Operator> tncOperators) {
        this.tncOperators = tncOperators;
    }

    public RideOffer map(RideEstimate estimate, ArrivalTime time) {
        if (estimate == null && time == null) { return null; }

        if(estimate != null) {
            WrappedCurrency currency = new WrappedCurrency(estimate.currency);

            return new RideOffer(
                    tncOperators.apply(estimate.company),
                    estimate.duration,
                    money(estimate.maxCost, currency),
                    money(estimate.minCost, currency),
                    time == null ? null : time.productId,
                    time == null ? null : time.displayName,
                    time == null ? null : time.estimatedSeconds
            );
        }
        // time is NOT null
        return new RideOffer(
                null,
                null,
                null,
                null,
                time.productId,
                time.displayName,
                time.estimatedSeconds
        );

    }

    Money money(double value, WrappedCurrency currency) {
        return new Money(currency, (int) value * currency.getDefaultFractionDigits());
    }
}
