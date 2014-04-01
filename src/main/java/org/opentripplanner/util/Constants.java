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

package org.opentripplanner.util;

/**
 * Constants
 * 
 * @author Frank Purcell
 * @version $Revision: 1.0 $
 * @since 1.0
 */
public interface Constants {

    // geo json stuff
    public static final String GEO_JSON_POINT = "{\"type\": \"Point\", \"coordinates\": [";
    public static final String GEO_JSON_TAIL = "]}";

    // PostGIS POINT(x, y) construct
    public static final String POINT_PREFIX = "POINT(";
    public static final int POINT_PREFIX_LEN = POINT_PREFIX.length();
    public static final String POINT_SUFFIX = ")";
    public static final int POINT_SUFFIX_LEN = POINT_SUFFIX.length();
    public static final String POINT_SEPARATOR = " ";
}
