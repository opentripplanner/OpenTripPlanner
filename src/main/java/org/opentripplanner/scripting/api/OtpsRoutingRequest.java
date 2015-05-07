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

package org.opentripplanner.scripting.api;

import java.util.Calendar;
import java.util.Date;

import org.opentripplanner.api.parameter.QualifiedModeSet;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RouteMatcher;
import org.opentripplanner.routing.core.RoutingRequest;

/**
 * Routing request options: date/time, modes, max walk distance...
 * 
 * Example of code (python script):
 * 
 * <pre>
 *   router = otp.getRouter()
 *   req = otp.createRequest()
 *   req.setDateTime(2015, 1, 15, 14, 00, 00)
 *   req.setModes('WALK,BUS,RAIL')
 *   req.setOrigin(45.123, 1.456)
 *   spt = router.plan(req)
 *   ...
 * </pre>
 * 
 * @author laurent
 */
public class OtpsRoutingRequest {

    protected RoutingRequest req;

    protected OtpsRoutingRequest(RoutingRequest req) {
        this.req = req;
        req.batch = true;
    }

    public void setDateTime(int year, int month, int day, int hour, int min, int sec) {
        Calendar cal = Calendar.getInstance(); // Use default timezone
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, min);
        cal.set(Calendar.SECOND, sec);
        cal.set(Calendar.MILLISECOND, 0);
        req.setDateTime(cal.getTime());
    }

    public void setDateTime(Date dateTime) {
        req.setDateTime(dateTime);
    }

    public void setDateTime(long epochSec) {
        req.setDateTime(new Date(epochSec * 1000L));
    }

    public void setModes(String modesStr) {
        new QualifiedModeSet(modesStr).applyToRoutingRequest(req);
    }

    public void setArriveBy(boolean arriveBy) {
        req.arriveBy = arriveBy;
    }

    public void setMaxTimeSec(long maxTimeSec) {
        req.worstTime = req.dateTime + (req.arriveBy ? -maxTimeSec : maxTimeSec);
    }

    public void setWalkSpeedMs(double walkSpeed) {
        req.walkSpeed = walkSpeed;
    }

    public void setBikeSpeedMs(double bikeSpeed) {
        req.bikeSpeed = bikeSpeed;
    }

    public void setMaxWalkDistance(double maxWalkDistance) {
        req.maxWalkDistance = maxWalkDistance;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        req.wheelchairAccessible = wheelchairAccessible;
    }

    public void setClampInitialWait(long clampInitialWait) {
        req.clampInitialWait = clampInitialWait;
    }

    public void setOrigin(double lat, double lon) {
        req.from = new GenericLocation(lat, lon);
    }

    public void setOrigin(OtpsIndividual origin) {
        this.setOrigin(origin.lat, origin.lon);
    }

    public void setDestination(double lat, double lon) {
        req.to = new GenericLocation(lat, lon);
    }

    public void setDestination(OtpsIndividual dest) {
        this.setDestination(dest.lat, dest.lon);
    }

    public void setBannedRoutes(String routeSpecList) {
        req.bannedRoutes = RouteMatcher.parse(routeSpecList);
    }
}
