package org.opentripplanner.routing.alertpatch;


/**
 * Represents conditions for when an AlertPatch is applicable
 *
 * Values are defined in the SIRI SituationExchange-xsd
 */
public enum StopCondition {
    START_POINT, // at departure or when passengers expect to board
    DESTINATION, // for passengers expecting to disembark, or at the last stop
    NOT_STOPPING, // when passing a stop
    EXCEPTIONAL_STOP, // for passengers expecting an interchange
    REQUEST_STOP, // when a passenger must request the serving of a stop
    STOP // affects all interactions with the stop (boarding, alighting, arrival, departure, interchanges
}
