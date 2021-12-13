package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.onebusaway.gtfs.impl.translation.TranslationServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.translation.TranslationServiceImpl;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.BoardingArea;
import org.opentripplanner.model.Entrance;
import org.opentripplanner.model.PathwayNode;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.util.I18NString;
import org.opentripplanner.util.TranslatedString;

/**
 * This class is responsible for mapping between GTFS DAO objects and into OTP Transit model.
 * General mapping code or reusable bussiness logic should be moved into the Builder; hence
 * reusable for other import modules.
 */
public class GTFSToOtpTransitServiceMapper {
    private final AgencyMapper agencyMapper;

    private final StationMapper stationMapper = new StationMapper();

    private final StopMapper stopMapper = new StopMapper();

    private final EntranceMapper entranceMapper = new EntranceMapper();

    private final PathwayNodeMapper pathwayNodeMapper = new PathwayNodeMapper();

    private final BoardingAreaMapper boardingAreaMapper = new BoardingAreaMapper();

    private final LocationMapper locationMapper = new LocationMapper();

    private final LocationGroupMapper locationGroupMapper = new LocationGroupMapper(
        stopMapper,
        locationMapper
    );

    private final FareAttributeMapper fareAttributeMapper = new FareAttributeMapper();

    private final ServiceCalendarDateMapper serviceCalendarDateMapper = new ServiceCalendarDateMapper();

    private final FeedInfoMapper feedInfoMapper;

    private final ShapePointMapper shapePointMapper = new ShapePointMapper();

    private final ServiceCalendarMapper serviceCalendarMapper = new ServiceCalendarMapper();

    private final PathwayMapper pathwayMapper = new PathwayMapper(
        stopMapper,
        entranceMapper,
        pathwayNodeMapper,
        boardingAreaMapper
    );

    private final RouteMapper routeMapper;

    private final TripMapper tripMapper;

    private final BookingRuleMapper bookingRuleMapper;

    private final StopTimeMapper stopTimeMapper;

    private final FrequencyMapper frequencyMapper;

    private final FareRuleMapper fareRuleMapper;

    private final DataImportIssueStore issueStore;

    private final GtfsRelationalDao data;

    private final OtpTransitServiceBuilder builder = new OtpTransitServiceBuilder();

    private static final String NAME_FIELD_NAME = "name";

    private static final String URL_FIELD_NAME = "url";

    public GTFSToOtpTransitServiceMapper(
            String feedId,
            DataImportIssueStore issueStore,
            GtfsRelationalDao data
    ) {
        this.issueStore = issueStore;
        this.data = data;
        feedInfoMapper = new FeedInfoMapper(feedId);
        agencyMapper = new AgencyMapper(feedId);
        routeMapper = new RouteMapper(agencyMapper, issueStore);
        tripMapper = new TripMapper(routeMapper);
        bookingRuleMapper = new BookingRuleMapper();
        stopTimeMapper = new StopTimeMapper(stopMapper, locationMapper, locationGroupMapper, tripMapper, bookingRuleMapper);
        frequencyMapper = new FrequencyMapper(tripMapper);
        fareRuleMapper = new FareRuleMapper(
            routeMapper, fareAttributeMapper
        );
    }

    public OtpTransitServiceBuilder getBuilder() {
        return builder;
    }

    public void mapStopTripAndRouteDatantoBuilder() {

        TranslationServiceImpl ts = new TranslationServiceImpl();
        ts.setData(TranslationServiceDataFactoryImpl.createData(data));
        Set<String> languages = getLanguagesFromTranslations(data.getAllTranslations());
        //add also feed info's language if not exists translated languages on translations.txt
        if (data.getAllFeedInfos().iterator().hasNext()) {
            languages.add(data.getAllFeedInfos().iterator().next().getLang());
        }
        
        builder.getAgenciesById().addAll(agencyMapper.map(data.getAllAgencies()));
        builder.getCalendarDates().addAll(serviceCalendarDateMapper.map(data.getAllCalendarDates()));
        builder.getCalendars().addAll(serviceCalendarMapper.map(data.getAllCalendars()));
        builder.getFareAttributes().addAll(fareAttributeMapper.map(data.getAllFareAttributes()));
        builder.getFareRules().addAll(fareRuleMapper.map(data.getAllFareRules()));
        builder.getFeedInfos().addAll(feedInfoMapper.map(data.getAllFeedInfos()));
        builder.getFrequencies().addAll(frequencyMapper.map(data.getAllFrequencies()));
        builder.getRoutes().addAll(routeMapper.map(data.getAllRoutes()));
        for (ShapePoint shapePoint : shapePointMapper.map(data.getAllShapePoints())) {
            builder.getShapePoints().put(shapePoint.getShapeId(), shapePoint);
        }

        mapGtfsStopsToOtpTypes(data, ts, languages);

        builder.getLocations().addAll(locationMapper.map(data.getAllLocations()));
        builder.getLocationGroups().addAll(locationGroupMapper.map(data.getAllLocationGroups()));
        builder.getPathways().addAll(pathwayMapper.map(data.getAllPathways()));
        builder.getStopTimesSortedByTrip().addAll(stopTimeMapper.map(data.getAllStopTimes()));
        builder.getTripsById().addAll(tripMapper.map(data.getAllTrips()));

        mapAndAddTransfersToBuilder();
    }

    /**
     * Note! Trip-pattens must be added BEFORE mapping transfers
     */
    private void mapAndAddTransfersToBuilder() {
        TransferMapper transferMapper = new TransferMapper(
                routeMapper,
                stationMapper,
                stopMapper,
                tripMapper,
                builder.getStopTimesSortedByTrip()
        );
        builder.getTransfers().addAll(transferMapper.map(data.getAllTransfers()));
    }

    private Set<String> getLanguagesFromTranslations(Collection allTranslations) {
        Set<String> languages = new HashSet<>(Set.of());
        java.util.Iterator<org.onebusaway.gtfs.model.Translation> iterator =
                allTranslations.iterator();
        while (iterator.hasNext()) {
            org.onebusaway.gtfs.model.Translation translation = iterator.next();
            languages.add(translation.getLanguage());
        }
        return languages;
    }

    private I18NString getTranslations(
            TranslationServiceImpl ts,
            Set<String> languages,
            Object object,
            String fieldName
    ) {
        HashMap<String, String> translations = new HashMap<>();
        for (String lang: languages) {
            if (object instanceof org.onebusaway.gtfs.model.Stop) {
                org.onebusaway.gtfs.model.Stop t = ts.getTranslatedEntity(lang, org.onebusaway.gtfs.model.Stop.class,
                        (org.onebusaway.gtfs.model.Stop) object
                );
                if (fieldName.equals(NAME_FIELD_NAME)) {
                    translations.put(lang, t.getName());
                } else if (fieldName.equals(URL_FIELD_NAME)) {
                    translations.put(lang, t.getUrl());
                }
            }
        }
        return TranslatedString.getI18NString(translations);
    }

    private void mapGtfsStopsToOtpTypes(GtfsRelationalDao data, TranslationServiceImpl ts, Set<String> languages) {
        StopToParentStationLinker stopToParentStationLinker = new StopToParentStationLinker(issueStore);
        for (org.onebusaway.gtfs.model.Stop it : data.getAllStops()) {
            if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STOP) {
                Stop stop = stopMapper.map(it, getTranslations(ts, languages, it, NAME_FIELD_NAME), getTranslations(ts, languages, it, URL_FIELD_NAME));
                builder.getStops().add(stop);
                stopToParentStationLinker.addStationElement(stop, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_STATION) {
                Station station = stationMapper.map(it);
                builder.getStations().add(station);
                stopToParentStationLinker.addStation(station);
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_ENTRANCE_EXIT) {
                Entrance entrance = entranceMapper.map(it, getTranslations(ts, languages, it, NAME_FIELD_NAME));
                builder.getEntrances().add(entrance);
                stopToParentStationLinker.addStationElement(entrance, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_NODE) {
                PathwayNode pathwayNode = pathwayNodeMapper.map(it);
                builder.getPathwayNodes().add(pathwayNode);
                stopToParentStationLinker.addStationElement(pathwayNode, it.getParentStation());
            } else if(it.getLocationType() == org.onebusaway.gtfs.model.Stop.LOCATION_TYPE_BOARDING_AREA) {
                BoardingArea boardingArea = boardingAreaMapper.map(it, getTranslations(ts, languages, it, NAME_FIELD_NAME));
                builder.getBoardingAreas().add(boardingArea);
                stopToParentStationLinker.addBoardingArea(boardingArea, it.getParentStation());
            }
        }

        stopToParentStationLinker.link();
    }
}
