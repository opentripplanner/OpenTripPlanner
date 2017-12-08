package org.opentripplanner.netex.loader;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.rutebanken.netex.model.Authority;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DestinationDisplay;
import org.rutebanken.netex.model.GroupOfLines;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.Line;
import org.rutebanken.netex.model.Network;
import org.rutebanken.netex.model.OperatingPeriod;
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

    private final Map<String, Authority> authoritiesById = new HashMap<>();

    private final Map<String, Authority> authoritiesByGroupOfLinesId = new HashMap<>();

    private final Map<String, Authority> authoritiesByNetworkId = new HashMap<>();

    private final Map<String, GroupOfLines> groupOfLinesByLineId = new HashMap<>();

    private final Map<String, GroupOfLines> groupOfLinesById = new HashMap<>();

    private final Map<String, Network> networkByLineId = new HashMap<>();

    private final Map<String, Network> networkById = new HashMap<>();

    private final Set<String> calendarServiceIds = new HashSet<>();

    private final Multimap<String, StopPlace> stopPlaceById = ArrayListMultimap.create();

    private final Multimap<String, Quay> quayById = ArrayListMultimap.create();

    private final Map<String, DestinationDisplay> destinationDisplayMap = new HashMap<>();

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

    public void addAuthority(Authority authority){
        authoritiesById.put(authority.getId(), authority);
    }

    /**
     * Lookup Authority in this class and if not found delegate up to the parent NetexDao.
     */
    public Authority lookupAuthorityById(String id){
        Authority v = authoritiesById.get(id);
        return returnLocalValue(v) ? v : parent.lookupAuthorityById(id);
    }

    public Collection<Authority> getAuthorities(){
        return authoritiesById.values();
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

    public void addAuthorityByGroupOfLinesId(Authority authority, String groupOfLinesId) {
        authoritiesByGroupOfLinesId.put(groupOfLinesId, authority);
    }

    /**
     * Lookup authority in this class and if not found delegate up to the parent NetexDao.
     */
    public Authority lookupAuthorityByGroupOfLinesId(String groupOfLinesId) {
        Authority v = authoritiesByGroupOfLinesId.get(groupOfLinesId);
        return returnLocalValue(v) ? v : parent.lookupAuthorityByGroupOfLinesId(groupOfLinesId);
    }

    public void addAuthorityByNetworkId(Authority authority, String networkId) {
        authoritiesByNetworkId.put(networkId, authority);
    }

    /**
     * Lookup authority in this class and if not found delegate up to the parent NetexDao.
     */
    public Authority lookupAuthorityByNetworkId(String networkId) {
        Authority v = authoritiesByNetworkId.get(networkId);
        return returnLocalValue(v) ? v : parent.lookupAuthorityByNetworkId(networkId);
    }

    public void addGroupOfLines(GroupOfLines groupOfLines) {
        groupOfLinesById.put(groupOfLines.getId(), groupOfLines);
    }

    /**
     * Lookup authority in this class and if not found delegate up to the parent NetexDao.
     */
    public GroupOfLines lookupGroupOfLinesById(String id) {
        GroupOfLines v = groupOfLinesById.get(id);
        return returnLocalValue(v) ? v : parent.lookupGroupOfLinesById(id);
    }

    public void addGroupOfLinesByLineId(GroupOfLines groupOfLines, String lineId) {
        groupOfLinesByLineId.put(lineId, groupOfLines);
    }

    /**
     * Lookup authority in this class and if not found delegate up to the parent NetexDao.
     */
    public GroupOfLines lookupGroupOfLinesByLineId(String lineId) {
        GroupOfLines v = groupOfLinesByLineId.get(lineId);
        return returnLocalValue(v) ? v : parent.lookupGroupOfLinesByLineId(lineId);
    }

    public void addNetworkByLineId(Network network, String lineId) {
        networkByLineId.put(lineId, network);
    }

    public Network lookupNetworkByLineId(String lineId) {
        Network v = networkByLineId.get(lineId);
        return returnLocalValue(v) ? v : parent.lookupNetworkByLineId(lineId);
    }

    public void addNetwork(Network network) {
        networkById.put(network.getId(), network);
    }

    public Network lookupNetworkById(String networkId) {
        Network v = networkById.get(networkId);
        return returnLocalValue(v) ? v : parent.lookupNetworkById(networkId);
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

    public Map<String, DestinationDisplay> getDestinationDisplayMap() {
        return destinationDisplayMap;
    }
}
