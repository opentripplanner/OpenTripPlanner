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
 * This class replaces a small part of the functionality provided by ArrayUtils from Apache Commons.
 */
public class ArrayUtils {
    public static boolean contains(Object array[], Object object) {
        if (array != null) {
            if (object != null) {
                for (Object element : array) if (object.equals(element)) return true;
            } else {
                for (Object element : array) if (element == null) return true;
            }
        }
        return false;
    }
}
