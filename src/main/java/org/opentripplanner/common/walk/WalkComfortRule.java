package org.opentripplanner.common.walk;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A single rule for use in calculating walk comfort scores. Maps an WalkComfortTest
 * to a factor (relative to 1.0) that is used in computing the score.
 *
 * Created by demory on 11/20/17.
 */

public class WalkComfortRule {

    private static final Logger LOG = LoggerFactory.getLogger(WalkComfortRule.class);

    /**
     * the "root" test for this rule
     */
    private WalkComfortTest test;

    /**
     * the factor to apply to the composite comfort score if this rule's test is satisfied
     */
    private float factor;

    public WalkComfortRule(JsonNode ruleNode) { //String osmKey, String referenceValue, float factor) {
        this.factor = ruleNode.get("factor").floatValue();
        this.test = createTestFromNode(ruleNode);
    }

    public float getFactor() {
        return this.factor;
    }

    /**
     * returns this rule's factor if test passes, or 1.0 if not
     */
    public float computeFactor(Map<String, String> tags) {
        return test.apply(tags) ? factor : 1.0f;
    }

    private WalkComfortTest createTestFromNode(JsonNode node) {
        String type = null;
        if (node.has("type")) type = node.get("type").asText();
        else if (node.has("key") && node.has("referenceValue")) type = "equal";

        switch (type) {
            case "equal": return new EqualTest(node);
            case "numeric-equal": return new NumericEqualTest(node);
            case "greater": return new GreaterTest(node);
            case "greater-equal": return new GreaterEqualTest(node);
            case "less": return new LessTest(node);
            case "less-equal": return new LessEqualTest(node);
            case "absent": return new AbsentTagTest(node);
            case "and": return new AndTest(node);
            case "or": return new OrTest(node);
        }

        return null;
    }


    /**
     * WalkComfortTest interface, defines a single method to apply test to an OSM way's tag/value set
     */
    private interface WalkComfortTest {
        boolean apply(Map<String, String> tags);
    }

    /**
     * A test to check if a specified tag matches one or more specified values
     */
    private class EqualTest implements WalkComfortTest {

        /**
         * the name (key) of an OSM tag to match
         **/
        private String tagKey;

        /**
         * a set of one or more values to match
         **/
        private Set<String> values;

        EqualTest(JsonNode node) {
            // Read the key from the node, which is always a single referenceValue
            this.tagKey = node.get("key").asText();

            // Read the values which might be a single referenceValue or an array
            values = new HashSet<>();
            if (node.get("value").isArray()) {
                Iterator<JsonNode> arrIter = node.get("value").elements();
                while (arrIter.hasNext()) {
                    JsonNode arrNode = arrIter.next();
                    values.add(arrNode.textValue());
                }
            } else {
                values.add(node.get("value").asText());
            }
        }

        @Override
        public boolean apply(Map<String, String> tags) {
            if (tags.containsKey(tagKey)) {
                String value = tags.get(tagKey);
                if (values.contains(value)) return true;
            }
            return false;
        }
    }

    /**
     * An abstract test to check a numeric tag value against a defined reference value
     */
    private abstract class NumericTest implements WalkComfortTest {

        // the name (key) of an OSM tag to match
        String tagKey;

        // a numeric referenceValue that the test referenceValue must be greater than
        Double referenceValue;

        NumericTest(JsonNode node) {
            // Read the key from the node
            this.tagKey = node.get("key").asText();

            // Read the referenceValue and attempt to process as number
            String valStr = node.get("value").asText();
            try {
                referenceValue = Double.valueOf(valStr);
            } catch (NumberFormatException e) {
                referenceValue = null;
                LOG.warn("Numeric WalkComfortTest defined with non-numeric referenceValue: " + valStr);
            }
        }

        @Override
        public boolean apply(Map<String, String> tags) {
            if (referenceValue == null || !tags.containsKey(tagKey)) return false;
            try {
                Double tagValue = Double.valueOf(tags.get(tagKey));
                if (applyNumeric(tagValue)) return true;
            } catch (NumberFormatException e) {
                LOG.warn("Numeric test encountered non-numeric tag value: " + tags.get(tagKey));
            }
            return false;
        }

        abstract boolean applyNumeric (double tagValue);
    }

    /**
     * A test to check if a numeric tag value is equal to a defined reference value
     */
    private class NumericEqualTest extends NumericTest {
        NumericEqualTest (JsonNode node) { super(node); }

        boolean applyNumeric (double tagValue) {
            return tagValue == referenceValue;
        }
    }

    /**
     * A test to check if a numeric tag value is greater than a defined reference value
     */
    private class GreaterTest extends NumericTest {
        GreaterTest (JsonNode node) { super(node); }

        boolean applyNumeric (double tagValue) {
            return tagValue > referenceValue;
        }
    }

    /**
     * A test to check if a numeric tag value is greater than or equal to a defined reference value
     */
    private class GreaterEqualTest extends NumericTest {
        GreaterEqualTest (JsonNode node) { super(node); }

        boolean applyNumeric (double tagValue) {
            return tagValue >= referenceValue;
        }
    }

    /**
     * A test to check if a numeric tag value is less than a defined reference value
     */
    private class LessTest extends NumericTest {
        LessTest (JsonNode node) { super(node); }

        boolean applyNumeric (double tagValue) {
            return tagValue < referenceValue;
        }
    }

    /**
     * A test to check if a numeric tag value is less than or equal to a defined reference value
     */
    private class LessEqualTest extends NumericTest {
        LessEqualTest (JsonNode node) { super(node); }

        boolean applyNumeric (double tagValue) {
            return tagValue <= referenceValue;
        }
    }

    /**
     *  A test that passes only if a specified tag is NOT specified
     */
    private class AbsentTagTest implements WalkComfortTest {

        /**
         * the name (key) of an OSM tag that must be absent
         **/
        private String tagKey;

        AbsentTagTest(JsonNode node) {
            // Read the key from the node
            this.tagKey = node.get("key").asText();
        }

        @Override
        public boolean apply(Map<String, String> tags) {
            return !tags.containsKey(tagKey);
        }
    }

    /**
     *  A test consisting of a set of sub-tests, where ALL sub-tests must pass for the overall test to pass
     */
    private class AndTest implements WalkComfortTest {
        private Set<WalkComfortTest> tests = new HashSet<>();

        AndTest(JsonNode node) {
            Iterator<JsonNode> testIter = node.get("tests").elements();
            while (testIter.hasNext()) {
                tests.add(createTestFromNode(testIter.next()));
            }
        }

        @Override
        public boolean apply(Map<String, String> tags) {
            int passedTests = 0;
            for (WalkComfortTest test : tests) {
                if (test.apply(tags)) passedTests++;
            }
            return passedTests == tests.size();
        }
    }

    /**
     * A test consisting of a set of sub-tests, where at least ONE sub-test must pass for the overall test to pass
     */
    private class OrTest implements WalkComfortTest {
        private Set<WalkComfortTest> tests = new HashSet<>();

        OrTest(JsonNode node) {
            Iterator<JsonNode> testIter = node.get("tests").elements();
            while (testIter.hasNext()) {
                tests.add(createTestFromNode(testIter.next()));
            }
        }

        @Override
        public boolean apply(Map<String, String> tags) {
            int passedTests = 0;
            for (WalkComfortTest test : tests) {
                if (test.apply(tags)) passedTests++;
            }
            return passedTests > 0;
        }
    }
}
