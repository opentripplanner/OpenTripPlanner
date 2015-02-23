/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.services.notes.NoteMatcher;
import org.opentripplanner.util.LocalizedString;
import org.opentripplanner.util.TranslatedString;

public class NoteProperties {

    private static final Pattern patternMatcher = Pattern.compile("\\{(.*?)\\}");

    public String notePattern;

    public NoteMatcher noteMatcher;

    public NoteProperties(String notePattern, NoteMatcher noteMatcher) {
        this.notePattern = notePattern;
        this.noteMatcher = noteMatcher;
    }

    public T2<Alert, NoteMatcher> generateNote(OSMWithTags way) {
        Alert note = new Alert();
        if (patternMatcher.matcher(notePattern).matches()) {
            Map<String, String> noteText = TemplateLibrary.generateI18N(notePattern, way);
            note.alertHeaderText = TranslatedString.getI18NString(noteText);
        } else {
            note.alertHeaderText = new LocalizedString(notePattern, way);
        }
        return new T2<>(note, noteMatcher);
    }
}
