package org.opentripplanner.ext.geocoder;

import static java.util.Map.entry;

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
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.codecs.PostingsFormat;
import org.apache.lucene.codecs.lucene99.Lucene99Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.FuzzyQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.suggest.document.Completion99PostingsFormat;
import org.apache.lucene.search.suggest.document.CompletionAnalyzer;
import org.apache.lucene.search.suggest.document.ContextQuery;
import org.apache.lucene.search.suggest.document.ContextSuggestField;
import org.apache.lucene.search.suggest.document.FuzzyCompletionQuery;
import org.apache.lucene.search.suggest.document.SuggestIndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.opentripplanner.ext.geocoder.StopCluster.Coordinate;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.service.TransitService;

public class LuceneIndex implements Serializable {

  private static final String TYPE = "type";
  private static final String ID = "id";
  private static final String SUGGEST = "suggest";
  private static final String NAME = "name";
  private static final String NAME_NGRAM = "name_ngram";
  private static final String CODE = "code";
  private static final String LAT = "latitude";
  private static final String LON = "longitude";
  private static final String MODE = "mode";
  private static final String AGENCY_IDS = "agency_ids";

  private final TransitService transitService;
  private final Analyzer analyzer;
  private final SuggestIndexSearcher searcher;

  public LuceneIndex(TransitService transitService) {
    this.transitService = transitService;
    StopClusterMapper stopClusterMapper = new StopClusterMapper(transitService);

    this.analyzer =
      new PerFieldAnalyzerWrapper(
        new StandardAnalyzer(),
        Map.ofEntries(
          entry(NAME, new EnglishAnalyzer()),
          entry(NAME_NGRAM, new EnglishNGramAnalyzer()),
          entry(SUGGEST, new CompletionAnalyzer(new StandardAnalyzer()))
        )
      );

    var directory = new ByteBuffersDirectory();

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
              Set.of(),
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
              Set.of(),
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
              I18NString.of(stopCluster.name()),
              stopCluster.code(),
              stopCluster.coordinate().lat(),
              stopCluster.coordinate().lon(),
              stopCluster.modes(),
              stopCluster.agencyIds()
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

    var newIndex = new LuceneIndex(serverContext.transitService());
    graph.setLuceneIndex(newIndex);
    return newIndex;
  }

  public Stream<StopLocation> queryStopLocations(String query, boolean autocomplete) {
    return matchingDocuments(StopLocation.class, query, autocomplete)
      .map(document -> transitService.getStopLocation(FeedScopedId.parse(document.get(ID))));
  }

  public Stream<StopLocationsGroup> queryStopLocationGroups(String query, boolean autocomplete) {
    return matchingDocuments(StopLocationsGroup.class, query, autocomplete)
      .map(document -> transitService.getStopLocationsGroup(FeedScopedId.parse(document.get(ID))));
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
    return matchingDocuments(StopCluster.class, query, false).map(this::toStopCluster);
  }

  private StopCluster toStopCluster(Document document) {
    var clusterId = FeedScopedId.parse(document.get(ID));
    var name = document.get(NAME);
    var code = document.get(CODE);
    var lat = document.getField(LAT).numericValue().doubleValue();
    var lon = document.getField(LON).numericValue().doubleValue();
    var modes = Arrays.asList(document.getValues(MODE));
    var agencies = Arrays
      .stream(document.getValues(AGENCY_IDS))
      .map(id -> transitService.getAgencyForId(FeedScopedId.parse(id)))
      .filter(Objects::nonNull)
      .map(StopClusterMapper::toAgency)
      .toList();
    var feedPublisher = StopClusterMapper.toFeedPublisher(
      transitService.getFeedInfo(clusterId.getFeedId())
    );
    return new StopCluster(
      clusterId,
      code,
      name,
      new Coordinate(lat, lon),
      modes,
      agencies,
      feedPublisher
    );
  }

  static IndexWriterConfig iwcWithSuggestField(Analyzer analyzer, final Set<String> suggestFields) {
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    Codec filterCodec = new Lucene99Codec() {
      final PostingsFormat postingsFormat = new Completion99PostingsFormat();

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
    Collection<String> modes,
    Collection<String> agencyIds
  ) {
    String typeName = type.getSimpleName();

    Document document = new Document();
    document.add(new StoredField(ID, id));
    document.add(new TextField(TYPE, typeName, Store.YES));
    document.add(new TextField(NAME, Objects.toString(name), Store.YES));
    document.add(new TextField(NAME_NGRAM, Objects.toString(name), Store.YES));
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
    for (var ids : agencyIds) {
      document.add(new TextField(AGENCY_IDS, ids, Store.YES));
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
        var nameParser = new QueryParser(NAME, analyzer);
        var nameQuery = nameParser.parse(searchTerms);

        var ngramNameQuery = new TermQuery(
          new Term(NAME_NGRAM, analyzer.normalize(NAME_NGRAM, searchTerms))
        );

        var fuzzyNameQuery = new FuzzyQuery(new Term(NAME, analyzer.normalize(NAME, searchTerms)));
        var prefixNameQuery = new PrefixQuery(
          new Term(NAME, analyzer.normalize(NAME, searchTerms))
        );
        var codeQuery = new TermQuery(new Term(CODE, analyzer.normalize(CODE, searchTerms)));

        var prefixCodeQuery = new PrefixQuery(
          new Term(CODE, analyzer.normalize(CODE, searchTerms))
        );

        var typeQuery = new TermQuery(
          new Term(TYPE, analyzer.normalize(TYPE, type.getSimpleName()))
        );

        var builder = new BooleanQuery.Builder()
          .setMinimumNumberShouldMatch(1)
          .add(typeQuery, Occur.MUST)
          .add(codeQuery, Occur.SHOULD)
          .add(prefixCodeQuery, Occur.SHOULD)
          .add(nameQuery, Occur.SHOULD)
          .add(fuzzyNameQuery, Occur.SHOULD)
          .add(prefixNameQuery, Occur.SHOULD)
          .add(ngramNameQuery, Occur.SHOULD);

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
    } catch (IOException | ParseException ex) {
      throw new RuntimeException(ex);
    }
  }
}
