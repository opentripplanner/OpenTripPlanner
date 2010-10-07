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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URL;
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
import org.onebusaway.gtfs.services.GtfsMutableRelationalDao;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
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

    private final Logger _log = LoggerFactory.getLogger(GtfsGraphBuilderImpl.class);

    private GtfsBundles _gtfsBundles;

    private GtfsMutableRelationalDao _dao = new GtfsRelationalDaoImpl();

    private EntityReplacementStrategy _entityReplacementStrategy = new EntityReplacementStrategyImpl();

    private File _cacheDirectory;

    public void setGtfsBundles(GtfsBundles gtfsBundles) {
        _gtfsBundles = gtfsBundles;
    }

    public void setDao(GtfsMutableRelationalDao dao) {
        _dao = dao;
    }

    public void setEntityReplacementStrategy(EntityReplacementStrategy strategy) {
        _entityReplacementStrategy = strategy;
    }

    public void setCacheDirectory(File cacheDirectory) {
        _cacheDirectory = cacheDirectory;
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
                        stop.getId().getId(), stop));
            }

            GTFSPatternHopFactory hf = new GTFSPatternHopFactory(context);
            hf.run(graph);

            // We need to save the calendar service data so we can use it later
            graph.putService(CalendarServiceData.class, data);

    }

    /****
     * Private Methods
     ****/

    private void readGtfs() throws IOException {

        StoreImpl store = new StoreImpl();
        List<GtfsReader> readers = new ArrayList<GtfsReader>();

        EntityHandler counter = new EntityCounter();

        for (GtfsBundle gtfsBundle : _gtfsBundles.getBundles()) {

            File path = getPathForGtfsBundle(gtfsBundle);
            if (!path.isFile()) {
                throw new IOException(path + " is not a normal file");
            }
            _log.info("gtfs=" + path);

            GtfsReader reader = new GtfsReader();
            reader.setInputLocation(path);
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

    private File getPathForGtfsBundle(GtfsBundle gtfsBundle) throws IOException {

        File path = gtfsBundle.getPath();
        if (path != null)

            return path;

        URL url = gtfsBundle.getUrl();

        if (url != null) {

            File tmpDir = getTemporaryDirectory();
            String fileName = gtfsBundle.getDefaultAgencyId() + "_gtfs.zip";
            File gtfsFile = new File(tmpDir, fileName);

            if (gtfsFile.exists()) {
                _log.info("using already downloaded gtfs file: path=" + gtfsFile);
                return gtfsFile;
            }

            _log.info("downloading gtfs: url=" + url + " path=" + gtfsFile);

            BufferedInputStream in = new BufferedInputStream(url.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(gtfsFile));

            copyStreams(in, out);

            return gtfsFile;
        }

        throw new IllegalStateException("GtfsBundle did not include a path or a url");
    }

    private void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int rc = in.read(buffer);
            if (rc == -1)
                break;
            out.write(buffer, 0, rc);
        }
        in.close();
        out.close();
    }

    private File getTemporaryDirectory() {

        if (_cacheDirectory != null) {
            if (!_cacheDirectory.exists())
                _cacheDirectory.mkdirs();
            return _cacheDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir"));
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
    }

    private class EntityCounter implements EntityHandler {

        private Map<Class<?>, Integer> _count = new HashMap<Class<?>, Integer>();

        @Override
        public void handleEntity(Object bean) {
            int count = incrementCount(bean.getClass());
            if (count % 1000 == 0)
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
}
