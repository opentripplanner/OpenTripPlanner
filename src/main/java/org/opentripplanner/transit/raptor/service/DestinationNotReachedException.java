package org.opentripplanner.transit.raptor.service;


/**
 * This exception is used to abort a multi-criteria search. A multi-criteria search may perform
 * one or 2 simple raptor searches before is start. If the simple raptor search do not find
 * a possible result, then this exception is thrown to abort/prevent the multi-criteria search
 * from being performed.
 */
class DestinationNotReachedException extends RuntimeException {}
