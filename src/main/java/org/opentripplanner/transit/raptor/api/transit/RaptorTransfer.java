package org.opentripplanner.transit.raptor.api.transit;


import org.opentripplanner.util.time.DurationUtils;

/**
 * Encapsulate information about an access, transfer or egress path. We do not distinguish
 * between the access (origin to first stop), transfer (stop to stop) or egress (last stop to
 * destination), to Raptor - all these are the same thing.
 */
public interface RaptorTransfer {

    /**
     * <ul>
     *     <li>Access: The first stop in the journey, where the access path just arrived at.
     *     <li>Transit: Stop index where the path arrive at.
     *     <li>Egress: Last stop before destination, hence not the arrival point, but the departure
     *     stop.
     * </ul>
     * The journey origin, destination and transit path board stop must be part of the context;
     * hence not a member attribute of this type.
     */
    int stop();

    /**
     * The generalized cost of this transfer in centi-seconds. The value is used to compare with
     * riding transit, and will be one component of a full itinerary.
     *
     * This methods is called many times, so care needs to be taken that the value is stored, not
     * calculated for each invocation.
     */
    int generalizedCost();

    /**
     * The time duration to walk or travel the path in seconds. This is not the entire duration
     * from the journey origin, but just:
     * <ul>
     *     <li>Access: journey origin to first stop.
     *     <li>Transit: stop to stop.
     *     <li>Egress: last stop to journey destination.
     * </ul>
     */
    int durationInSeconds();

    /* TIME-DEPENDENT ACCESS/TRANSFER/EGRESS */
    // The methods below should be only overridden when a RaptorTransfer is only available at
    // specific times, such as flexible transit, TNC or shared vehicle schemes with limited opening
    // hours, not for regular access/transfer/egress.

    /**
     * Returns the earliest possible departure time for the path. Used Eg. in flex routing and TNC
     * when the access path can't start immediately, but have to wait for a vehicle arriving. Also
     * DRT systems or bike shares can have operation time limitations.
     *
     * Returns -1 if transfer is not possible after the requested departure time
     */
    default int earliestDepartureTime(int requestedDepartureTime) {
        return requestedDepartureTime;
    };

    /**
     * Returns the latest possible arrival time for the path. Used in DRT systems or bike shares
     * where they can have operation time limitations.
     *
     * Returns -1 if transfer is not possible before the requested arrival time
     */
    default int latestArrivalTime(int requestedArrivalTime) {
        return requestedArrivalTime;
    };

    /*
       ACCESS/TRANSFER/EGRESS PATH CONTAINING MULTIPLE LEGS

       The methods below should be only overridden when a RaptorTransfer contains information about
       public services, which were generated outside the RAPTOR algorithm. Examples of such schemes
       include flexible transit service and TNC. They should not be used for regular
       access/transfer/egress.
    */

    /**
     * Some services involving multiple legs are not handled by the RAPTOR algorithm and need to be
     * inserted into the algorithm at a specific place of the algorithm. The number-of-rides must
     * be accounted for in order to get the number of transfers correct. The number-of-transfers is
     * part of the criteria used to keep an optimal result.
     * <p>
     * Note! The number returned should include all "rides" in the access leg resulting in an extra
     * transfer, including boarding the first Raptor scheduled trip. There is no need to account for
     * riding your own bicycle or scooter, and a rental bike is debatable. The guideline is that if
     * there is a transfer involved that is equivalent to the "human cost" to a normal transit
     * transfer, then it should be counted. If not, you should account for it using the cost
     * function instead.
     * <p>
     * Examples/guidelines:
     * <p>
     * <pre>
     * Access/egress  | num-of-rides | Description
     *     walk       |      0       | Plain walking leg
     *  bicycle+walk  |      0       | Use bicycle to get to stop
     * rental-bicycle |      0       | Picking up the bike and returning it is is best
     *                |              | accounted using time and cost penalties, not transfers.
     *     taxi       |     0/1      | Currently 0 in OTP(car), but this is definitely discussable.
     *     flex       |      1       | Walking leg followed by a flex transit leg
     * walk-flex-walk |      1       | Walking , then flex transit and then walking again
     *   flex-flex    |      2       | Two flex transit legs after each other
     * </pre>
     * {@code flex} is used as a placeholder for any type of on-board public service.
     *
     * @return the number transfers including the first boarding in the RAPTOR algorithm.
     */
    default int numberOfRides() {
        return 0;
    }

    default boolean hasRides() {
        return numberOfRides() > 0;
    }

    /**
     * Is this {@link RaptorTransfer} is connected to the given {@code stop} directly by
     * <b>transit</b>? For access and egress paths we allow plugging in flexible transit and other
     * means of transport, witch might include one or more legs onboard a vehicle. This method
     * should return {@code true} if the leg connecting to the given stop arrives `onBoard` a public
     * transport or riding another kind of service like a taxi.
     *
     * This information is used to generate transfers from that stop to other stops only when this
     * method returns true.
     */
    default boolean stopReachedOnBoard() {
        return false;
    }

    /** Call this from toString */
    default String asString() {
      String duration = DurationUtils.durationToStr(durationInSeconds());
        return hasRides()
            ? String.format("Flex %s %dx ~ %d", duration, numberOfRides(), stop())
            : String.format("On-Street %s ~ %d", duration, stop());
    }
}
