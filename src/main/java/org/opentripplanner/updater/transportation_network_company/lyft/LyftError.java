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

package org.opentripplanner.updater.transportation_network_company.lyft;

public class LyftError {
    public String error;
    public String error_description;

    @Override
    public String toString() {
        return "LyftError{" +
            "error='" + (error != null ? error : "null") + '\'' +
            ", error_description='" + (error_description != null ? error_description : "null") + '\'' +
            '}';
    }
}
