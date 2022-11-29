package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.model.StreetNote;
import org.opentripplanner.routing.services.notes.NoteMatcher;

record StreetNoteAndNoteMatcher(StreetNote note, NoteMatcher matcher) {}
