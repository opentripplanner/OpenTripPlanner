package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.graph_builder.module.osm.specifier.OsmSpecifier;

/**
 * Defines which OSM ways get notes and what kind of notes they get.
 *
 * @author novalis
 */
public class NotePicker {

  public OsmSpecifier specifier;

  public NoteProperties noteProperties;

  public NotePicker(OsmSpecifier specifier, NoteProperties noteProperties) {
    this.specifier = specifier;
    this.noteProperties = noteProperties;
  }
}
