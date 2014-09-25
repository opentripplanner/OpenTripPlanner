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

package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Context;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.routing.services.GraphService;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import org.opentripplanner.standalone.OTPApplication;
import org.opentripplanner.standalone.OTPServer;

public class AlertPatchServiceImpl implements AlertPatchService {

    private Graph graph;

    private Map<String, AlertPatch> alertPatches = new HashMap<String, AlertPatch>();
    private ListMultimap<AgencyAndId, AlertPatch> patchesByRoute = LinkedListMultimap.create();
    private ListMultimap<AgencyAndId, AlertPatch> patchesByStop = LinkedListMultimap.create();

    public AlertPatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<AlertPatch> getAllAlertPatches() {
        return alertPatches.values();
    }

    @Override
    public Collection<AlertPatch> getStopPatches(AgencyAndId stop) {
        List<AlertPatch> result = patchesByStop.get(stop);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public Collection<AlertPatch> getRoutePatches(AgencyAndId route) {
        List<AlertPatch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;

    }

    @Override
    public synchronized void apply(AlertPatch alertPatch) {
        if (alertPatches.containsKey(alertPatch.getId())) {
            expire(alertPatches.get(alertPatch.getId()));
        }

        alertPatch.apply(graph);
        alertPatches.put(alertPatch.getId(), alertPatch);

        AgencyAndId stop = alertPatch.getStop();
        if (stop != null) {
            patchesByStop.put(stop, alertPatch);
        }
        AgencyAndId route = alertPatch.getRoute();
        if (route != null) {
            patchesByRoute.put(route, alertPatch);
        }
    }

    @Override
    public void expire(Set<String> purge) {
        for (String patchId : purge) {
            if (alertPatches.containsKey(patchId)) {
                expire(alertPatches.get(patchId));
            }
        }

        alertPatches.keySet().removeAll(purge);
    }

    @Override
    public void expireAll() {
        for (AlertPatch alertPatch : alertPatches.values()) {
            expire(alertPatch);
        }
        alertPatches.clear();
    }

    @Override
    public void expireAllExcept(Set<String> retain) {
        ArrayList<String> toRemove = new ArrayList<String>();

        for (Entry<String, AlertPatch> entry : alertPatches.entrySet()) {
            final String key = entry.getKey();
            if (!retain.contains(key)) {
                toRemove.add(key);
                expire(entry.getValue());
            }
        }
        alertPatches.keySet().removeAll(toRemove);
    }

    private void expire(AlertPatch alertPatch) {
        AgencyAndId stop = alertPatch.getStop();
        if (stop != null) {
            patchesByStop.remove(stop, alertPatch);
        }
        AgencyAndId route = alertPatch.getRoute();
        if (route != null) {
            patchesByRoute.remove(route, alertPatch);
        }

        alertPatch.remove(graph);
    }
}
