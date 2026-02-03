package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.osm.wayproperty.specifier.OsmSpecifier;

/**
 * Defines which OSM ways get notes and what kind of notes they get.
 *
 * @author novalis
 */
public record NotePicker(OsmSpecifier specifier, NoteProperties noteProperties) {}
