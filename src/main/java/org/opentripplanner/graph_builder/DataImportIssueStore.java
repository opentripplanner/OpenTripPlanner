package org.opentripplanner.graph_builder;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import org.opentripplanner.graph_builder.annotation.DataImportIssue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DataImportIssueStore {
        private static final Logger DATA_IMPORT_ISSUES_LOG =
            LoggerFactory.getLogger("DATA_IMPORT_ISSUES");

        private final List<DataImportIssue> issues = new ArrayList<>();

        private final boolean storeIssues;

        public DataImportIssueStore(boolean storeIssues) {
                this.storeIssues = storeIssues;
        }

        public void add(DataImportIssue issue) {
                DATA_IMPORT_ISSUES_LOG.info(issue.getMessage());
                if (storeIssues) {
                        this.issues.add(issue);
                }
        }

        public List<DataImportIssue> getIssues() {
                return this.issues;
        }

        public void summarize() {
                Multiset<Class<? extends DataImportIssue>> classes = HashMultiset.create();
                DATA_IMPORT_ISSUES_LOG.info("Summary (number of each type of issue):");
                for (DataImportIssue gba : issues)
                        classes.add(gba.getClass());
                for (Multiset.Entry<Class<? extends DataImportIssue>> e : classes.entrySet()) {
                        String name = e.getElement().getSimpleName();
                        int count = e.getCount();
                        DATA_IMPORT_ISSUES_LOG.info("    {} - {}", name, count);
                }
        }
}
