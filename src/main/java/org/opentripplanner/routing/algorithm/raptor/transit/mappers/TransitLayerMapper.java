package org.opentripplanner.routing.algorithm.raptor.transit.mappers;

import org.opentripplanner.model.Stop;
import org.opentripplanner.routing.algorithm.raptor.transit.Transfer;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.TripPatternForDate;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.ServiceCalendarMapper.mapServiceCodesByLocalDates;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.StopIndexMapper.mapIndexByStop;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransfersMapper.mapTransfers;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TripPatternMapper.mapPatternsByServiceCode;
import static org.opentripplanner.routing.algorithm.raptor.transit.mappers.TripPatternMapper.mapTripPatternsByStopDate;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed,
 * with service days at the top level, which contains TripPatternForDate objects that contain
 * only TripSchedules running on that particular date. This makes it faster to filter out
 * TripSchedules when doing Range Raptor searches.
 */

public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private final Graph graph;


    private TransitLayerMapper(Graph graph) {
        this.graph = graph;
    }

    public static TransitLayer map(Graph graph) {
        return new TransitLayerMapper(graph).map();
    }

    private TransitLayer map() {
        List<Stop> stopsByIndex;
        Map<Stop, Integer> indexByStop;
        HashMap<LocalDate, List<TripPatternForDate>> tripPatternsByStopByDate;
        List<List<Transfer>> transferByStopIndex;

        LOG.info("Mapping transitLayer from Graph...");

        stopsByIndex = mapStopsByIndex();
        indexByStop = mapIndexByStop(stopsByIndex);
        tripPatternsByStopByDate = mapTripPatterns(indexByStop);
        transferByStopIndex = mapTransfers(graph.index.stopVertexForStop, stopsByIndex, indexByStop);

        LOG.info("Mapping complete.");

        return new TransitLayer(tripPatternsByStopByDate, transferByStopIndex, stopsByIndex, indexByStop);
    }

    private ArrayList<Stop> mapStopsByIndex() {
        return new ArrayList<>(this.graph.index.stopForId.values());
    }

    /**
     * Map trip tripPatterns and trips to Raptor classes
     */
    private HashMap<LocalDate, List<TripPatternForDate>> mapTripPatterns(
            Map<Stop, Integer> indexByStop
    ) {
        return mapTripPatternsByStopDate(
                mapPatternsByServiceCode(indexByStop,  originalTripPatterns()),
                mapServiceCodesByLocalDates(graph.getCalendarService(), graph.serviceCodes)
        );
    }

    private Collection<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns() {
        return graph.tripPatternForId.values();
    }
}
