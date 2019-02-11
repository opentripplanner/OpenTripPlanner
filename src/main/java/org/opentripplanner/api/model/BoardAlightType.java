package org.opentripplanner.api.model;

/**
 * Distinguish between special ways a passenger may board or alight at a stop. The majority of
 * boardings and alightings will be of type "default" -- a regular boarding or alighting at a
 * regular transit stop. Currently, the only non-default types are related to GTFS-Flex, but this
 * pattern can be extended as necessary.
 */
public enum BoardAlightType {

    /**
     * A regular boarding or alighting at a fixed-route transit stop.
     */
    DEFAULT,

    /**
     * A flag-stop boarding or alighting, e.g. flagging the bus down or a passenger asking the bus
     * driver for a drop-off between stops. This is specific to GTFS-Flex.
     */
    FLAG_STOP,

    /**
     * A boarding or alighting at which the vehicle deviates from its fixed route to drop off a
     * passenger. This is specific to GTFS-Flex.
     */
    DEVIATED;
}
