package org.opentripplanner.ext.geocoder;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
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
import org.apache.lucene.codecs.lucene91.Lucene91Codec;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.LatLonPoint;
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
import org.apache.lucene.search.suggest.document.PrefixCompletionQuery;
import org.apache.lucene.search.suggest.document.SuggestIndexSearcher;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopCollection;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.I18NString;

public class LuceneIndex implements Serializable {

    private static final String TYPE = "type";
    private static final String ID = "id";
    private static final String SUGGEST = "suggest";
    private static final String NAME = "name";
    private static final String CODE = "code";
    private static final String COORDINATE = "coordinate";

    private final Graph graph;
    private final Analyzer analyzer;
    private final SuggestIndexSearcher searcher;

    public LuceneIndex(Graph graph) {
        this.graph = graph;
        this.analyzer = new PerFieldAnalyzerWrapper(
                new StandardAnalyzer(),
                Map.of(
                        NAME, new SimpleAnalyzer(),
                        SUGGEST, new CompletionAnalyzer(new StandardAnalyzer())
                )
        );

        var directory = new ByteBuffersDirectory();

        try {
            try (
                    var directoryWriter = new IndexWriter(
                            directory, iwcWithSuggestField(analyzer, Set.of(SUGGEST)))
            ) {
                graph.getAllStopLocations()
                        .forEach(stopLocation -> addToIndex(
                                directoryWriter,
                                StopLocation.class,
                                stopLocation.getId().toString(),
                                stopLocation.getName(),
                                stopLocation.getCode(),
                                stopLocation.getCoordinate().latitude(),
                                stopLocation.getCoordinate().longitude()
                        ));

                graph.getAllStopCollections()
                        .forEach(stopCollection -> addToIndex(
                                directoryWriter,
                                StopCollection.class,
                                stopCollection.getId().toString(),
                                stopCollection.getName(),
                                null,
                                stopCollection.getCoordinate().latitude(),
                                stopCollection.getCoordinate().longitude()
                        ));

                graph.getVertices()
                        .stream()
                        .filter(v -> v instanceof StreetVertex)
                        .map(v -> (StreetVertex) v)
                        .forEach(streetVertex -> addToIndex(
                                directoryWriter,
                                StreetVertex.class,
                                streetVertex.getLabel(),
                                streetVertex.getIntersectionName(),
                                streetVertex.getLabel(),
                                streetVertex.getLat(),
                                streetVertex.getLon()
                        ));
            }

            DirectoryReader indexReader = DirectoryReader.open(directory);
            searcher = new SuggestIndexSearcher(indexReader);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Stream<Document> matchingDocuments(
            Class<?> type,
            String searchTerms,
            boolean autocomplete
    ) {
        try {
            if (autocomplete) {
                var completionQuery =
                        new PrefixCompletionQuery(
                                analyzer,
                                new Term(SUGGEST, analyzer.normalize(SUGGEST, searchTerms))
                        );
                var query = new ContextQuery(completionQuery);
                query.addContext(type.getSimpleName());

                var topDocs = searcher.suggest(query, 25, true);

                return Arrays.stream(topDocs.scoreDocs)
                        .map(scoreDoc -> {
                            try {
                                return searcher.doc(scoreDoc.doc);
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
            else {
                var parser = new QueryParser(CODE, analyzer);
                var nameQuery = parser.createPhraseQuery(NAME, searchTerms);
                var codeQuery =
                        new TermQuery(new Term(CODE, analyzer.normalize(CODE, searchTerms)));
                var typeQuery =
                        new TermQuery(
                                new Term(TYPE, analyzer.normalize(TYPE, type.getSimpleName())));

                var builder = new BooleanQuery.Builder()
                        .setMinimumNumberShouldMatch(1)
                        .add(typeQuery, Occur.MUST)
                        .add(codeQuery, Occur.SHOULD);

                if (nameQuery != null) {
                    builder.add(nameQuery, Occur.SHOULD);
                }

                var query = builder.build();

                var topDocs = searcher.search(query, 25);

                return Arrays.stream(topDocs.scoreDocs)
                        .map(scoreDoc -> {
                            try {
                                return searcher.doc(scoreDoc.doc);
                            }
                            catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Stream<StopLocation> queryStopLocations(
            String query,
            boolean autocomplete
    ) {
        return matchingDocuments(StopLocation.class, query, autocomplete)
                .map(document -> graph.getStopLocationById(FeedScopedId.parseId(document.get(ID))));
    }

    public Stream<StopCollection> queryStopCollections(String query, boolean autocomplete) {
        return matchingDocuments(StopCollection.class, query, autocomplete)
                .map(document -> graph.getStopCollectionById(
                        FeedScopedId.parseId(document.get(ID))));
    }

    public Stream<StreetVertex> queryStreetVertices(String query, boolean autocomplete) {
        return matchingDocuments(StreetVertex.class, query, autocomplete)
                .map(document -> (StreetVertex) graph.getVertex(document.get(ID)));
    }

    public static synchronized LuceneIndex forServer(Router router) {
        var graph = router.graph;
        var existingIndex = graph.getService(LuceneIndex.class);
        if (existingIndex != null) {
            return existingIndex;
        }

        var newIndex = new LuceneIndex(graph);
        graph.putService(LuceneIndex.class, newIndex);
        return newIndex;
    }

    private static void addToIndex(
            IndexWriter writer,
            Class<?> type,
            String id,
            I18NString name,
            @Nullable String code,
            double latitude,
            double longitude
    ) {
        String typeName = type.getSimpleName();

        Document document = new Document();
        document.add(new StoredField(ID, id));
        document.add(new TextField(TYPE, typeName, Store.YES));
        document.add(new TextField(NAME, Objects.toString(name), Store.YES));
        document.add(new ContextSuggestField(SUGGEST, Objects.toString(name), 1, typeName));
        document.add(new LatLonPoint(COORDINATE, latitude, longitude));

        if (code != null) {
            document.add(new TextField(CODE, code, Store.YES));
            document.add(new ContextSuggestField(SUGGEST, code, 1, typeName));
        }

        try {
            writer.addDocument(document);
        }
        catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    static IndexWriterConfig iwcWithSuggestField(
            Analyzer analyzer,
            final Set<String> suggestFields
    ) {
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        Codec filterCodec = new Lucene91Codec() {
            final PostingsFormat postingsFormat = new Completion90PostingsFormat();

            @Override
            public PostingsFormat getPostingsFormatForField(String field) {
                if (suggestFields.contains(field)) {return postingsFormat;}
                return super.getPostingsFormatForField(field);
            }
        };
        iwc.setCodec(filterCodec);
        return iwc;
    }
}
