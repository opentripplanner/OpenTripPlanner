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
package org.opentripplanner.routing.services.notes;

import org.opentripplanner.api.model.TripPlan;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class TripPlanInPastNotesService implements Serializable, PlanNotesService {

    private static final long serialVersionUID = 1;

    private String message;

    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public Collection<Alert> getAlerts(RoutingRequest request, TripPlan plan) {
        Collection<Alert> alerts = new ArrayList<>();
        long maxStartTime = plan.itinerary.stream()
                .mapToLong(i -> i.startTime.getTimeInMillis())
                .max().orElse(Long.MAX_VALUE);
        if (maxStartTime < request.rctx.debugOutput.getStartedCalculating()) {
            alerts.add(Alert.createSimpleAlerts(message));
        }
        return alerts;
    }
}
