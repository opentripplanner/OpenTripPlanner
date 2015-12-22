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

package org.opentripplanner.graph_builder.module;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.*;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl;
import org.opentripplanner.calendar.impl.MultiCalendarServiceImpl;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.services.GraphBuilderModule;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.edgetype.factory.GtfsStopContext;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.FareServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

public class GtfsModule implements GraphBuilderModule {

    private static final Logger LOG = LoggerFactory.getLogger(GtfsModule.class);

    EntityHandler counter = new EntityCounter();

    private FareServiceFactory _fareServiceFactory;

    /** will be applied to all bundles which do not have the cacheDirectory property set */
    private File cacheDirectory; 
    
    /** will be applied to all bundles which do not have the useCached property set */
    private Boolean useCached; 

    Set<String> agencyIdsSeen = Sets.newHashSet();

    int nextAgencyId = 1; // used for generating agency IDs to resolve ID conflicts

    public List<GtfsBundle> gtfsBundles;

    public GtfsModule(List<GtfsBundle> bundles) { this.gtfsBundles = bundles; };

    public List<String> provides() {
        List<String> result = new ArrayList<String>();
        result.add("transit");
        return result;
    }

    public List<String> getPrerequisites() {
        return Collections.emptyList();
    }

    public void setFareServiceFactory(FareServiceFactory factory) {
        _fareServiceFactory = factory;
    }

    @Override
    public void buildGraph(Graph graph, HashMap<Class<?>, Object> extra) {
        // we're about to add another agency to the graph, so clear the cached timezone
        // in case it should change
        // OTP doesn't currently support multiple time zones in a single graph;
        // at least this way we catch the error and log it instead of silently ignoring
        // because the time zone from the first agency is cached
        graph.clearTimeZone();

        MultiCalendarServiceImpl service = new MultiCalendarServiceImpl();
        GtfsStopContext stopContext = new GtfsStopContext();
        
        try {
            for (GtfsBundle gtfsBundle : gtfsBundles) {
                // apply global defaults to individual GTFSBundles (if globals have been set)
                if (cacheDirectory != null && gtfsBundle.cacheDirectory == null)
                    gtfsBundle.cacheDirectory = cacheDirectory;
                if (useCached != null && gtfsBundle.useCached == null)
                    gtfsBundle.useCached = useCached;
                GtfsMutableRelationalDao dao = new GtfsRelationalDaoImpl();
                GtfsContext context = GtfsLibrary.createContext(gtfsBundle.getFeedId(), dao, service);
                GTFSPatternHopFactory hf = new GTFSPatternHopFactory(context);
                hf.setStopContext(stopContext);
                hf.setFareServiceFactory(_fareServiceFactory);
                hf.setMaxStopToShapeSnapDistance(gtfsBundle.getMaxStopToShapeSnapDistance());

                loadBundle(gtfsBundle, graph, dao);

                CalendarServiceDataFactoryImpl csfactory = new CalendarServiceDataFactoryImpl();
                csfactory.setGtfsDao(dao);
                CalendarServiceData data = csfactory.createData();
                service.addData(data, dao);

                hf.subwayAccessTime = gtfsBundle.subwayAccessTime;
                hf.run(graph);

                if (gtfsBundle.doesTransfersTxtDefineStationPaths()) {
                    hf.createTransfersTxtTransfers();
                }
                if (gtfsBundle.linkStopsToParentStations) {
                    hf.linkStopsToParentStations(graph);
                } 
                if (gtfsBundle.parentStationTransfers) {
                    hf.createParentStationTransfers();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // We need to save the calendar service data so we can use it later
        CalendarServiceData data = service.getData();
        graph.putService(CalendarServiceData.class, data);
        graph.updateTransitFeedValidity(data);

        graph.hasTransit = true;
        graph.calculateTransitCenter();

    }

    /****
     * Private Methods
     ****/

    private void loadBundle(GtfsBundle gtfsBundle, Graph graph, GtfsMutableRelationalDao dao)
            throws IOException {

        StoreImpl store = new StoreImpl(dao);
        store.open();
        LOG.info("reading {}", gtfsBundle.toString());

        GtfsFeedId gtfsFeedId = gtfsBundle.getFeedId();

        GtfsReader reader = new GtfsReader();
        reader.setInputSource(gtfsBundle.getCsvInputSource());
        reader.setEntityStore(store);
        reader.setInternStrings(true);
        reader.setDefaultAgencyId(gtfsFeedId.getId());

        if (LOG.isDebugEnabled())
            reader.addEntityHandler(counter);

        if (gtfsBundle.getDefaultBikesAllowed())
            reader.addEntityHandler(new EntityBikeability(true));

        for (Class<?> entityClass : reader.getEntityClasses()) {
            LOG.info("reading entities: " + entityClass.getName());
            reader.readEntities(entityClass);
            store.flush();
            // NOTE that agencies are first in the list and read before all other entity types, so it is effective to
            // set the agencyId here. Each feed ("bundle") is loaded by a separate reader, so there is no risk of
            // agency mappings accumulating.
            if (entityClass == Agency.class) {
                for (Agency agency : reader.getAgencies()) {
                    String agencyId = agency.getId();
                    LOG.info("This Agency has the ID {}", agencyId);
                    // Somehow, when the agency's id field is missing, OBA replaces it with the agency's name.
                    // TODO Figure out how and why this is happening.
                    if (agencyId == null || agencyIdsSeen.contains(gtfsFeedId.getId() + agencyId)) {
                        // Loop in case generated name is already in use.
                        String generatedAgencyId = null;
                        while (generatedAgencyId == null || agencyIdsSeen.contains(generatedAgencyId)) {
                            generatedAgencyId = "F" + nextAgencyId;
                            nextAgencyId++;
                        }
                        LOG.warn("The agency ID '{}' was already seen, or I think it's bad. Replacing with '{}'.", agencyId, generatedAgencyId);
                        reader.addAgencyIdMapping(agencyId, generatedAgencyId); // NULL key should work
                        agency.setId(generatedAgencyId);
                        agencyId = generatedAgencyId;
                    }
                    if (agencyId != null) agencyIdsSeen.add(gtfsFeedId.getId() + agencyId);
                }
            }
        }

        for (ShapePoint shapePoint : store.getAllEntitiesForType(ShapePoint.class)) {
            shapePoint.getShapeId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Route route : store.getAllEntitiesForType(Route.class)) {
            route.getId().setAgencyId(reader.getDefaultAgencyId());
            generateRouteColor(route);
        }
        for (Stop stop : store.getAllEntitiesForType(Stop.class)) {
            stop.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Trip trip : store.getAllEntitiesForType(Trip.class)) {
            trip.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (ServiceCalendar serviceCalendar : store.getAllEntitiesForType(ServiceCalendar.class)) {
            serviceCalendar.getServiceId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (ServiceCalendarDate serviceCalendarDate : store.getAllEntitiesForType(ServiceCalendarDate.class)) {
            serviceCalendarDate.getServiceId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (FareAttribute fareAttribute : store.getAllEntitiesForType(FareAttribute.class)) {
            fareAttribute.getId().setAgencyId(reader.getDefaultAgencyId());
        }
        for (Pathway pathway : store.getAllEntitiesForType(Pathway.class)) {
            pathway.getId().setAgencyId(reader.getDefaultAgencyId());
        }

        store.close();

    }

    /**
     * Generates routeText colors for routes with routeColor and without routeTextColor
     *
     * If route doesn't have color or already has routeColor and routeTextColor nothing is done.
     *
     * textColor can be black or white. White for dark colors and black for light colors of routeColor.
     * If color is light or dark is calculated based on luminance formula:
     * sqrt( 0.299*Red^2 + 0.587*Green^2 + 0.114*Blue^2 )
     *
     * @param route
     */
    private void generateRouteColor(Route route) {
        String routeColor = route.getColor();
        //No route color - skipping
        if (routeColor == null) {
            return;
        }
        String textColor = route.getTextColor();
        //Route already has text color skipping
        if (textColor != null) {
            return;
        }

        Color routeColorColor = Color.decode("#"+routeColor);
        //gets float of RED, GREEN, BLUE in range 0...1
        float[] colorComponents = routeColorColor.getRGBColorComponents(null);
        //Calculates luminance based on https://stackoverflow.com/questions/596216/formula-to-determine-brightness-of-rgb-color
        double newRed = 0.299*Math.pow(colorComponents[0],2.0);
        double newGreen = 0.587*Math.pow(colorComponents[1],2.0);
        double newBlue = 0.114*Math.pow(colorComponents[2],2.0);
        double luminance = Math.sqrt(newRed+newGreen+newBlue);

        //For brighter colors use black text color and reverse for darker
        if (luminance > 0.5) {
            textColor = "000000";
        } else {
            textColor = "FFFFFF";
        }
        route.setTextColor(textColor);
    }

    private class StoreImpl implements GenericMutableDao {

        private GtfsMutableRelationalDao dao;

        StoreImpl(GtfsMutableRelationalDao dao) {
            this.dao = dao;
        }

        @Override
        public void open() {
            dao.open();
        }

        @Override
        public <T> T getEntityForId(Class<T> type, Serializable id) {
            return dao.getEntityForId(type, id);
        }

        @Override
        public void saveEntity(Object entity) {
            dao.saveEntity(entity);
        }

        @Override
        public void flush() {
            dao.flush();
        }

        @Override
        public void close() {
            dao.close();
        }

        @Override
        public <T> void clearAllEntitiesForType(Class<T> type) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <K extends Serializable, T extends IdentityBean<K>> void removeEntity(T entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> Collection<T> getAllEntitiesForType(Class<T> type) {
            return dao.getAllEntitiesForType(type);
        }

        @Override
        public void saveOrUpdateEntity(Object entity) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void updateEntity(Object entity) {
            throw new UnsupportedOperationException();
        }
    }

    private static class EntityCounter implements EntityHandler {

        private Map<Class<?>, Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if (count % 1000000 == 0)
                if (LOG.isDebugEnabled()) {
                    String name = bean.getClass().getName();
                    int index = name.lastIndexOf('.');
                    if (index != -1)
                        name = name.substring(index + 1);
                    LOG.debug("loading " + name + ": " + count);
                }
        }

        private int incrementCount(Class<?> entityType) {
            Integer value = _count.get(entityType);
            if (value == null)
                value = 0;
            value++;
            _count.put(entityType, value);
            return value;
        }

    }

    private static class EntityBikeability implements EntityHandler {

        private Boolean _defaultBikesAllowed;

        public EntityBikeability(Boolean defaultBikesAllowed) {
            _defaultBikesAllowed = defaultBikesAllowed;
        }

        @Override
        public void handleEntity(Object bean) {
            if (!(bean instanceof Trip)) {
                return;
            }

            Trip trip = (Trip) bean;
            if (_defaultBikesAllowed && BikeAccess.fromTrip(trip) == BikeAccess.UNKNOWN) {
                BikeAccess.setForTrip(trip, BikeAccess.ALLOWED);
            }
        }
    }

    @Override
    public void checkInputs() {
        for (GtfsBundle bundle : gtfsBundles) {
            bundle.checkInputs();
        }
    }

}
