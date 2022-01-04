package org.opentripplanner.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Translation;

/**
 * This helper is util for translating wanted text found in GTFS's translation.txt -file.
 *
 * If translation not found then it will return given default value.
 */

public final class TranslationHelper {


    public static final String TABLE_FEED_INFO = "feed_info";
    public static final String TABLE_STOPS = "stops";

    public static final String STOP_NAME = "stop_name";
    public static final String STOP_URL = "stop_url";

    private static String feedLanguage = null;

    private static Map<String, Map<String, List<Translation>>> translationMap =
            new HashMap<>();

    public TranslationHelper(Collection<Translation> allTranslations, Collection<FeedInfo> feedInfos) {
        if (feedInfos.iterator().hasNext()) {
            feedLanguage = feedInfos.iterator().next().getLang();
        }

        Map<String, List<Translation>> byTableName =
                allTranslations.stream().collect(Collectors.groupingBy(t -> t.getTableName()));

        for (Map.Entry<String, List<Translation>> i : byTableName.entrySet()) {
            String tableName = i.getKey();
            if (tableName.equals(TABLE_FEED_INFO)) {
                // will create with following structure:
                // {<tableName>={""=[Translation@1, ..., Translation@Z]}}
                translationMap.put(tableName, i.getValue()
                        .stream()
                        .collect(Collectors.groupingBy(t -> "")));
            }
            else {
                // will create with following structure:
                // {<tableName>_by_record={<recordId@1>=[Translation@1, ..., Translation@Z], ..., <recordId@Z>=[Translation@1, ..., Translation@Z]}}
                // or with record sub id (if exists)
                // {<tableName>_by_record={<recordId@1_recordSubId@1>=[Translation@1, ..., Translation@Z], ..., <recordId@Z_recordSubId@Z>=[Translation@1, ..., Translation@Z]}}
                translationMap.put(tableName + "_by_record", i.getValue()
                        .stream()
                        .filter(t -> t.getFieldValue() == null)
                        .collect(Collectors.groupingBy(t -> t.getRecordSubId() != null
                                ? String.join("_", t.getRecordId(), t.getRecordSubId())
                                : t.getRecordId())));
                // will create with following structure:
                // {<tableName>_by_field={<field@1>=[Translation@1, ..., Translation@Z], ..., <field@Z>=[Translation@1, ..., Translation@Z]}}
                translationMap.put(tableName + "_by_field", i.getValue()
                        .stream()
                        .filter(t -> t.getFieldValue() != null && t.getRecordId() == null)
                        .collect(Collectors.groupingBy(
                                t -> t.getFieldValue())));
            }
        }
    }

    public I18NString getTranslation(
            String tableName,
            String fieldName,
            String recordId,
            String recordSubId,
            String defaultValue
    ) {
        List<Translation> translationList = null;
        String key = recordSubId != null ? String.join("_", recordId, recordSubId) : recordId;
        if (tableName.equals(TABLE_FEED_INFO)) {
            translationList = translationMap.get(tableName).get("");
        }
        else {
            translationList = translationMap.get(tableName + "_by_record").get(key);
            if (translationList == null) {
                translationList = translationMap.get(tableName + "_by_field")
                        .get(defaultValue);
            }
        }
        if (translationList != null) {
            HashMap<String, String> hm = new HashMap<>();
            hm.put(feedLanguage, defaultValue);
            for (Translation translation : translationList) {
                if (fieldName.equals(translation.getFieldName())) {
                    hm.put(translation.getLanguage(), translation.getTranslation());
                }
            }
            return TranslatedString.getI18NString(hm);
        }
        return new NonLocalizedString(defaultValue);
    }
}
