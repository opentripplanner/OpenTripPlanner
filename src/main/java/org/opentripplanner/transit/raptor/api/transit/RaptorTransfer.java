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
     * Returns the earliest possible departure time for the leg. Used Eg. in flex routing and TNC
     * when the access leg can't start immediately, but have to wait for a vehicle arriving. Also
     * DRT systems or bike shares can have operation time limitations.
     *
     * Returns -1 if transfer is not possible after the requested departure time
     */
    int earliestDepartureTime(int requestedDepartureTime);

    /**
     * Returns the latest possible arrival time for the leg. Used in DRT systems or bike shares
     * where they can have operation time limitations.
     *
     * Returns -1 if transfer is not possible before the requested arrival time
     */
    int latestArrivalTime(int requestedArrivalTime);

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

    /**
     * Return the number publicly available services for this {@link RaptorTransfer}. This is used by Raptor to get the
     * number of transfers correct witch is part of the criteria used to keep optimal result. This method apply to both
     * access and egress, but is not used for public-transit-transfers.
     */
    default int numberOfPublicServiceLegs() {
        return 0;
    }

    /**
     * Is this {@link RaptorTransfer} is connected to the given {@code stop} by <b>transit</b>? For access and egress
     * paths we allow plugging in flex and other means of transport, witch might include one or more legs onboard a
     * vehicle. This method should return {@code true} if the leg connecting to the given stop is `onBoard` a public
     * transport or riding another kind of service like a taxi. This method apply to both access and egress, but is not
     * used for public-transit-transfers.
     */
    default boolean connectedByPublicService() {
        return false;
    }
}
