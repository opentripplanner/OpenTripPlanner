package org.opentripplanner.routing.services.notes;

import org.opentripplanner.model.StreetNote;

import java.io.Serializable;

/**
 * A container for a pair (note matcher, note).
 * 
 * @author laurent
 */
public class MatcherAndStreetNote implements Serializable {
    private static final long serialVersionUID = 1L;

    public MatcherAndStreetNote(NoteMatcher matcher, StreetNote note) {
        this.matcher = matcher;
        this.note = note;
    }

    private NoteMatcher matcher;

    private StreetNote note;

    public NoteMatcher getMatcher() {
        return matcher;
    }

    public StreetNote getNote() {
        return note;
    }
}