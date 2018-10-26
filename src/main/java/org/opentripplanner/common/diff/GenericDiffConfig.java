package org.opentripplanner.common.diff;

import com.google.common.collect.Sets;

import java.util.Set;

public class GenericDiffConfig {

    /**
     * Current depth of recursive progression
     * Actually, not a configuration, but a status.
     * Should have been a DiffStatus object.
     */
    protected int depth;

    protected Set<String> ignoreFields = Sets.newHashSet();

    protected Set<String> identifiers = Sets.newHashSet();

    protected Set<Class> onlyDoEqualsCheck = Sets.newHashSet();

    protected Set<Class> useEqualsBuilder = Sets.newHashSet();

    public static GenericDiffConfigBuilder builder() {
        return new GenericDiffConfigBuilder();
    }

    public static class GenericDiffConfigBuilder {

        private final GenericDiffConfig genericDiffConfig;

        public GenericDiffConfigBuilder() {
            genericDiffConfig = new GenericDiffConfig();
        }

        /**
         * Fields to be treated as identifiers in collections (if they apply for type)
         */
        public GenericDiffConfigBuilder identifiers(Set<String> identifiers) {
            genericDiffConfig.identifiers = identifiers;
            return this;
        }

        /**
         * Do not compare these types recursively. Only check the equals method.
         */
        public GenericDiffConfigBuilder onlyDoEqualsCheck(Set<Class> onlyDoEqualsCheck) {
            genericDiffConfig.onlyDoEqualsCheck = onlyDoEqualsCheck;
            return this;
        }

        /**
         * Common field names to ignore for all objects
         */
        public GenericDiffConfigBuilder ignoreFields(Set<String> ignoreFields) {
            genericDiffConfig.ignoreFields = ignoreFields;
            return this;
        }

        public GenericDiffConfigBuilder useEqualsBuilder(Set<Class> classes) {
            genericDiffConfig.useEqualsBuilder = classes;
            return this;
        }

        public GenericDiffConfig build() {
            return genericDiffConfig;
        }

    }

}
