package org.opentripplanner.datastore;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.opentripplanner.datastore.base.DataSourceRepository;
import org.opentripplanner.datastore.base.LocalDataSourceRepository;
import org.opentripplanner.datastore.configure.DataStoreFactory;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.opentripplanner.datastore.FileType.CONFIG;
import static org.opentripplanner.datastore.FileType.DEM;
import static org.opentripplanner.datastore.FileType.GRAPH;
import static org.opentripplanner.datastore.FileType.GTFS;
import static org.opentripplanner.datastore.FileType.NETEX;
import static org.opentripplanner.datastore.FileType.OSM;
import static org.opentripplanner.datastore.FileType.REPORT;
import static org.opentripplanner.datastore.FileType.UNKNOWN;

/**
 * The responsibility of this class is to provide access to all data sources OTP uses like the
 * graph, including OSM data and transit data. The default is to use the the local disk, but other
 * "providers/repositories" can be implemented to access files in the cloud (as an example).
 * <p>
 * This class provide an abstraction layer for accessing OTP data input and output sources.In a
 * cloud ecosystem you might find it easier to access the data directly from the cloud storage,
 * rather than first copy the data into your node local disk, and then copy the build graph back
 * into cloud storage after building it. Depending on the source this might also offer enhanced
 * performance.
 * <p>
 * Use the {@link DataStoreFactory} to obtain a
 * new instance of this class.
 */
public class OtpDataStore {
    public static final String BUILD_REPORT_DIR = "report";
    private static final String STREET_GRAPH_FILENAME = "streetGraph.obj";
    private static final String GRAPH_FILENAME = "graph.obj";

    private final OtpDataStoreConfig config;
    private final List<String> repositoryDescriptions = new ArrayList<>();
    private final List<DataSourceRepository> allRepositories;
    private final LocalDataSourceRepository localRepository;
    private final Multimap<FileType, DataSource> sources = ArrayListMultimap.create();

    /* Named resources available for both reading and writing. */
    private DataSource streetGraph;
    private DataSource graph;
    private CompositeDataSource buildReportDir;

    /**
     * Use the {@link DataStoreFactory} to
     * create a new instance of this class.
     */
    public OtpDataStore(
            OtpDataStoreConfig config,
            List<DataSourceRepository> repositories
    ) {
        this.config = config;
        this.repositoryDescriptions.addAll(
                repositories.stream()
                        .map(DataSourceRepository::description)
                        .collect(Collectors.toList())
        );
        this.allRepositories = repositories;
        this.localRepository = getLocalDataSourceRepo(repositories);
    }

    public void open() {
        allRepositories.forEach(DataSourceRepository::open);
        addAll(localRepository.listExistingSources(CONFIG));
        addAll(findMultipleSources(config.osmFiles(), OSM));
        addAll(findMultipleSources(config.demFiles(),  DEM));
        addAll(findMultipleCompositeSources(config.gtfsFiles(), GTFS));
        addAll(findMultipleCompositeSources(config.netexFiles(), NETEX));

        streetGraph = findSingleSource(config.streetGraph(), STREET_GRAPH_FILENAME, GRAPH);
        graph = findSingleSource(config.graph(), GRAPH_FILENAME, GRAPH);
        buildReportDir = findCompositeSource(config.reportDirectory(), BUILD_REPORT_DIR, REPORT);

        addAll(Arrays.asList(streetGraph, graph, buildReportDir));

        // Also read in unknown sources in case the data input source is miss-spelled,
        // We look for files on the local-file-system, other repositories ignore this call.
        addAll(findMultipleSources(Collections.emptyList(), UNKNOWN));
    }

    /**
     * Static method used to get direct access to graph file without creating the
     * {@link OtpDataStore} - this is used by other application and tests that want to load the
     * graph from a directory on the local file system.
     *
     * Never use this method in the OTP application to access the graph, use the data-store.
     *
     * @param path the location where the graph file must exist.
     *
     * @return The graph file - the graph is not loaded, you can use the
     * {@link org.opentripplanner.routing.graph.SerializedGraphObject#load(File)} to load the graph.
     */
    public static File graphFile(File path) {
        return new File(path, GRAPH_FILENAME);
    }

    /**
     * @return a description(path) for each datasource used/enabled.
     */
    public List<String> getRepositoryDescriptions() {
        return repositoryDescriptions;
    }

    /**
     * List all existing data sources by file type. An empty list is returned if there is no files
     * of the given type.
     * <p>
     * This method should not be called after this data store is closed. The behavior is undefined.
     *
     * @return The collection may contain elements of type {@link DataSource} or
     * {@link CompositeDataSource}.
     */
    @NotNull
    public Collection<DataSource> listExistingSourcesFor(FileType type) {
        return sources.get(type).stream().filter(DataSource::exists).collect(Collectors.toList());
    }

    @NotNull
    public DataSource getStreetGraph() {
        return streetGraph;
    }

    @NotNull
    public DataSource getGraph() {
        return graph;
    }

    @NotNull
    public CompositeDataSource getBuildReportDir() {
        return buildReportDir;
    }


    /* private methods */

    private void add(DataSource source) {
        if(source != null) {
            sources.put(source.type(), source);
        }
    }

    private void addAll(List<? extends DataSource> list) {
        list.forEach(this::add);
    }

    private LocalDataSourceRepository getLocalDataSourceRepo(List<DataSourceRepository> repositories) {
        List<LocalDataSourceRepository> localRepos = repositories
                .stream()
                .filter(it -> it instanceof LocalDataSourceRepository)
                .map(it -> (LocalDataSourceRepository)it)
                .collect(Collectors.toList());
        if(localRepos.size() != 1) {
            throw new IllegalStateException("Only one LocalDataSourceRepository is supported.");
        }
        return localRepos.get(0);
    }

    private DataSource findSingleSource(@Nullable URI uri, @NotNull String filename, @NotNull FileType type) {
        if(uri != null) {
            return findSourceUsingAllRepos(it -> it.findSource(uri, type));
        }
        return localRepository.findSource(filename, type);
    }

    private CompositeDataSource findCompositeSource(@Nullable URI uri, @NotNull String filename, @NotNull FileType type) {
        if(uri != null) {
            return findSourceUsingAllRepos(it -> it.findCompositeSource(uri, type));
        }
        else {
            return localRepository.findCompositeSource(filename, type);
        }
    }

    private List<DataSource> findMultipleSources(@NotNull Collection<URI> uris, @NotNull FileType type) {
        if(uris == null || uris.isEmpty()) {
            return localRepository.listExistingSources(type);
        }
        List<DataSource> result = new ArrayList<>();
        for (URI uri : uris) {
            DataSource res = findSourceUsingAllRepos(it -> it.findSource(uri, type));
            result.add(res);
        }
        return result;
    }

    private List<CompositeDataSource> findMultipleCompositeSources(
            @NotNull Collection<URI> uris, @NotNull FileType type
    ) {
        if(uris.isEmpty()) {
            return localRepository.listExistingSources(type)
                    .stream()
                    .map(it -> (CompositeDataSource)it)
                    .collect(Collectors.toList());
        }
        List<CompositeDataSource> result = new ArrayList<>();
        for (URI uri : uris) {
            CompositeDataSource res = findSourceUsingAllRepos(it -> it.findCompositeSource(uri, type));
            result.add(res);
        }
        return result;
    }

    @Nullable
    private <T> T findSourceUsingAllRepos(Function<DataSourceRepository, T> repoFindSource) {
        for (DataSourceRepository it : allRepositories) {
            T res = repoFindSource.apply(it);
            if (res != null) {
                return res;
            }
        }
        return null;
    }
}
