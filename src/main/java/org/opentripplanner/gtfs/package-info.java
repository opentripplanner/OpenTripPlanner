/**
 * This package contains a GTFS loader library.
 * It is made to be as tolerant as possible of errors in the input, because a large proportion of GTFS feeds contain
 * errors, and we want to be able to report all of those errors rather than bailing out the first time we hit an
 * exception or missing field.
 *
 * This package is currently in the OpenTripPlanner repository to avoid dependency mismatches during development, but should
 * be written such that it functions as an independent library.
 */
package org.opentripplanner.gtfs;