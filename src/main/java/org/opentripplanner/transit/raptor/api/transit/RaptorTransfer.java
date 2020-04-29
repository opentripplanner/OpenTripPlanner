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

}
