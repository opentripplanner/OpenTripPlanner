package org.opentripplanner.model.plan;

import org.opentripplanner.model.Operator;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.core.Money;

/**
 * This is an offer made by a 3rd party operator it can be attached to an itinerary Leg.
 * The service can be provided by a Transit, Taxi or TNC Operator.
 * <p>
 * This field is returned as provided by the service operator. They may be retrieved
 * from an external service or estimated by OTP.
 */
public class RideOffer {

    /**
     * The specific company that the TNC travel is for.
     */
    public final Operator operator;

    /**
     * The predicted travel duration in seconds of this leg as obtained from service provider.
     */
    public final Integer travelDurationSeconds;

    /**
     * The maximum estimated cost in the specified currency as obtained from service provider.
     */
    public final Money maxPrice;

    /**
     * The minimum estimated cost in the specified currency as obtained from service provider.
     */
    public final Money minPrice;

    /**
     * The ID of the relevant travel product as obtained from the API of the service provider.
     */
    public final String productId;

    /**
     * The human-readable display name of the particular TNC product as obtained from service
     * provider.
     */
    public final String displayName;

    /**
     * The estimated time in seconds for a TNC vehicle to reach the starting point of the ride as obtained
     * from service provider.
     */
    public final Integer estimatedArrival;


    public RideOffer(
            Operator operator,
            Integer travelDurationSeconds,
            Money maxPrice,
            Money minPrice,
            String productId,
            String displayName,
            Integer estimatedArrival
    ) {
        this.operator = operator;
        this.travelDurationSeconds = travelDurationSeconds;
        this.maxPrice = maxPrice;
        this.minPrice = minPrice;
        this.productId = productId;
        this.displayName = displayName;
        this.estimatedArrival = estimatedArrival;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(RideOffer.class)
                .addObj("operator", operator)
                .addDuration("travelDuration", travelDurationSeconds)
                .addObj("maxCost", maxPrice)
                .addObj("minCost", minPrice)
                .addStr("productId'", productId)
                .addStr("displayName'", displayName)
                .addNum("estimatedArrival", estimatedArrival, "epocSec")
                .toString();
    }
}
