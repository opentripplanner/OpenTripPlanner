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

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import lombok.Setter;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.patch.AlertPatch;
import org.opentripplanner.routing.patch.Patch;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.Map.Entry;

@Component
public class PatchServiceImpl implements PatchService {

    private Graph graph;

    @Autowired
    @Setter
    private GraphService graphService;

    private Map<String, Patch> patches = new HashMap<String, Patch>();
    private ListMultimap<AgencyAndId, Patch> patchesByRoute = LinkedListMultimap.create();
    private ListMultimap<AgencyAndId, Patch> patchesByStop = LinkedListMultimap.create();

    protected PatchServiceImpl() {
    }

    public PatchServiceImpl(Graph graph) {
        this.graph = graph;
    }

    @Override
    public Collection<Patch> getAllPatches() {
        return patches.values();
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
    public Collection<Patch> getRoutePatches(AgencyAndId route) {
        List<Patch> result = patchesByRoute.get(route);
        if (result == null) {
            result = Collections.emptyList();
        }
        return result;

    }

    @Override
    public synchronized void apply(Patch patch) {
        if (graph == null)
            graph = graphService.getGraph();

        if (patches.containsKey(patch.getId())) {
            expire(patches.get(patch.getId()));
        }

        patch.apply(graph);
        patches.put(patch.getId(), patch);
        if (patch instanceof AlertPatch) {
            AlertPatch alertPatch = (AlertPatch) patch;
            AgencyAndId stop = alertPatch.getStop();
            if (stop != null) {
                patchesByStop.put(stop, patch);
            }
            AgencyAndId route = alertPatch.getRoute();
            if (route != null) {
                patchesByRoute.put(route, patch);
            }
        }

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
        if (graph == null)
            graph = graphService.getGraph();

        if (patch instanceof AlertPatch) {
            AlertPatch alertPatch = (AlertPatch) patch;
            AgencyAndId stop = alertPatch.getStop();
            if (stop != null) {
                patchesByStop.remove(stop, patch);
            }
            AgencyAndId route = alertPatch.getRoute();
            if (route != null) {
                patchesByRoute.remove(route, patch);
            }
        }

        patch.remove(graph);
    }
}
