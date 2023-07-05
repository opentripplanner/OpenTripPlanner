package org.opentripplanner.ext.geocoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene95.Lucene95Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.suggest.document.Completion90PostingsFormat;
import org.apache.lucene.search.suggest.document.CompletionAnalyzer;
import org.apache.lucene.search.suggest.document.ContextQuery;
import org.apache.lucene.search.suggest.document.ContextSuggestField;
import org.apache.lucene.search.suggest.document.FuzzyCompletionQuery;
import org.apache.lucene.search.suggest.document.SuggestIndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.opentripplanner.ext.geocoder.StopCluster.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.street.model.vertex.StreetVertex;
import org.opentripplanner.street.model.vertex.VertexLabel;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.service.TransitService;

public class LuceneIndex implements Serializable {

  private static final String TYPE = "type";
  private static final String ID = "id";
  private static final String SUGGEST = "suggest";
  private static final String NAME = "name";
  private static final String CODE = "code";
  private static final String LAT = "latitude";
  private static final String LON = "longitude";
  private static final String MODE = "mode";

  private final Graph graph;

  private final TransitService transitService;
  private final Analyzer analyzer;
  private final SuggestIndexSearcher searcher;

  public LuceneIndex(Graph graph, TransitService transitService) {
    this.graph = graph;
    this.transitService = transitService;
    this.analyzer =
      new PerFieldAnalyzerWrapper(
        new StandardAnalyzer(),
        Map.of(NAME, new SimpleAnalyzer(), SUGGEST, new CompletionAnalyzer(new StandardAnalyzer()))
      );

    var directory = new ByteBuffersDirectory();

    var stopClusterMapper = new StopClusterMapper(transitService);
    try {
      try (
        var directoryWriter = new IndexWriter(
          directory,
          iwcWithSuggestField(analyzer, Set.of(SUGGEST))
        )
      ) {
        transitService
          .listStopLocations()
          .forEach(stopLocation ->
            addToIndex(
              directoryWriter,
              StopLocation.class,
              stopLocation.getId().toString(),
              stopLocation.getName(),
              stopLocation.getCode(),
              stopLocation.getCoordinate().latitude(),
              stopLocation.getCoordinate().longitude(),
              Set.of()
            )
          );

        transitService
          .listStopLocationGroups()
          .forEach(stopLocationsGroup ->
            addToIndex(
              directoryWriter,
              StopLocationsGroup.class,
              stopLocationsGroup.getId().toString(),
              stopLocationsGroup.getName(),
              null,
              stopLocationsGroup.getCoordinate().latitude(),
              stopLocationsGroup.getCoordinate().longitude(),
              Set.of()
            )
          );

        stopClusterMapper
          .generateStopClusters(
            transitService.listStopLocations(),
            transitService.listStopLocationGroups()
          )
          .forEach(stopCluster ->
            addToIndex(
              directoryWriter,
              StopCluster.class,
              stopCluster.id().toString(),
              new NonLocalizedString(stopCluster.name()),
              stopCluster.code(),
              stopCluster.coordinate().lat(),
              stopCluster.coordinate().lon(),
              stopCluster.modes()
            )
          );

        graph
          .getVertices()
          .stream()
          .filter(v -> v instanceof StreetVertex)
          .map(v -> (StreetVertex) v)
          .forEach(streetVertex ->
            addToIndex(
              directoryWriter,
              StreetVertex.class,
              streetVertex.getLabel().toString(),
              streetVertex.getIntersectionName(),
              streetVertex.getLabel().toString(),
              streetVertex.getLat(),
              streetVertex.getLon(),
              Set.of()
            )
          );
      }

      DirectoryReader indexReader = DirectoryReader.open(directory);
      searcher = new SuggestIndexSearcher(indexReader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static synchronized LuceneIndex forServer(OtpServerRequestContext serverContext) {
    var graph = serverContext.graph();
    var existingIndex = graph.getLuceneIndex();
    if (existingIndex != null) {
      return existingIndex;
    }

    var newIndex = new LuceneIndex(graph, serverContext.transitService());
    graph.setLuceneIndex(newIndex);
    return newIndex;
  }

  public Stream<StopLocation> queryStopLocations(String query, boolean autocomplete) {
    return matchingDocuments(StopLocation.class, query, autocomplete)
      .map(document -> transitService.getStopLocation(FeedScopedId.parseId(document.get(ID))));
  }

  public Stream<StopLocationsGroup> queryStopLocationGroups(String query, boolean autocomplete) {
    return matchingDocuments(StopLocationsGroup.class, query, autocomplete)
      .map(document -> transitService.getStopLocationsGroup(FeedScopedId.parseId(document.get(ID)))
      );
  }

  public Stream<StreetVertex> queryStreetVertices(String query, boolean autocomplete) {
    return matchingDocuments(StreetVertex.class, query, autocomplete)
      .map(document -> (StreetVertex) graph.getVertex(VertexLabel.string(document.get(ID))));
  }

  /**
   * Return all "stop clusters" for a given query.
   * <p>
   * Stop clusters are defined as follows.
   * <p>
   *  - If a stop has a parent station, only the parent is returned.
   *  - If two stops have the same name *and* are less than 10 meters from each other, only
   *    one of those is chosen at random and returned.
   */
  public Stream<StopCluster> queryStopClusters(String query) {
    return matchingDocuments(StopCluster.class, query, true).map(LuceneIndex::toStopCluster);
  }

  private static StopCluster toStopCluster(Document document) {
    var id = FeedScopedId.parseId(document.get(ID));
    var name = document.get(NAME);
    var code = document.get(CODE);
    var lat = document.getField(LAT).numericValue().doubleValue();
    var lon = document.getField(LON).numericValue().doubleValue();
    var modes = Arrays.asList(document.getValues(MODE));
    return new StopCluster(id, code, name, new Coordinate(lat, lon), modes);
  }

  static IndexWriterConfig iwcWithSuggestField(Analyzer analyzer, final Set<String> suggestFields) {
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    Codec filterCodec = new Lucene95Codec() {
      final PostingsFormat postingsFormat = new Completion90PostingsFormat();

      @Override
      public PostingsFormat getPostingsFormatForField(String field) {
        if (suggestFields.contains(field)) {
          return postingsFormat;
        }
        return super.getPostingsFormatForField(field);
      }
    };
    iwc.setCodec(filterCodec);
    return iwc;
  }

  private static void addToIndex(
    IndexWriter writer,
    Class<?> type,
    String id,
    I18NString name,
    @Nullable String code,
    double latitude,
    double longitude,
    Collection<String> modes
  ) {
    String typeName = type.getSimpleName();

    Document document = new Document();
    document.add(new StoredField(ID, id));
    document.add(new TextField(TYPE, typeName, Store.YES));
    document.add(new TextField(NAME, Objects.toString(name), Store.YES));
    document.add(new ContextSuggestField(SUGGEST, Objects.toString(name), 1, typeName));
    document.add(new StoredField(LAT, latitude));
    document.add(new StoredField(LON, longitude));

    if (code != null) {
      document.add(new TextField(CODE, code, Store.YES));
      document.add(new ContextSuggestField(SUGGEST, code, 1, typeName));
    }

    for (var mode : modes) {
      document.add(new TextField(MODE, mode, Store.YES));
    }

    try {
      writer.addDocument(document);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  private Stream<Document> matchingDocuments(
    Class<?> type,
    String searchTerms,
    boolean autocomplete
  ) {
    try {
      if (autocomplete) {
        var completionQuery = new FuzzyCompletionQuery(
          analyzer,
          new Term(SUGGEST, analyzer.normalize(SUGGEST, searchTerms)),
          null,
          2,
          true,
          4,
          3,
          true,
          3
        );
        var query = new ContextQuery(completionQuery);
        query.addContext(type.getSimpleName());

        var topDocs = searcher.suggest(query, 25, true);

        return Arrays
          .stream(topDocs.scoreDocs)
          .map(scoreDoc -> {
            try {
              return searcher.doc(scoreDoc.doc);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
      } else {
        var parser = new QueryParser(CODE, analyzer);
        var nameQuery = parser.createPhraseQuery(NAME, searchTerms);
        var codeQuery = new TermQuery(new Term(CODE, analyzer.normalize(CODE, searchTerms)));
        var typeQuery = new TermQuery(
          new Term(TYPE, analyzer.normalize(TYPE, type.getSimpleName()))
        );

        var builder = new BooleanQuery.Builder()
          .setMinimumNumberShouldMatch(1)
          .add(typeQuery, Occur.MUST)
          .add(codeQuery, Occur.SHOULD);

        if (nameQuery != null) {
          builder.add(nameQuery, Occur.SHOULD);
        }

        var query = builder.build();

        var topDocs = searcher.search(query, 25);

        return Arrays
          .stream(topDocs.scoreDocs)
          .map(scoreDoc -> {
            try {
              return searcher.doc(scoreDoc.doc);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          });
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
