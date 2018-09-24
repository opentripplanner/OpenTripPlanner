package org.opentripplanner.gtfs;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.gtfs.mapping.AgencyAndIdMapper;
import org.opentripplanner.gtfs.mapping.GTFSToOtpTransitServiceMapper;
import org.opentripplanner.model.OtpTransitService;

import java.io.File;
import java.io.IOException;

public class MockGtfs {

    private final org.onebusaway.gtfs.services.MockGtfs gtfsDelegate;

    private MockGtfs(org.onebusaway.gtfs.services.MockGtfs gtfsDelegate) {
        this.gtfsDelegate = gtfsDelegate;
    }

    public MockGtfs(File path) {
        this(new org.onebusaway.gtfs.services.MockGtfs(path));
    }

    public static MockGtfs create() throws IOException {
        return new MockGtfs(org.onebusaway.gtfs.services.MockGtfs.create());
    }

    public File getPath() {
        return gtfsDelegate.getPath();
    }

    public void putFile(String fileName, String content) {
        gtfsDelegate.putFile(fileName, content);
    }

    public void putFile(String fileName, File file) throws IOException {
        gtfsDelegate.putFile(fileName, file);
    }

    public void putLines(String fileName, String... rows) {
        gtfsDelegate.putLines(fileName, rows);
    }

    public OtpTransitService read() throws IOException {
        return GTFSToOtpTransitServiceMapper.mapGtfsDaoToOTPTransitService(gtfsDelegate.read());
    }

    public OtpTransitService read(org.onebusaway.gtfs.serialization.GtfsReader reader) throws IOException {
        return GTFSToOtpTransitServiceMapper.mapGtfsDaoToOTPTransitService(gtfsDelegate.read(reader));
    }

    public void putMinimal() {
        gtfsDelegate.putMinimal();
    }

    public void putAgencies(int numberOfRows, String... columns) {
        gtfsDelegate.putAgencies(numberOfRows, columns);
    }

    public void putDefaultAgencies() {
        gtfsDelegate.putDefaultAgencies();
    }

    public void putRoutes(int numberOfRows, String... columns) {
        gtfsDelegate.putRoutes(numberOfRows, columns);
    }

    public void putDefaultRoutes() {
        gtfsDelegate.putDefaultRoutes();
    }

    public void putStops(int numberOfRows, String... columns) {
        gtfsDelegate.putStops(numberOfRows, columns);
    }

    public void putDefaultStops() {
        gtfsDelegate.putDefaultStops();
    }

    public void putCalendars(int numberOfServiceIds, String... columns) {
        gtfsDelegate.putCalendars(numberOfServiceIds, columns);
    }

    public void putDefaultCalendar() {
        gtfsDelegate.putDefaultCalendar();
    }

    public void putCalendarDates(String... specs) {
        gtfsDelegate.putCalendarDates(specs);
    }

    public void putTrips(int numberOfRows, String routeIds, String serviceIds, String... columns) {
        gtfsDelegate.putTrips(numberOfRows, routeIds, serviceIds, columns);
    }

    public void putDefaultTrips() {
        gtfsDelegate.putDefaultTrips();
    }

    public void putStopTimes(String tripIds, String stopIds) {
        gtfsDelegate.putStopTimes(tripIds, stopIds);
    }

    public void putDefaultStopTimes() {
        gtfsDelegate.putDefaultStopTimes();
    }

    /**
     * @return a full id with the default agency id ("a0") for the feed.
     */
    public FeedScopedId id(String id) {
        return AgencyAndIdMapper.mapAgencyAndId(gtfsDelegate.id(id));
    }
}
