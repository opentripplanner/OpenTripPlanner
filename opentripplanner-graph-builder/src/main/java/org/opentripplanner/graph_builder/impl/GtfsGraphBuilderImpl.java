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

package org.opentripplanner.graph_builder.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.EntityReplacementStrategy;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link GraphBuilder} plugin that supports adding transit network data from a GTFS feed to the
 * routing {@link Graph}.
 * 
 * Supports reading from multiple {@link GtfsReader} instances sequentially with respect to GTFS
 * entity classes. That is to say, given three feeds A, B, and C, all {@link Agency} entities will
 * be read from A, B, and C in turn, and then all {@link ShapePoint} entities will be read from A,
 * B, and C in turn, and so forth. This sequential reading scheme allows for cases where two
 * separate feeds may have cross-feed references (ex. StopTime => Stop) as facilitated by the use of
 * an {@link EntityReplacementStrategy}.
 * 
 * @author bdferris
 * 
 */
public class GtfsGraphBuilderImpl implements GraphBuilder {

    private static final Logger _log = LoggerFactory.getLogger(GtfsGraphBuilderImpl.class);

    private GtfsBundles _gtfsBundles;

    private GtfsMutableRelationalDao _dao = new GtfsRelationalDaoImpl();

    private EntityReplacementStrategy _entityReplacementStrategy = new EntityReplacementStrategyImpl();

    public void setGtfsBundles(GtfsBundles gtfsBundles) {
        _gtfsBundles = gtfsBundles;
        /* check for dups */
        HashSet<String> bundles = new HashSet<String>();
        for (GtfsBundle bundle : gtfsBundles.getBundles()) {
            String key = bundle.getDataKey();
            if (bundles.contains(key)) {
                throw new RuntimeException("duplicate GTFS bundle " +  key);
            }
            bundles.add(key);
        }
    }

    public void setDao(GtfsMutableRelationalDao dao) {
        _dao = dao;
    }

    public void setEntityReplacementStrategy(EntityReplacementStrategy strategy) {
        _entityReplacementStrategy = strategy;
    }

    @Override
    public void buildGraph(Graph graph) {
            try {
                readGtfs();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl();
            factory.setGtfsDao(_dao);
            CalendarServiceData data = factory.createData();

            CalendarServiceImpl service = new CalendarServiceImpl();
            service.setData(data);

            GtfsContext context = GtfsLibrary.createContext(_dao, service);

            // Load stops
            for (Stop stop : _dao.getAllStops()) {

                String id = GtfsLibrary.convertIdToString(stop.getId());
                graph.addVertex(new TransitStop(id, stop.getLon(), stop.getLat(), stop.getName(),
                        stop.getId(), stop));
            }

            GTFSPatternHopFactory hf = new GTFSPatternHopFactory(context);
            hf.run(graph);

            // We need to save the calendar service data so we can use it later
            graph.putService(CalendarServiceData.class, data);
            graph.updateTransitFeedValidity(data);
            
            // if in-memory DAO is being used, replace it with an empty one
            // to free up some space for other graphbuilders
            if (_dao instanceof GtfsRelationalDaoImpl) 
                _dao = new GtfsRelationalDaoImpl();
    }

    /****
     * Private Methods
     ****/

    private void readGtfs() throws IOException {

        StoreImpl store = new StoreImpl();
        List<GtfsReader> readers = new ArrayList<GtfsReader>();

        EntityHandler counter = new EntityCounter();

        for (GtfsBundle gtfsBundle : _gtfsBundles.getBundles()) {

            GtfsReader reader = new GtfsReader();
            reader.setInputSource(gtfsBundle.getCsvInputSource());
            reader.setEntityStore(store);
            reader.setInternStrings(true);
            
            if (gtfsBundle.getDefaultAgencyId() != null)
                reader.setDefaultAgencyId(gtfsBundle.getDefaultAgencyId());

            for (Map.Entry<String, String> entry : gtfsBundle.getAgencyIdMappings().entrySet())
                reader.addAgencyIdMapping(entry.getKey(), entry.getValue());

            if (_log.isDebugEnabled())
                reader.addEntityHandler(counter);

            if(gtfsBundle.getDefaultBikesAllowed())
                reader.addEntityHandler(new EntityBikeability(true));

            readers.add(reader);
        }

        // No feeds?
        if (readers.isEmpty()) {
            _log.info("no feeds specified");
            return;
        }

        store.open();

        List<Agency> agencies = new ArrayList<Agency>();
        List<Class<?>> entityClasses = readers.get(0).getEntityClasses();

        for (Class<?> entityClass : entityClasses) {
            _log.info("reading entities: " + entityClass.getName());

            for (GtfsReader reader : readers) {

                // Pre-load the agencies, since one agency can be mentioned across
                // multiple feeds
                if (entityClass.equals(Agency.class))
                    reader.setAgencies(agencies);

                reader.readEntities(entityClass);

                if (entityClass.equals(Agency.class))
                    agencies.addAll(reader.getAgencies());

                store.flush();
            }
        }

        store.close();
    }

    private class StoreImpl implements GenericMutableDao {

        @Override
        public void open() {
            _dao.open();
        }

        @Override
        public <T> T getEntityForId(Class<T> type, Serializable id) {
            Serializable replacement = _entityReplacementStrategy.getReplacementEntityId(type, id);
            if (replacement != null)
                id = replacement;
            return _dao.getEntityForId(type, id);
        }

        @Override
        public void saveEntity(Object entity) {

            Class<? extends Object> entityType = entity.getClass();
            if (entity instanceof IdentityBean<?>
                    && _entityReplacementStrategy.hasReplacementEntities(entityType)) {
                IdentityBean<?> bean = (IdentityBean<?>) entity;
                Serializable id = bean.getId();
                if (_entityReplacementStrategy.hasReplacementEntity(entityType, id))
                    return;
            }

            _dao.saveEntity(entity);
        }

        @Override
        public void flush() {
            _dao.flush();
        }

        @Override
        public void close() {
            _dao.close();
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
            throw new UnsupportedOperationException();
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

    private class EntityCounter implements EntityHandler {

        private Map<Class<?>, Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if (count % 10000 == 0)
                if (_log.isDebugEnabled()) {
                    String name = bean.getClass().getName();
                    int index = name.lastIndexOf('.');
                    if (index != -1)
                        name = name.substring(index + 1);
                    _log.debug("loading " + name + ": " + count);
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

    private class EntityBikeability implements EntityHandler {

        private Boolean _defaultBikesAllowed;

        public EntityBikeability(Boolean defaultBikesAllowed) {
            _defaultBikesAllowed = defaultBikesAllowed;
        }

        @Override
        public void handleEntity(Object bean) {
            if(!(bean instanceof Trip)) {
                return;
            }

            Trip trip = (Trip) bean;
            if(_defaultBikesAllowed && trip.getTripBikesAllowed() == 0 && trip.getRoute().getBikesAllowed() == 0) {
                trip.setTripBikesAllowed(2);
            }
        }
    }
}
