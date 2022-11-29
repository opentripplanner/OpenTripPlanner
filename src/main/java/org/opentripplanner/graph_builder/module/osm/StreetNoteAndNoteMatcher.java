package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.model.NoteMatcher;
import org.opentripplanner.model.StreetNote;

record StreetNoteAndNoteMatcher(StreetNote note, NoteMatcher matcher) {}
