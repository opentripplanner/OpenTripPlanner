package org.opentripplanner.routing.services.notes;

import java.io.Serializable;

import org.opentripplanner.routing.alertpatch.Alert;

/**
 * A container for a pair (note matcher, note).
 * 
 * @author laurent
 */
public class MatcherAndAlert implements Serializable {
    private static final long serialVersionUID = 1L;

    public MatcherAndAlert(NoteMatcher matcher, Alert note) {
        this.matcher = matcher;
        this.note = note;
    }

    private NoteMatcher matcher;

    private Alert note;

    public NoteMatcher getMatcher() {
        return matcher;
    }

    public Alert getNote() {
        return note;
    }
}