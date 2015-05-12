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

import java.util.Locale;

/**
 *
 * This interface is used when providing translations on server side.
 *
 * @author mabu
 */
public interface I18NString {

    /**
     * Returns default translation (english)
     * @return 
     */
    public String toString();
    
    /**
     * Returns wanted translation
     * @param locale Wanted locale
     * @return 
     */
    public String toString(Locale locale);
    
}
