package org.opentripplanner.netex.loader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.Operator;
import org.rutebanken.netex.model.Quay;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.StopPlace;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class holds indexes of Netex objects for lookup during
 * the NeTEx import.
 * <p>
 * A NeTEx import is grouped into several levels: <em>shard data</em>, <em>group shared data</em>,
 * and <em>singel files</em>. To discard objects not needed any more; this class support the
 * creation of multiple levels, by storing a refernece to a parent at an higher level. All
 * <code>lookupX</code> methods first look in the local index, and then if nothing is found
 * delegate the lookup to its parent.
 * <p>
 * Accessors like {@link #getStopPlaceIds()} return ONLY the local elements, not elements
 * present in the parent NetexDao.
 */
public class NetexDao {
    private final Map<String, String> quayIdByStopPointRef = new HashMap<>();

    private final Map<String, JourneyPattern> journeyPatternsById = new HashMap<>();

    private final Map<String, Route> routeById = new HashMap<>();

    private final Map<String, Line> lineById = new HashMap<>();

    private final Multimap<String, ServiceJourney> serviceJourneyById = ArrayListMultimap.create();

    private final Map<String, DayType> dayTypeById = new HashMap<>();

    private final Multimap<String, DayTypeAssignment> dayTypeAssignment = ArrayListMultimap.create();

    private final Map<String, Boolean> dayTypeAvailable = new HashMap<>();

    private final Map<String, OperatingPeriod> operatingPeriodById = new HashMap<>();

    private final Map<String, Operator> operators = new HashMap<>();

    private final Set<String> calendarServiceIds = new HashSet<>();

    private final Multimap<String, StopPlace> stopPlaceById = ArrayListMultimap.create();

    private final Multimap<String, Quay> quayById = ArrayListMultimap.create();

    private String timeZone;

    private final NetexDao parent;

    NetexDao() {
        this.parent = null;
    }

    NetexDao(NetexDao parent) {
        this.parent = parent;
    }

    void addStopPlace(StopPlace stopPlace) {
        stopPlaceById.put(stopPlace.getId(), stopPlace);
    }

    public Set<String> getStopPlaceIds() {
        return stopPlaceById.keySet();
    }

    /**
     * Lookup elements in this class and if not found delegate up to the parent NetexDao.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    public Collection<StopPlace> lookupStopPlacesById(String id) {
        Collection<StopPlace> v = stopPlaceById.get(id);
        return returnLocalValue(v) ? v : parent.lookupStopPlacesById(id);
    }

    void addQuayIdByStopPointRef(String stopPointRef, String quayId) {
        quayIdByStopPointRef.put(stopPointRef, quayId);
    }

    /**
     * Lookup quayId in this class and if not found delegate up to the parent NetexDao.
     */
    public String lookupQuayIdByStopPointRef(String stopRef) {
        String v = quayIdByStopPointRef.get(stopRef);
        return returnLocalValue(v) ? v : parent.lookupQuayIdByStopPointRef(stopRef);
    }

    void addQuay(Quay quay) {
        quayById.put(quay.getId(), quay);
    }

    /**
     * Lookup quay in this class and if not found delegate up to the parent NetexDao.
     */
    public Collection<Quay> lookupQuayById(String id) {
        Collection<Quay> v = quayById.get(id);
        return returnLocalValue(v) ? v : parent.lookupQuayById(id);
    }

    Quay lookupQuayLastVersionById(String id) {
        return lookupQuayById(id).stream()
                .max(Comparator.comparingInt(o2 -> Integer.parseInt(o2.getVersion())))
                .orElse(null);
    }

    void addDayTypeAvailable(String dayType, Boolean available) {
        dayTypeAvailable.put(dayType, available);
    }

    /**
     * Lookup dayType availability in this class and if not found delegate up to the parent NetexDao.
     */
    public Boolean lookupDayTypeAvailable(String dayType) {
        Boolean v = dayTypeAvailable.get(dayType);
        return returnLocalValue(v) ? v : parent.lookupDayTypeAvailable(dayType);
    }

    void addDayTypeAssignment(String ref, DayTypeAssignment assignment) {
        dayTypeAssignment.put(ref, assignment);
    }

    /**
     * Lookup elements in this class and if not found delegate up to the parent NetexDao.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    public Collection<DayTypeAssignment> lookupDayTypeAssignment(String ref) {
        Collection<DayTypeAssignment> v = dayTypeAssignment.get(ref);
        return returnLocalValue(v) ? v : parent.lookupDayTypeAssignment(ref);
    }

    public void addCalendarServiceId(String serviceId) {
        calendarServiceIds.add(serviceId);
    }

    public Iterable<String> getCalendarServiceIds() {
        return calendarServiceIds;
    }

    /**
     * Retrive timezone from this class, if not found delegate up to the parent NetexDao.
     */
    public String getTimeZone() {
        return returnLocalValue(timeZone) ? timeZone : parent.getTimeZone();
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    void addServiceJourneyById(String journeyPatternId, ServiceJourney serviceJourney) {
        serviceJourneyById.put(journeyPatternId, serviceJourney);
    }

    /**
     * Lookup elements in this class and if not found delegate up to the parent NetexDao.
     * NB! elements of this class and its parents are NOT merged, the closest win.
     * @return an empty collection if no element are found.
     */
    public Collection<ServiceJourney> lookupServiceJourneysById(String id) {
        Collection<ServiceJourney> v = serviceJourneyById.get(id);
        return returnLocalValue(v) ? v : parent.lookupServiceJourneysById(id);
    }

    void addJourneyPattern(JourneyPattern journeyPattern) {
        journeyPatternsById.put(journeyPattern.getId(), journeyPattern);
    }

    /**
     * Lookup JourneyPattern in this class and if not found delegate up to the parent NetexDao.
     */
    public JourneyPattern lookupJourneyPatternById(String id) {
        JourneyPattern v = journeyPatternsById.get(id);
        return returnLocalValue(v) ? v : parent.lookupJourneyPatternById(id);
    }

    public Collection<JourneyPattern> getJourneyPatterns() {
        return journeyPatternsById.values();
    }

    void addLine(Line line) {
        lineById.put(line.getId(), line);
    }

    public Collection<Line> getLines() {
        return lineById.values();
    }

    void addRoute(Route route) {
        routeById.put(route.getId(), route);
    }

    /**
     * Lookup route in this class and if not found delegate up to the parent NetexDao.
     */
    public Route lookupRouteById(String id) {
        Route v = routeById.get(id);
        return returnLocalValue(v) ? v : parent.lookupRouteById(id);
    }

    void addDayType(DayType dayType) {
        dayTypeById.put(dayType.getId(), dayType);
    }

    public DayType getDayTypeById(String id) {
        DayType v = dayTypeById.get(id);
        return returnLocalValue(v) ? v : parent.getDayTypeById(id);
    }

    void addOperatingPeriod(OperatingPeriod operatingPeriod) {
        operatingPeriodById.put(operatingPeriod.getId(), operatingPeriod);
    }

    /**
     * Lookup operating period in this class and if not found delegate up to the parent NetexDao.
     */
    public OperatingPeriod lookupOperatingPeriodById(String id) {
        OperatingPeriod v = operatingPeriodById.get(id);
        return returnLocalValue(v) ? v : parent.lookupOperatingPeriodById(id);
    }

    /**
     * @return true if id exist in this class or in one of the parents.
     */
    public boolean operatingPeriodExist(String id) {
        return operatingPeriodById.containsKey(id) ||
                (parent != null && parent.operatingPeriodExist(id));
    }

    void addOperator(Operator operator) {
        operators.put(operator.getId(), operator);
    }

    public Collection<Operator> getOperators() {
        return operators.values();
    }


    /* private methods */

    private boolean returnLocalValue(Object value) {
        return value != null || parent == null;
    }

    private boolean returnLocalValue(Collection value) {
        return notEmpty(value) || parent == null;
    }

    private static boolean notEmpty(Collection c) {
        return !(c == null || c.isEmpty());
    }
}
