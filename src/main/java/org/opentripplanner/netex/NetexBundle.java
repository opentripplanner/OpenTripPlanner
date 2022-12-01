package org.opentripplanner.netex;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBException;
import org.opentripplanner.datastore.api.CompositeDataSource;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.netex.config.NetexFeedParameters;
import org.opentripplanner.netex.index.NetexEntityIndex;
import org.opentripplanner.netex.loader.GroupEntries;
import org.opentripplanner.netex.loader.NetexDataSourceHierarchy;
import org.opentripplanner.netex.loader.NetexXmlParser;
import org.opentripplanner.netex.loader.parser.NetexDocumentParser;
import org.opentripplanner.netex.mapping.NetexMapper;
import org.opentripplanner.netex.validation.Validator;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads/reads a NeTEx bundle of a data source(zip file/directory/cloud storage) and maps it into
 * the OTP internal transit model.
 * <p>
 * The NeTEx loader will use a file naming convention to load files in a particular order and
 * keeping an index of entities to enable linking. The convention is documented here {@link
 * NetexFeedParameters#sharedFilePattern()} and here {@link NetexDataSourceHierarchy}.
 * <p>
 * This class is also responsible for logging progress and exception handling.
 */
public class NetexBundle implements Closeable {

  private static final Logger LOG = LoggerFactory.getLogger(NetexBundle.class);

  private final CompositeDataSource source;

  private final NetexDataSourceHierarchy hierarchy;

  private final String feedId;
  private final Set<String> ferryIdsNotAllowedForBicycle;
  private final double maxStopToShapeSnapDistance;
  private final boolean noTransfersOnIsolatedStops;
  private final boolean ignoreFareFrame;
  /** The NeTEx entities loaded from the input files and passed on to the mapper. */
  private NetexEntityIndex index = new NetexEntityIndex();
  /** Report errors to issue store */
  private DataImportIssueStore issueStore;
  /** maps the NeTEx XML document to OTP transit model. */
  private NetexMapper mapper;
  private NetexXmlParser xmlParser;

  public NetexBundle(
    String feedId,
    CompositeDataSource source,
    NetexDataSourceHierarchy hierarchy,
    Set<String> ferryIdsNotAllowedForBicycle,
    double maxStopToShapeSnapDistance,
    boolean noTransfersOnIsolatedStops,
    boolean ignoreFareFrame
  ) {
    this.feedId = feedId;
    this.source = source;
    this.hierarchy = hierarchy;
    this.ferryIdsNotAllowedForBicycle = ferryIdsNotAllowedForBicycle;
    this.maxStopToShapeSnapDistance = maxStopToShapeSnapDistance;
    this.noTransfersOnIsolatedStops = noTransfersOnIsolatedStops;
    this.ignoreFareFrame = ignoreFareFrame;
  }

  /** load the bundle, map it to the OTP transit model and return */
  public OtpTransitServiceBuilder loadBundle(
    Deduplicator deduplicator,
    DataImportIssueStore issueStore
  ) {
    LOG.info("Reading {}", hierarchy.description());

    this.issueStore = issueStore;

    // Store result in a mutable OTP Transit Model
    OtpTransitServiceBuilder transitBuilder = new OtpTransitServiceBuilder(issueStore);

    // init parser and mapper
    xmlParser = new NetexXmlParser();
    mapper =
      new NetexMapper(
        transitBuilder,
        feedId,
        deduplicator,
        issueStore,
        ferryIdsNotAllowedForBicycle,
        maxStopToShapeSnapDistance,
        noTransfersOnIsolatedStops
      );

    // Load data
    loadFileEntries();

    return transitBuilder;
  }

  public void checkInputs() {
    if (!source.exists()) {
      throw new RuntimeException("NeTEx " + source.path() + " does not exist.");
    }
  }

  /* private methods */

  @Override
  public void close() throws IOException {
    source.close();
  }

  /** Load all files entries in the bundle */
  private void loadFileEntries() {
    // Load global shared files
    loadFilesThenMapToOtpTransitModel("shared file", hierarchy.sharedEntries());

    for (GroupEntries group : hierarchy.groups()) {
      LOG.info("reading group {}", group.name());

      scopeInputData(() -> {
        // Load shared group files
        loadFilesThenMapToOtpTransitModel("shared group file", group.sharedEntries());

        for (DataSource entry : group.independentEntries()) {
          scopeInputData(() -> {
            // Load each independent file in group
            loadFilesThenMapToOtpTransitModel("group file", List.of(entry));
          });
        }
      });
    }
    mapper.finishUp();
    NetexDocumentParser.finnishUp();
  }

  /**
   * make a new index and pushes it on the index stack, before executing the task and at the end pop
   * of the index.
   */
  private void scopeInputData(Runnable task) {
    index = index.push();
    mapper = mapper.push();
    task.run();
    mapper = mapper.pop();
    index = index.pop();
  }

  /**
   * Load a set of files and map the entries to OTP Transit model after the loading is complete. It
   * is important to do this in 2 steps to be able to link references. An attempt to map each entry,
   * when read, would lead to missing references, since the order entries are read is not enforced
   * in any way.
   */
  private void loadFilesThenMapToOtpTransitModel(
    String fileDescription,
    Iterable<DataSource> entries
  ) {
    for (DataSource entry : entries) {
      // Load entry and store it in the index
      loadSingeFileEntry(fileDescription, entry);
    }

    // Validate input data, and remove invalid data
    Validator.validate(index, issueStore);

    // map current NeTEx objects into the OTP Transit Model
    mapper.mapNetexToOtp(index.readOnlyView());
  }

  /** Load a single entry and store it in the index for later */
  private void loadSingeFileEntry(String fileDescription, DataSource entry) {
    try {
      LOG.info("reading entity {}: {}", fileDescription, entry.name());

      PublicationDeliveryStructure doc = xmlParser.parseXmlDoc(entry.asInputStream());
      NetexDocumentParser.parseAndPopulateIndex(index, doc, ignoreFareFrame);
    } catch (JAXBException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }
}
