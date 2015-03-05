/**
 * This package contains custom MapDB serializers for OTP's OSM data model.
 * Without them, the 70MB DC PBF becomes a 248MB MapDB
 * With them, using compression and variable-byte encodings, it becomes a 158MB MapDB
 */
package org.opentripplanner.osm.serializer;