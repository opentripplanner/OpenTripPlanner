package org.opentripplanner.graph_builder.impl;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
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
import org.onebusaway.gtfs.serialization.GtfsEntityStore;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.serialization.GtfsReaderContext;
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
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.factory.GTFSPatternHopFactory;
import org.opentripplanner.routing.vertextypes.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
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

    private GtfsEntityStore _store = _defaultDao;

    private EntityReplacementStrategy _entityReplacementStrategy = new EntityReplacementStrategyImpl();

    public void setGtfsBundles(GtfsBundles gtfsBundles) {
        _gtfsBundles = gtfsBundles;
    }

    public void setGtfsRelationalDao(GtfsRelationalDao dao) {
        _dao = dao;
    }

    public void setGtfsEntityStore(GtfsEntityStore store) {
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
                Vertex vertex = graph
                        .addVertex(new GenericVertex(id, stop.getLon(), stop.getLat()));
                vertex.setType(TransitStop.class);
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
            
            if( _log.isDebugEnabled() )
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

    private class StoreImpl implements GtfsEntityStore {

        @Override
        public void open() {
            _store.open();
        }

        @Override
        public Object load(Class<?> entityClass, Serializable id) {
            Serializable replacement = _entityReplacementStrategy.getReplacementEntityId(
                    entityClass, id);
            if (replacement != null)
                id = replacement;
            return _store.load(entityClass, id);
        }

        @Override
        public void save(GtfsReaderContext context, Object entity) {

            Class<? extends Object> entityType = entity.getClass();
            if (entity instanceof IdentityBean<?>
                    && _entityReplacementStrategy.hasReplacementEntities(entityType)) {
                IdentityBean<?> bean = (IdentityBean<?>) entity;
                Serializable id = bean.getId();
                if (_entityReplacementStrategy.hasReplacementEntity(entityType, id))
                    return;
            }

            _store.save(context, entity);
        }

        @Override
        public void flush() {
            _store.flush();
        }

        @Override
        public void close() {
            _store.close();
        }
    }
    
    private class EntityCounter implements EntityHandler {
        
        private Map<Class<?>,Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if( count % 1000 == 0)
                _log.debug("loaded: " + count);
        }
        
        private int incrementCount(Class<?> entityType) {
            Integer value = _count.get(entityType);
            if( value == null)
                value = 0;
            value++;
            _count.put(entityType,value);
            return value;
        }
        
    }
}
