package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.routing.services.notes.StreetNotesService;

import java.beans.PropertyEditorSupport;

public class NotePropertiesEditor extends PropertyEditorSupport {
    private NoteProperties value;

    public void setAsText(String pattern) {
        value = new NoteProperties(pattern, StreetNotesService.ALWAYS_MATCHER);
    }

    public String getAsText() {
        return value.notePattern;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object object) {
        value = (NoteProperties) object;
    }
}
