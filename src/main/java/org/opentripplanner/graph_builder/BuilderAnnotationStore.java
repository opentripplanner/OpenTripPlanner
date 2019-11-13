package org.opentripplanner.graph_builder;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.opentripplanner.graph_builder.annotation.GraphBuilderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class BuilderAnnotationStore {
        private static final Logger GRAPH_BUILDER_ANNOTATION_LOG =
                LoggerFactory.getLogger("GRAPH_BUILDER_ANNOTATION_LOG");

        private final List<GraphBuilderAnnotation> annotations = new ArrayList<>();

        private final boolean storeAnnotations;

        public BuilderAnnotationStore(boolean storeAnnotations) {
                this.storeAnnotations = storeAnnotations;
        }

        public void add(GraphBuilderAnnotation annotation) {
                GRAPH_BUILDER_ANNOTATION_LOG.info(annotation.getMessage());
                if (storeAnnotations) {
                        this.annotations.add(annotation);
                }
        }

        public void summarize() {
                Multiset<Class<? extends GraphBuilderAnnotation>> classes = HashMultiset.create();
                GRAPH_BUILDER_ANNOTATION_LOG.info("Summary (number of each type of annotation):");
                for (GraphBuilderAnnotation gba : annotations)
                        classes.add(gba.getClass());
                for (Multiset.Entry<Class<? extends GraphBuilderAnnotation>> e : classes.entrySet()) {
                        String name = e.getElement().getSimpleName();
                        int count = e.getCount();
                        GRAPH_BUILDER_ANNOTATION_LOG.info("    {} - {}", name, count);
                }
        }
}
