package org.opentripplanner.routing.algorithm.raptor.transit_layer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.raptor.transit_data_provider.TripSchedule;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maps the TransitLayer object from the OTP Graph object. The ServiceDay hierarchy is reversed, with service days at
 * the top level, which contains TripPatternForDate objects that contain only TripSchedules running on that particular
 * date. This makes it faster to filter out TripSchedules when doing Range Raptor searches.
 */

public class TransitLayerMapper {

    private static final Logger LOG = LoggerFactory.getLogger(TransitLayerMapper.class);

    private Graph graph;
    private TransitLayer transitLayer;

    public TransitLayer map(Graph graph) {
        this.graph = graph;
        this.transitLayer = new TransitLayer();
        LOG.info("Mapping transitLayer from Graph...");
        createStopMaps();
        mapTripPatterns();
        mapTransfers();
        LOG.info("Mapping complete.");
        return this.transitLayer;
    }

    /** Create maps between stop indices used by Raptor and stop objects in original graph */
    private void createStopMaps() {
        ArrayList<Stop> stops = new ArrayList<>(graph.index.stopForId.values());
        transitLayer.stopsByIndex = new ArrayList<>();
        transitLayer.indexByStop = new HashMap<>();
        for (int i = 0; i < stops.size(); i++) {
            Stop currentStop = stops.get(i);
            transitLayer.stopsByIndex.add(currentStop);
            transitLayer.indexByStop.put(currentStop, i);
        }
    }

    /** Map trip tripPatterns and trips to Raptor classes */
    private void mapTripPatterns() {
        List<org.opentripplanner.routing.edgetype.TripPattern> originalTripPatterns = new ArrayList<>(graph.index.patternForId.values());

        Multimap<Integer, TripPattern> patternsByServiceCode = HashMultimap.create();

        int patternId = 0;
        for (org.opentripplanner.routing.edgetype.TripPattern tripPattern : originalTripPatterns) {
            List<TripSchedule> tripSchedules = new ArrayList<>();
            int[] stopPattern = new int[tripPattern.stopPattern.size];

            List<TripTimes> sortedTripTimes = tripPattern.scheduledTimetable.tripTimes.stream()
                    .sorted(Comparator.comparing(t -> t.getArrivalTime(0)))
                    .collect(Collectors.toList());

            TripPattern newTripPattern = new TripPattern(
                    patternId++,
                    tripSchedules,
                    tripPattern.mode,
                    stopPattern
            );

            for (TripTimes tripTimes : sortedTripTimes) {
                TripScheduleImpl tripSchedule = new TripScheduleImpl(
                    new int[stopPattern.length],
                    new int[stopPattern.length],
                    tripTimes.trip,
                    tripPattern,
                    tripTimes.serviceCode
                );

                for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                    tripSchedule.setArrival(i, tripTimes.getArrivalTime(i));
                    tripSchedule.setDeparture(i, tripTimes.getDepartureTime(i));
                }

                patternsByServiceCode.put(tripTimes.serviceCode, newTripPattern);
                tripSchedules.add(tripSchedule);
            }

            for (int i = 0; i < tripPattern.stopPattern.size; i++) {
                int stopIndex = transitLayer.indexByStop.get(tripPattern.getStop(i));
                newTripPattern.getStopPattern()[i] = stopIndex;
            }
        }

        Multimap<LocalDate, Integer> serviceCodesByLocalDates = HashMultimap.create();

        for (Iterator<FeedScopedId> it = graph.getCalendarService().getServiceIds().iterator(); it.hasNext();) {
            FeedScopedId serviceId = it.next();
            Set<LocalDate> localDates = graph.getCalendarService().getServiceDatesForServiceId(serviceId)
                    .stream().map(this::localDateFromServiceDate).collect(Collectors.toSet());
            int serviceIndex = graph.serviceCodes.get(serviceId);

            for (LocalDate date : localDates) {
                serviceCodesByLocalDates.put(date, serviceIndex);
            }
        }

        transitLayer.tripPatternsForDate = new HashMap<>();

        for (Map.Entry<LocalDate, Collection<Integer>> serviceEntry : serviceCodesByLocalDates.asMap().entrySet()) {
            Set<Integer> servicesForDate = new HashSet<>(serviceEntry.getValue());
            List<TripPattern> filteredPatterns = serviceEntry.getValue().stream().map(s -> patternsByServiceCode.get(s))
                    .flatMap(Collection::stream) .collect(Collectors.toList());

            List<TripPatternForDate> tripPatternsForDate = new ArrayList<>();

            for (TripPattern tripPattern : filteredPatterns) {
                List<TripSchedule> tripSchedules = new ArrayList<>(tripPattern.getTripSchedules());

                tripSchedules = tripSchedules.stream().filter(t -> servicesForDate.contains(t.getServiceCode()))
                        .collect(Collectors.toList());

                TripPatternForDate tripPatternForDate = new TripPatternForDate(
                        tripPattern,
                        tripSchedules);

                tripPatternsForDate.add(tripPatternForDate);
            }

            transitLayer.tripPatternsForDate.put(serviceEntry.getKey(), tripPatternsForDate);
    }

        // Sort by TripPattern for easier merging in OtpRRDataProvider
        transitLayer.tripPatternsForDate.entrySet().stream()
                .forEach(t -> t.getValue()
                        .sort((p1, p2) -> Integer.compare(p1.getTripPattern().getId(), p2.getTripPattern().getId())));
    }

    /** Copy pre-calculated transfers from the original graph */
    private void mapTransfers() {
        transitLayer.transferByStopIndex = Stream.generate(ArrayList<Transfer>::new)
                .limit(transitLayer.stopsByIndex.size()).collect(Collectors.toList());
        for (int i = 0; i < transitLayer.stopsByIndex.size(); i++) {
            for (Edge edge : graph.index.stopVertexForStop.get(transitLayer.stopsByIndex.get(i)).getOutgoing()) {
                if (edge instanceof SimpleTransfer) {
                    int stopIndex = transitLayer.indexByStop.get(((TransitStop)edge.getToVertex()).getStop());
                    double distance = edge.getDistance();

                    Transfer transfer = new Transfer(
                            stopIndex,
                            (int)distance,
                            Arrays.asList(edge.getGeometry().getCoordinates()));

                    transitLayer.transferByStopIndex.get(i).add(transfer);
                }
            }
        }
    }

    private LocalDate localDateFromServiceDate(ServiceDate serviceDate) {
        return LocalDate.of(serviceDate.getYear(), serviceDate.getMonth(), serviceDate.getDay());
    }
}
