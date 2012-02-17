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

package org.opentripplanner.routing.util;

public class SlopeCosts {
    public double slopeSpeedEffectiveLength;
    public double slopeWorkCost; // the cost in watt-seconds at 5 m/s 
    public double maxSlope;
    public double slopeSafetyCost; //an additional safety cost caused by the slope
    
    public SlopeCosts(double slopeSpeedEffectiveLength, double slopeWorkCost, double slopeSafetyCost, double maxSlope) {
        this.slopeSpeedEffectiveLength = slopeSpeedEffectiveLength;
        this.slopeWorkCost = slopeWorkCost;
        this.slopeSafetyCost = slopeSafetyCost;
        this.maxSlope = maxSlope;
    }
}
