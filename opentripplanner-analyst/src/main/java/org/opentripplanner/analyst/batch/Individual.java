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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import org.opentripplanner.analyst.core.Sample;

/** Individual locations that make up Populations for the purpose of many-to-many searches. */
@ToString @RequiredArgsConstructor
public class Individual {

    public final String label;
    public final double lon;
    public final double lat;
    @NonNull public double input;  // not final to allow clamping and scaling by filters
    public Sample sample= null; // not final, allowing sampling to occur after filterings
    
    // public boolean rejected;
        
}
