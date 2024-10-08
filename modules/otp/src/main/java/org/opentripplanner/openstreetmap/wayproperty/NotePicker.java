package org.opentripplanner.openstreetmap.wayproperty;

import org.opentripplanner.openstreetmap.wayproperty.specifier.OsmSpecifier;

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
