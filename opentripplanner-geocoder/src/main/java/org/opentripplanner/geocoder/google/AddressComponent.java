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

package org.opentripplanner.geocoder.google;

import java.util.List;

public class AddressComponent {
	
	private String long_name;
	private String short_name;
	private List<String> types;
	
	public String getLong_name() {
		return long_name;
	}
	public void setLong_name(String longName) {
		long_name = longName;
	}
	public String getShort_name() {
		return short_name;
	}
	public void setShort_name(String shortName) {
		short_name = shortName;
	}
	public List<String> getTypes() {
		return types;
	}
	public void setTypes(List<String> types) {
		this.types = types;
	}
}
