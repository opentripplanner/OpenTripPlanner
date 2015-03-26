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

package org.opentripplanner.routing.core;

/**
 * TODO: apparently, other than TRANSFERS all of these only affect BICYCLE traversal of street edges.
 * If so this should be very clearly stated in documentation and even in the Enum name, which could be
 * BicycleOptimizeType, since TRANSFERS is vestigial and should probably be removed.
 */
public enum OptimizeType {
    QUICK, /* the fastest trip */
    SAFE,
    FLAT, /* needs a rewrite */
    GREENWAYS,
    TRIANGLE,
    TRANSFERS /* obsolete, replaced by the transferPenalty option in Traverse options */
}