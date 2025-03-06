package org.opentripplanner.ext.geocoder;

import static java.util.Map.entry;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.apache.lucene.codecs.lucene101.Lucene101Codec;
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
import org.apache.lucene.search.suggest.document.Completion101PostingsFormat;
import org.apache.lucene.search.suggest.document.CompletionAnalyzer;
import org.apache.lucene.search.suggest.document.ContextQuery;
import org.apache.lucene.search.suggest.document.ContextSuggestField;
import org.apache.lucene.search.suggest.document.FuzzyCompletionQuery;
import org.apache.lucene.search.suggest.document.SuggestIndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.opentripplanner.ext.stopconsolidation.StopConsolidationService;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.model.site.StopLocationsGroup;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.collection.ListUtils;

public class LuceneIndex implements Serializable {

  private static final String TYPE = "type";
  private static final String ID = "id";
  private static final String SECONDARY_IDS = "secondary_ids";
  private static final String SUGGEST = "suggest";
  private static final String NAME = "name";
  private static final String NAME_NGRAM = "name_ngram";
  private static final String CODE = "code";
  private static final String LAT = "latitude";
  private static final String LON = "longitude";

  private final TransitService transitService;
  private final Analyzer analyzer;
  private final SuggestIndexSearcher searcher;
  private final StopClusterMapper stopClusterMapper;

  /**
   * Since the {@link TransitService} is request scoped, we don't inject it into this class.
   * However, we do need some methods in the service and that's why we instantiate it manually in this
   * constructor.
   */
  public LuceneIndex(
    TimetableRepository timetableRepository,
    StopConsolidationService stopConsolidationService
  ) {
    this(new DefaultTransitService(timetableRepository), stopConsolidationService);
  }

  /**
   * This method is only visible for testing.
   */
  LuceneIndex(
    TransitService transitService,
    @Nullable StopConsolidationService stopConsolidationService
  ) {
    this.transitService = transitService;
    this.stopClusterMapper = new StopClusterMapper(transitService, stopConsolidationService);

    this.analyzer = new PerFieldAnalyzerWrapper(
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
              List.of(),
              ListUtils.ofNullable(stopLocation.getName()),
              ListUtils.ofNullable(stopLocation.getCode()),
              stopLocation.getCoordinate().latitude(),
              stopLocation.getCoordinate().longitude()
            )
          );

        transitService
          .listStopLocationGroups()
          .forEach(stopLocationsGroup ->
            addToIndex(
              directoryWriter,
              StopLocationsGroup.class,
              stopLocationsGroup.getId().toString(),
              List.of(),
              ListUtils.ofNullable(stopLocationsGroup.getName()),
              List.of(),
              stopLocationsGroup.getCoordinate().latitude(),
              stopLocationsGroup.getCoordinate().longitude()
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
              stopCluster.primaryId(),
              stopCluster.secondaryIds(),
              stopCluster.names(),
              stopCluster.codes(),
              stopCluster.coordinate().lat(),
              stopCluster.coordinate().lon()
            )
          );
      }

      DirectoryReader indexReader = DirectoryReader.open(directory);
      searcher = new SuggestIndexSearcher(indexReader);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Stream<StopLocation> queryStopLocations(String query, boolean autocomplete) {
    return matchingDocuments(StopLocation.class, query, autocomplete).map(document ->
      transitService.getStopLocation(FeedScopedId.parse(document.get(ID)))
    );
  }

  public Stream<StopLocationsGroup> queryStopLocationGroups(String query, boolean autocomplete) {
    return matchingDocuments(StopLocationsGroup.class, query, autocomplete).map(document ->
      transitService.getStopLocationsGroup(FeedScopedId.parse(document.get(ID)))
    );
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
    var primaryId = FeedScopedId.parse(document.get(ID));
    var primary = stopClusterMapper.toLocation(primaryId);

    var secondaryIds = Arrays.stream(document.getValues(SECONDARY_IDS))
      .map(FeedScopedId::parse)
      .map(stopClusterMapper::toLocation)
      .toList();

    return new StopCluster(primary, secondaryIds);
  }

  static IndexWriterConfig iwcWithSuggestField(Analyzer analyzer, final Set<String> suggestFields) {
    IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
    Codec filterCodec = new Lucene101Codec() {
      final PostingsFormat postingsFormat = new Completion101PostingsFormat();

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
    Collection<String> secondaryIds,
    Collection<I18NString> names,
    Collection<String> codes,
    double latitude,
    double longitude
  ) {
    String typeName = type.getSimpleName();

    Document document = new Document();
    document.add(new StoredField(ID, id));
    for (var secondaryId : secondaryIds) {
      document.add(new StoredField(SECONDARY_IDS, secondaryId));
    }
    document.add(new TextField(TYPE, typeName, Store.YES));
    for (var name : names) {
      document.add(new TextField(NAME, Objects.toString(name), Store.YES));
      document.add(new TextField(NAME_NGRAM, Objects.toString(name), Store.YES));
      document.add(new ContextSuggestField(SUGGEST, Objects.toString(name), 1, typeName));
    }
    document.add(new StoredField(LAT, latitude));
    document.add(new StoredField(LON, longitude));

    for (var code : codes) {
      document.add(new TextField(CODE, code, Store.YES));
      document.add(new ContextSuggestField(SUGGEST, code, 1, typeName));
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
    searchTerms = searchTerms.strip();
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

        return Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
          try {
            return searcher.storedFields().document(scoreDoc.doc);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      } else {
        var nameParser = new QueryParser(NAME_NGRAM, analyzer);
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

        return Arrays.stream(topDocs.scoreDocs).map(scoreDoc -> {
          try {
            return searcher.storedFields().document(scoreDoc.doc);
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
