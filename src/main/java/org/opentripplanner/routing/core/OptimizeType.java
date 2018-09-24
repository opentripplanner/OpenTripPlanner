package org.opentripplanner.routing.core;

/**
 * TODO: apparently, other than TRANSFERS all of these only affect BICYCLE traversal of street edges.
 * If so this should be very clearly stated in documentation and even in the Enum name, which could be
 * BicycleOptimizeType, since TRANSFERS is vestigial and should probably be removed.
 */
public enum OptimizeType {
    QUICK, /* the fastest trip */
    SAFE,
    FLAT, /* needs a rewrite */
    GREENWAYS,
    TRIANGLE,
    TRANSFERS /* obsolete, replaced by the transferPenalty option in Traverse options */
}