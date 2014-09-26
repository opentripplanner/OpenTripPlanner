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
    public final boolean flattened;
    public final double slopeSpeedFactor; // The slope speed factor multiplier, w/o units
    public final double slopeWorkFactor; // The slope work factor in joules per meters at 5 m/s
    public final double maxSlope; // Max{abs(slope)}
    public final double slopeSafetyCost; // An additional safety cost caused by the slope
    public final double lengthMultiplier; // Multiplier to get true length based on flat (projected) length
    
    public SlopeCosts(double slopeSpeedFactor, double slopeWorkFactor, double slopeSafetyCost,
                      double maxSlope, double lengthMultiplier, boolean flattened) {
        this.slopeSpeedFactor = slopeSpeedFactor;
        this.slopeWorkFactor = slopeWorkFactor;
        this.slopeSafetyCost = slopeSafetyCost;
        this.maxSlope = maxSlope;
        this.lengthMultiplier = lengthMultiplier;
        this.flattened = flattened;
    }
}
