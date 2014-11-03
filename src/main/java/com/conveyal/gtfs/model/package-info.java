/**
 * This package contains an alternative to the GTFS loading classes we have been using from OBA.
 * It loads GTFS into a disk-backed map and attempts to be relatively efficient space-wise.
 * The intent is to eliminate the need for AgencyAndIds, using feed IDs and keeping each feed in its own
 * separate data structure.
 */
package com.conveyal.gtfs.model;