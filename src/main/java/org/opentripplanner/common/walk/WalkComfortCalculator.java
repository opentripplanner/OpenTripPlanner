package org.opentripplanner.common.walk;

import com.fasterxml.jackson.databind.JsonNode;
import org.opentripplanner.openstreetmap.model.OSMWithTags;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by demory on 11/20/17.
 */

public class WalkComfortCalculator {
    private static Logger LOG = LoggerFactory.getLogger(WalkComfortCalculator.class);

    public static final String WALK_CONFIG_FILENAME = "walk-config.json";
    private List<WalkComfortRule> rules = new ArrayList<>();

    public WalkComfortCalculator(JsonNode walkConfig) {

        if (walkConfig == null || !walkConfig.has("rules") || !walkConfig.get("rules").isArray()) {
            LOG.info("No rules found in walk config");
            return;
        }

        Iterator<JsonNode> ruleIter = walkConfig.get("rules").elements();
        while(ruleIter.hasNext()) {
            JsonNode ruleNode = ruleIter.next();
            String key = ruleNode.get("key").asText();
            String value = ruleNode.get("value").asText();
            float factor = ruleNode.get("factor").floatValue();
            WalkComfortRule rule = new WalkComfortRule(key, value, factor);
            rules.add(rule);
        }
    }

    public float computeScore(OSMWithTags way) {
        return computeScore(way.getTags());
    }

    public float computeScore(Map<String, String> tags) {
        float factor = 1.0f;
        for(WalkComfortRule rule : rules) {
            String val = tags.get(rule.getTagKey());
            if(val == null) continue;
            float f = rule.computeFactor(val);
            factor = factor * f;
        }
        return factor;
    }
}
