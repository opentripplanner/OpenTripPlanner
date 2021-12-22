package org.opentripplanner.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.onebusaway.gtfs.model.Translation;

public final class TranslationHelper {

    private static final String TABLE_NAME_FEED_INFO = "feed_info";
    private static String feedLanguage = null;

    private static Map<String, Map<Optional<String>, List<Translation>>> translationMap =
            new HashMap<>();

    public TranslationHelper(Collection<Translation> allTranslations, String language) {
        feedLanguage = language;
        Map<String, List<Translation>> byTableName =
                allTranslations.stream().collect(Collectors.groupingBy(t -> t.getTableName()));
        for (String tableName : byTableName.keySet()) {
            if (tableName.equals(TABLE_NAME_FEED_INFO)) {
                translationMap.put(tableName, byTableName.get(tableName)
                        .stream()
                        .collect(Collectors.groupingBy(t -> Optional.ofNullable(""))));
            }
            else {
                translationMap.put(tableName + "_by_record", byTableName.get(tableName)
                        .stream()
                        .filter(t -> t.getFieldValue() == null)
                        .collect(Collectors.groupingBy(t -> t.getRecordSubId() != null
                                ? Optional.ofNullable(
                                String.join("_", t.getRecordId(), t.getRecordSubId()))
                                : Optional.ofNullable(t.getRecordId()))));
                translationMap.put(tableName + "_by_field", byTableName.get(tableName)
                        .stream()
                        .filter(t -> t.getFieldValue() != null && t.getRecordId() == null)
                        .collect(Collectors.groupingBy(
                                t -> Optional.ofNullable(t.getFieldValue()))));
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
        if (tableName.equals(TABLE_NAME_FEED_INFO)) {
            translationList = translationMap.get(tableName).get(Optional.ofNullable(""));
        }
        else {
            translationList =
                    translationMap.get(tableName + "_by_record").get(Optional.ofNullable(key));
            if (translationList == null) {
                translationList = translationMap.get(tableName + "_by_field")
                        .get(Optional.ofNullable(defaultValue));
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
        return null;
    }
}
