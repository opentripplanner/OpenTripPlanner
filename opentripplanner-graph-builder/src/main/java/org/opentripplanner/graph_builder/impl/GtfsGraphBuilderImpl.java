/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

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
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.csv.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.IdentityBean;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GenericMutableDao;
import org.onebusaway.gtfs.services.GtfsMutableDao;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.gtfs.services.calendar.CalendarServiceData;
import org.opentripplanner.graph_builder.model.GtfsBundle;
import org.opentripplanner.graph_builder.model.GtfsBundles;
import org.opentripplanner.graph_builder.services.EntityReplacementStrategy;
import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.vertextypes.TransitStop;
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

    private final Logger _log = LoggerFactory.getLogger(GtfsGraphBuilderImpl.class);

    private GtfsBundles _gtfsBundles;

    private GtfsRelationalDaoImpl _defaultDao = new GtfsRelationalDaoImpl();

    private GtfsRelationalDao _dao = _defaultDao;

    private GtfsMutableDao _store = _defaultDao;

    private EntityReplacementStrategy _entityReplacementStrategy = new EntityReplacementStrategyImpl();

    public void setGtfsBundles(GtfsBundles gtfsBundles) {
        _gtfsBundles = gtfsBundles;
    }

    public void setGtfsRelationalDao(GtfsRelationalDao dao) {
        _dao = dao;
    }

    public void setGtfsEntityStore(GtfsMutableDao store) {
        _store = store;
    }

    public void setEntityReplacementStrategy(EntityReplacementStrategy strategy) {
        _entityReplacementStrategy = strategy;
    }

    @Override
    public void buildGraph(Graph graph) {

        try {
            readGtfs();

            CalendarServiceDataFactoryImpl factory = new CalendarServiceDataFactoryImpl();
            factory.setGtfsDao(_dao);
            CalendarServiceData data = factory.createServiceCalendarData();

            CalendarServiceImpl service = new CalendarServiceImpl();
            service.setServiceCalendarData(data);

            GtfsContext context = GtfsLibrary.createContext(_dao, service);

            // Load stops
            for (Stop stop : _dao.getAllStops()) {

                String id = GtfsLibrary.convertIdToString(stop.getId());
                graph.addVertex(new GenericVertex(id, stop.getLon(), stop.getLat(), stop.getName(),
                        TransitStop.class));
            }

            GTFSPatternHopFactory hf = new GTFSPatternHopFactory(context);
            hf.run(graph);

            // We need to save the calendar service data so we can use it later
            graph.putService(CalendarServiceData.class, data);

        } catch (Exception ex) {
            throw new IllegalStateException("error building graph from gtfs", ex);
        }
    }

    /****
     * Private Methods
     ****/

    private void readGtfs() throws IOException {

        StoreImpl store = new StoreImpl();
        List<GtfsReader> readers = new ArrayList<GtfsReader>();

        EntityHandler counter = new EntityCounter();

        for (GtfsBundle gtfsBundle : _gtfsBundles.getBundles()) {

            _log.info("gtfs=" + gtfsBundle.getPath());

            GtfsReader reader = new GtfsReader();
            reader.setInputLocation(gtfsBundle.getPath());
            reader.setEntityStore(store);

            if (gtfsBundle.getDefaultAgencyId() != null)
                reader.setDefaultAgencyId(gtfsBundle.getDefaultAgencyId());

            for (Map.Entry<String, String> entry : gtfsBundle.getAgencyIdMappings().entrySet())
                reader.addAgencyIdMapping(entry.getKey(), entry.getValue());

            if (_log.isDebugEnabled())
                reader.addEntityHandler(counter);

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
            _store.open();
        }

        @Override
        public <T> T getEntityForId(Class<T> type, Serializable id) {
            Serializable replacement = _entityReplacementStrategy.getReplacementEntityId(type, id);
            if (replacement != null)
                id = replacement;
            return _store.getEntityForId(type, id);
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

            _store.saveEntity(entity);
        }

        @Override
        public void flush() {
            _store.flush();
        }

        @Override
        public void close() {
            _store.close();
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
    }

    private class EntityCounter implements EntityHandler {

        private Map<Class<?>, Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if (count % 1000 == 0)
                _log.debug("loaded: " + count);
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
}
