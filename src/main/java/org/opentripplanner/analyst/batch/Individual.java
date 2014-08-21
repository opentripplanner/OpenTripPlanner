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

package org.opentripplanner.analyst.batch;

import org.opentripplanner.analyst.core.Sample;

/** Individual locations that make up Populations for the purpose of many-to-many searches. */
public class Individual {

    public String label;
    public double lon;
    public double lat;
    public double input;  // not final to allow clamping and scaling by filters
    public Sample sample= null; // not final, allowing sampling to occur after filterings
    
    public Individual(String label, double lon, double lat, double input) {
        this.label = label;
        this.lon = lon;
        this.lat = lat;
        this.input = input;
    }

    public Individual() { }
 
    // public boolean rejected;

    
}
