/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.module;

import java.io.File;

/**
 * Created by johan on 17/06/15.
 */
public class GtfsFeedId {
    private final String id;

    public GtfsFeedId(String id) {
        this.id = cleanId(id);
    }

    /**
     * Cleans the id before it is set. This method ensures that the id is a valid id.
     *
     * @param id The feed id
     * @return The cleaned id.
     */
    protected String cleanId(String id) {
        // 1. Underscore is used as an separator by OBA.
        // 2. Colon is used as an separator in OTP.
        return id.replaceAll("_", "")
                .replaceAll(":", "");
    }

    public String getId() {
        return id;
    }

    public static GtfsFeedId createFromFile(File path) {
        String feedId = path.getName();
        int pos = feedId.lastIndexOf('.');
        if (pos > 0) {
            feedId = feedId.substring(0, pos);
        }
        return new GtfsFeedId(feedId);
    }
}
