package org.opentripplanner.transit.raptor.api.transit;


/**
 * Encapsulate information about a access, transfer or egress leg. We do not distinguish
 * between the access (origin to first stop), transfer (stop to stop) or egress (last stop to destination),
 * to Raptor - all these are the same thing.
 */
public interface RaptorTransfer {

    /**
     * <ul>
     *     <li>Access: The first stop in the journey, where the access leg just arrived at.
     *     <li>Transit: Stop index where the leg arrive at.
     *     <li>Egress: Last stop before destination, hence not the arrival point, but the departure stop.
     * </ul>
     * The journey origin, destination and transit leg board stop must be part of the context; hence not
     * a member attribute of this type.
     */
    int stop();

    /**
     * The time duration to walk or travel the leg in seconds. This is not the entire duration from the journey origin,
     * but just:
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
     * Returns the earliest possible departure time for the leg. Used Eg. in flex routing and TNC
     * when the access leg can't start immediately, but have to wait for a vehicle arriving. Also
     * DRT systems or bike shares can have operation time limitations.
     *
     * Returns -1 if transfer is not possible after the requested departure time
     */
    default int earliestDepartureTime(int requestedDepartureTime) {
        return requestedDepartureTime;
    };

    /**
     * Returns the latest possible arrival time for the leg. Used in DRT systems or bike shares
     * where they can have operation time limitations.
     *
     * Returns -1 if transfer is not possible before the requested arrival time
     */
    default int latestArrivalTime(int requestedArrivalTime) {
        return requestedArrivalTime;
    };

    /* ACCESS/TRANSFER/EGRESS CONTAINING MULTIPLE LEGS */
    // The methods below should be only overridden when a RaptorTransfer contains information about
    // public services, which were generated outside the RAPTOR algorithm. Examples of such schemes
    // include flexible transit service and TNC. They should not be used for regular
    // access/transfer/egress.

    /**
     * Some services involving multiple legs are not handled by the RAPTOR algorithm and need to be
     * inserted into the algorithm at a specific place of the algorithm, and to be accounted for,
     * in order to get the number of transfers correct, witch is part of the criteria used to keep
     * optimal result.
     *
     * Examples:
     *  1 - One walking leg
     *  2 - Waking leg followed by a transit leg
     *  3 - Waking leg followed by a transit leg and a walking leg
     *
     * @return the number legs for the transfer, generated outside the RAPTOR algorithm.
     */
    default int numberOfLegs() {
        return 1;
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
}
