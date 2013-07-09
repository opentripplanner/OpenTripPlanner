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

package org.opentripplanner.routing.edgetype;

/**
 * The updater module depends on the routing module. Maven doesn't allow circular dependencies 
 * (for good reasons). Therefore the updater modules cannot be visible from the routing module, 
 * and we cannot poll them without defining an interface in routing.
 * 
 * This feels like a hack and is probably symptomatic of a need for either reshuffling
 * classes and modules, or using Spring wiring.
 * 
 * @author abyrd
 */
public interface TimetableSnapshotSource {

    /** 
     * @return an up-to-date snapshot mapping TripPatterns to Timetables. This snapshot and the
     * timetable objects it references are guaranteed to never change, so the requesting thread is 
     * provided a consistent view of all TripTimes. The routing thread need only release its 
     * reference to the snapshot to release resources.
     */
    public TimetableResolver getSnapshot();
    
}
