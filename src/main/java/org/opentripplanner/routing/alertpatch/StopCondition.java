package org.opentripplanner.routing.alertpatch;


/**
 * Represents conditions for when an AlertPatch is applicable
 *
 * Values are defined in the SIRI SituationExchange-xsd
 */
public enum StopCondition {
    /**  at departure or when passengers expect to board */
    START_POINT,
    /**  for passengers expecting to disembark, or at the last stop */
    DESTINATION,
    /**  when passing a stop */
    NOT_STOPPING,
    /**  for passengers expecting an interchange */
    EXCEPTIONAL_STOP,
    /**  when a passenger must request the serving of a stop */
    REQUEST_STOP,
    /**  affects all interactions with the stop (boarding, alighting, arrival, departure, interchanges */
    STOP
}
