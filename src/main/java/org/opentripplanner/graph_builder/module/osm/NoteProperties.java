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

package org.opentripplanner.graph_builder.module.osm;

import java.util.Map;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.TranslatedString;
import org.opentripplanner.routing.services.notes.NoteMatcher;

public class NoteProperties {

    public String notePattern;

    public NoteMatcher noteMatcher;

    public NoteProperties(String notePattern, NoteMatcher noteMatcher) {
        this.notePattern = notePattern;
        this.noteMatcher = noteMatcher;
    }

    public T2<Alert, NoteMatcher> generateNote(OSMWithTags way) {
        Map<String, String> noteText = TemplateLibrary.generateI18N(notePattern, way,
                Alert.defaultLanguage);
        Alert note = new Alert();
        note.alertHeaderText = new TranslatedString();
        for (Map.Entry<String, String> kv : noteText.entrySet()) {
            note.alertHeaderText.addTranslation(kv.getKey(), kv.getValue());
        }
        return new T2<>(note, noteMatcher);
    }
}
