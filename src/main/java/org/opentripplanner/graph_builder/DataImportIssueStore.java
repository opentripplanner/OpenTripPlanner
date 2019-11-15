package org.opentripplanner.graph_builder;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.opentripplanner.graph_builder.annotation.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DataImportIssueStore {
        private static final Logger GRAPH_BUILDER_ANNOTATION_LOG =
                LoggerFactory.getLogger("GRAPH_BUILDER_ANNOTATION_LOG");

        private final List<DataImportIssue> annotations = new ArrayList<>();

        private final boolean storeAnnotations;

        public DataImportIssueStore(boolean storeAnnotations) {
                this.storeAnnotations = storeAnnotations;
        }

        public void add(DataImportIssue annotation) {
                GRAPH_BUILDER_ANNOTATION_LOG.info(annotation.getMessage());
                if (storeAnnotations) {
                        this.annotations.add(annotation);
                }
        }

        public List<DataImportIssue> getAnnotations() {
                return this.annotations;
        }

        public void summarize() {
                Multiset<Class<? extends DataImportIssue>> classes = HashMultiset.create();
                GRAPH_BUILDER_ANNOTATION_LOG.info("Summary (number of each type of annotation):");
                for (DataImportIssue gba : annotations)
                        classes.add(gba.getClass());
                for (Multiset.Entry<Class<? extends DataImportIssue>> e : classes.entrySet()) {
                        String name = e.getElement().getSimpleName();
                        int count = e.getCount();
                        GRAPH_BUILDER_ANNOTATION_LOG.info("    {} - {}", name, count);
                }
        }
}
