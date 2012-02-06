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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.patch.AlertPatch;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.opentripplanner.util.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchServiceImpl implements PatchService {

    private GraphService graphService;

    private HashMap<String, Patch> patches = new HashMap<String, Patch>();
    private HashMap<AgencyAndId,List<Patch>> patchesByRoute = new HashMap<AgencyAndId, List<Patch>>();
    private HashMap<AgencyAndId, List<Patch>> patchesByStop = new HashMap<AgencyAndId, List<Patch>>();

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    @Override
    public Collection<Patch> getStopPatches(AgencyAndId stop) {
        List<Patch> result = patchesByStop.get(stop);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;
    }

    @Override
    public synchronized void apply(Patch patch) {
        Graph graph = graphService.getGraph();

        if(patches.containsKey(patch.getId())) {
            expire(patches.get(patch.getId()));
        }

        patch.apply(graph);
        patches.put(patch.getId(), patch);
        if (patch instanceof AlertPatch) {
            AlertPatch alertPatch = (AlertPatch) patch;
            AgencyAndId stop = alertPatch.getStop();
            if (stop != null) {
                MapUtils.addToMapList(patchesByStop, stop, patch);
            }
            AgencyAndId route = alertPatch.getRoute();
            if (route != null) {
                MapUtils.addToMapList(patchesByRoute, stop, patch);
            }
        }

    }

    @Override
    public Collection<Patch> getRoutePatches(AgencyAndId route) {
        List<Patch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;

    }

    @Override
    public void expire(Set<String> purge) {
        for (String patchId : purge) {
            if (patches.containsKey(patchId)) {
                expire(patches.get(patchId));
            }
        }

        patches.keySet().removeAll(purge);
    }

    @Override
    public void expireAll() {
        for (Patch patch : patches.values()) {
            expire(patch);
        }
        patches.clear();
    }

    @Override
    public void expireAllExcept(Set<String> retain) {
        ArrayList<String> toRemove = new ArrayList<String>();

        for (Entry<String, Patch> entry : patches.entrySet()) {
            final String key = entry.getKey();
            if (!retain.contains(key)) {
                toRemove.add(key);
                expire(entry.getValue());
            }
        }
        patches.keySet().removeAll(toRemove);
    }

    private void expire(Patch patch) {
        Graph graph = graphService.getGraph();

        if (patch instanceof AlertPatch) {
            AlertPatch alertPatch = (AlertPatch) patch;
            AgencyAndId stop = alertPatch.getStop();
            if (stop != null) {
                MapUtils.removeFromMapList(patchesByStop, stop, patch);
            }
            AgencyAndId route = alertPatch.getRoute();
            if (route != null) {
                MapUtils.removeFromMapList(patchesByRoute, stop, patch);
            }
        }

        patch.remove(graph);
    }
}
