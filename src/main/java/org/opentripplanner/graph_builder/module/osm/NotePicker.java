package org.opentripplanner.graph_builder.module.osm;

/**
 * Defines which OSM ways get notes and what kind of notes they get.
 * 
 * @author novalis
 * 
 */
public class NotePicker {

    public OSMSpecifier specifier;

    public NoteProperties noteProperties;

    public NotePicker(OSMSpecifier specifier, NoteProperties noteProperties) {
        this.specifier = specifier;
        this.noteProperties = noteProperties;
    }
}
