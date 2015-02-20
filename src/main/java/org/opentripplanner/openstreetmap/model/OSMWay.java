/* 
 Copyright 2008 Brian Ferris
 
 This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.openstreetmap.model;

import java.util.ArrayList;
import java.util.List;

public class OSMWay extends OSMWithTags {

    private List<Long> _nodes = new ArrayList<Long>();

    public void addNodeRef(OSMNodeRef nodeRef) {
        _nodes.add(nodeRef.getRef());
    }

    public void addNodeRef(long nodeRef) {
        _nodes.add(nodeRef);
    }

    public void addNodeRef(long nodeRef, int index) {
        _nodes.add(index, nodeRef);
    }

    public List<Long> getNodeRefs() {
        return _nodes;
    }

    public String toString() {
        return "osm way " + id;
    }

    /**
     * Returns true if bicycle dismounts are forced.
     * 
     * @return
     */
    public boolean isBicycleDismountForced() {
        String bicycle = getTag("bicycle");
        return isTag("cycleway", "dismount") || "dismount".equals(bicycle);
    }

    /**
     * Returns true if these are steps.
     * 
     * @return
     */
    public boolean isSteps() {
        return "steps".equals(getTag("highway"));
    }

    /**
     * Is this way a roundabout?
     * 
     * @return
     */
    public boolean isRoundabout() {
        return "roundabout".equals(getTag("junction"));
    }

    /**
     * Returns true if this is a one-way street for driving.
     * 
     * @return
     */
    public boolean isOneWayForwardDriving() {
        return isTagTrue("oneway");
    }

    /**
     * Returns true if this way is one-way in the opposite direction of its definition.
     * 
     * @return
     */
    public boolean isOneWayReverseDriving() {
        return isTag("oneway", "-1");
    }

    /**
     * Returns true if bikes can only go forward.
     * 
     * @return
     */
    public boolean isOneWayForwardBicycle() {
        String oneWayBicycle = getTag("oneway:bicycle");
        return isTrue(oneWayBicycle) || isTagFalse("bicycle:backwards");
    }

    /**
     * Returns true if bikes can only go in the reverse direction.
     * 
     * @return
     */
    public boolean isOneWayReverseBicycle() {
        return "-1".equals(getTag("oneway:bicycle"));
    }

    /**
     * Returns true if bikes must use sidepath in forward direction
     * 
     * @return 
     */
    public boolean isForwardDirectionSidepath() {
        return "use_sidepath".equals(getTag("bicycle:forward"));
    }

    /**
     * Returns true if bikes must use sidepath in reverse direction
     * 
     * @return 
     */
    public boolean isReverseDirectionSidepath() {
        return "use_sidepath".equals(getTag("bicycle:backward"));
    }

    /**
     * Some cycleways allow contraflow biking.
     * 
     * @return
     */
    public boolean isOpposableCycleway() {
        // any cycleway which is opposite* allows contraflow biking
        String cycleway = getTag("cycleway");
        String cyclewayLeft = getTag("cycleway:left");
        String cyclewayRight = getTag("cycleway:right");

        return (cycleway != null && cycleway.startsWith("opposite"))
                || (cyclewayLeft != null && cyclewayLeft.startsWith("opposite"))
                || (cyclewayRight != null && cyclewayRight.startsWith("opposite"));
    }
}
