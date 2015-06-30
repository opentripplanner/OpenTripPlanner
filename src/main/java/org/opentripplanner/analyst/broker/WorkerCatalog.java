package org.opentripplanner.analyst.broker;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class WorkerCatalog {

    Map<String, WorkerObservation> observationsByWorkerId = new HashMap<>();
    Multimap<String, String> workersByGraph = HashMultimap.create();

    // How many workers we would ideally like to have given the number of jobs and tasks
    int targetWorkerCount;

    // We want to store integral target worker counts rather than fractional proportions to avoid "hunting" behavior.
    // The target quantities will all be integers so it is clear when they are reached.
    TObjectIntMap<String> targetWorkerCountPerGraph = new TObjectIntHashMap<>();

    // and function to update target counts based on jobs queue.

    public synchronized void catalog (String workerId, String graphAffinity) {
        WorkerObservation observation = new WorkerObservation(workerId, graphAffinity);
        WorkerObservation oldObservation = observationsByWorkerId.put(workerId, observation);
        if (oldObservation != null) {
            workersByGraph.remove(oldObservation.graphAffinity, workerId);
        }
        workersByGraph.put(graphAffinity, workerId);
    }

    public synchronized void purgeDeadWorkers () {
        long now = System.currentTimeMillis();
        long oldestAcceptable = now - 2 * 60 * 1000;
        List<WorkerObservation> ancientObservations = observationsByWorkerId.values().stream()
                .filter(o -> o.lastSeen < oldestAcceptable).collect(Collectors.toList());
        ancientObservations.forEach(o -> {
            observationsByWorkerId.remove(o.workerId);
            workersByGraph.remove(o.graphAffinity, o.workerId);
        });
    }

    public synchronized void updateTargetWorkerCounts (Multimap<String, String> activeJobsPerGraph) {

        final int activeWorkerCount = observationsByWorkerId.size(); // (plus outstanding instance requests)
        final int activeJobsCount = activeJobsPerGraph.size();

        // For now just distribute among all jobs equally, without weighting by users.
        activeJobsPerGraph.asMap().forEach((g, js) -> {
            targetWorkerCountPerGraph.put(g, js.size() * activeWorkerCount / activeJobsCount); // FIXME this will round down and waste workers
        });

    }

    /** Returns true if it is OK to steal a worker toward this graphId. */
    boolean notEnoughWorkers (String graphId) {
        return targetWorkerCountPerGraph.get(graphId) > workersByGraph.get(graphId).size();
    }

    /** Returns true if it is OK to steal a worker _away_ from this graphId. */
    boolean tooManyWorkers (String graphId) {
        return targetWorkerCountPerGraph.get(graphId) < workersByGraph.get(graphId).size();
    }

    /**
     * Returns a list of graphIds beginning with the supplied one, then moving on to any others that have too many
     * workers on them.
     */
    public List<String> orderedStealingList(String graphId) {
        return null;
    }

    public int size () {
        return workersByGraph.size();
    }

}
