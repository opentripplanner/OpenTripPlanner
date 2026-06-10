package org.opentripplanner.graph_builder.module.cache;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import java.io.Closeable;
import java.io.Serializable;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.routing.graph.kryosupport.KryoBuilder;
import org.opentripplanner.utils.time.DurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages persistent caches for expensive graph-build computations, backed by a
 * {@link CompositeDataSource} so it works with both local filesystems and cloud storage (e.g. GCP).
 * <p>
 * Each {@link CacheTask} has its own entry named
 * {@code <task>-cache-<serializationVersionId>.obj} inside the composite data source.
 * The per-task version ID is embedded in the entry name so that stale entries from a previous
 * format version are ignored automatically without needing to read their contents.
 * <p>
 * Only the cache for the task currently being executed is held in memory, reducing peak memory
 * usage during large graph builds.
 * <p>
 * Cache writes are performed asynchronously on a dedicated background thread so that the main
 * graph-build thread can continue immediately after a save is requested. Call {@link #close()}
 * at the end of the build to wait for any in-flight write to finish before exiting.
 */
public class GraphBuildCacheManager implements Closeable {

  /**
   * Maximum time to wait for a background cache write to finish before giving up.
   * The write should normally complete well within this window; the limit exists purely
   * as a safety valve so a slow or hung write never stalls the build process indefinitely.
   */
  static final Duration WRITE_TIMEOUT = Duration.ofMinutes(30);

  /** Disabled no-op instance — safe to use as a default when no real cache is needed. */
  public static final GraphBuildCacheManager NOOP = new GraphBuildCacheManager(
    GraphBuildCacheParameters.DISABLED,
    List.of()
  );

  private static final Logger LOG = LoggerFactory.getLogger(GraphBuildCacheManager.class);

  private final GraphBuildCacheParameters parameters;
  private final Map<CacheTask, DataSource> cacheFiles;

  /** Used for load operations only (main thread). Save operations create their own Kryo instance. */
  private final Kryo kryo;

  private final ExecutorService writeExecutor;

  public GraphBuildCacheManager(
    GraphBuildCacheParameters parameters,
    Iterable<DataSource> cacheFiles
  ) {
    this.parameters = parameters;
    this.cacheFiles = mapFilesToTask(cacheFiles);
    this.kryo = KryoBuilder.create();
    this.writeExecutor = Executors.newSingleThreadExecutor(r -> {
      var t = new Thread(r, "cache-writer");
      t.setDaemon(true);
      return t;
    });
  }

  public boolean isEnabled(CacheTask task) {
    return parameters.isEnabled(task);
  }

  /**
   * Load the cache for the given task. Returns {@code null} on a miss (entry absent, unreadable,
   * or version mismatch).
   * <p>
   * Return {@code null} if the cache is disabled by configuration.
   */
  @Nullable
  @SuppressWarnings("unchecked")
  public <T> T load(CacheTask task) {
    if (!isEnabled(task)) {
      return null;
    }
    DataSource entry = cacheFiles.get(task);
    if (entry == null || !entry.exists()) {
      LOG.info(
        "No {} cache file found in {}.",
        task.cacheFileName(),
        entry == null ? "configured cache directory" : entry.directory()
      );
      return null;
    }
    LOG.info("Loading {} cache from '{}'.", task, entry.path());
    try (Input input = new Input(entry.asInputStream())) {
      var wrapper = (CacheSerializationObject<T>) kryo.readClassAndObject(input);
      if (wrapper.serializationVersionId != task.serializationVersionId) {
        LOG.warn(
          "Ignoring {} cache at '{}': version mismatch (file={}, code={}).",
          task,
          entry.path(),
          wrapper.serializationVersionId,
          task.serializationVersionId
        );
        return null;
      }
      return wrapper.data;
    } catch (Exception e) {
      LOG.warn(
        "Failed to load {} cache from '{}': {}. Starting with empty cache.",
        task,
        entry.path(),
        e.getMessage()
      );
      return null;
    }
  }

  /**
   * <p>
   * Return {@code null} if the cache is disabled by configuration.
   */
  @Nullable
  public <K extends Serializable, V extends Serializable> KeyValueCache<K, V> loadKVCache(
    CacheTask task
  ) {
    return isEnabled(task) ? new KeyValueCache<>(task, load(task)) : null;
  }

  /**
   * Schedule an asynchronous write of {@code data} to the cache file for {@code task}.
   * <p>
   * The write is performed on a dedicated background thread so the calling (graph-build) thread
   * can continue immediately. A fresh {@link Kryo} instance is created for each write to avoid
   * thread-safety issues with the instance used by {@link #load}.
   * <p>
   * Call {@link #close()} at the end of the build to wait for the write to finish.
   */
  public <T> void save(CacheTask task, T data) {
    DataSource entry = cacheFiles.get(task);
    LOG.info("Saving {} cache to '{}' (async).", task, entry.path());
    writeExecutor.submit(() -> {
      try (Output output = new Output(entry.asOutputStream())) {
        KryoBuilder.create().writeClassAndObject(
          output,
          new CacheSerializationObject<>(task.serializationVersionId, data)
        );
        LOG.info("Saved {} cache to '{}'.", task, entry.path());
      } catch (KryoException e) {
        LOG.warn("Failed to save {} cache to '{}': {}.", task, entry.path(), e.getMessage());
      }
    });
  }

  public <K extends Serializable, V extends Serializable> void saveKVCache(
    KeyValueCache<K, V> cache
  ) {
    save(cache.task(), cache.newEntries());
  }

  /**
   * Signals that no more saves will be submitted, then waits up to {@link #WRITE_TIMEOUT} for
   * any in-flight write to finish. Should be called once at the end of the graph build, before
   * closing the underlying data sources.
   */
  @Override
  public void close() {
    writeExecutor.shutdown();
    try {
      if (!writeExecutor.awaitTermination(WRITE_TIMEOUT.toSeconds(), TimeUnit.SECONDS)) {
        LOG.warn(
          "Cache write did not finish within {}; aborting.",
          DurationUtils.durationToStr(WRITE_TIMEOUT)
        );
        writeExecutor.shutdownNow();
      }
    } catch (InterruptedException e) {
      LOG.warn("Interrupted while waiting for cache write to finish; aborting.");
      writeExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
  }

  private Map<CacheTask, DataSource> mapFilesToTask(Iterable<DataSource> cacheFiles) {
    var result = new HashMap<CacheTask, DataSource>();
    for (DataSource dataSource : cacheFiles) {
      result.put(CacheTask.matchName(dataSource.name()), dataSource);
    }
    return result;
  }
}
