package org.opentripplanner.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.onebusaway.gtfs.model.Translation;

public final class TranslationHelper {

    public static final String TABLE_NAME_FEED_INFO = "feed_info";

    private final Map<String, List<Translation>> allGroupedByTableName;

    public TranslationHelper(Collection<Translation> allTranslations) {
        this.allGroupedByTableName =
                allTranslations.stream().collect(Collectors.groupingBy(Translation::getTableName));
    }

    private String getKey(Translation translation) {
        if (translation.getRecordId() != null && translation.getRecordSubId() == null && translation.getFieldValue() == null) {
            return translation.getRecordId();
        } else if (translation.getRecordId() != null && translation.getRecordSubId() != null && translation.getFieldValue() == null) {
            return String.join("_", translation.getRecordId(), translation.getRecordSubId());
        } else if (translation.getRecordId() == null && translation.getFieldValue() != null) {
            return translation.getFieldValue();
        }
        return null;
    }

    public Map<Optional<String>, List<Translation>> getByTableName(String tableName) {
        if (allGroupedByTableName.containsKey(tableName)) {
            if (!tableName.equals(TABLE_NAME_FEED_INFO)) {
                return allGroupedByTableName.get(tableName)
                        .stream()
                        .collect(Collectors.groupingBy(
                                translation -> Optional.ofNullable(getKey(translation))));
            } else {
                return allGroupedByTableName.get(tableName)
                        .stream()
                        .collect(Collectors.groupingBy(translation -> Optional.ofNullable(null)));
            }
        }
        return null;
    }

    public static I18NString getTranslationsByFieldName(
            List<Translation> translationList,
            String fieldName,
            String defaultValue,
            String defaultLanguage
    ) {
        if(translationList == null || translationList.isEmpty()) {
            return null;
        }

        HashMap<String, String> hm = new HashMap<>();
        hm.put(defaultLanguage, defaultValue);
        for (Translation translation : translationList) {
            if (translation.getFieldName().equals(fieldName)) {
                hm.put(translation.getLanguage(), translation.getTranslation());
            }
        }
        return TranslatedString.getI18NString(hm);
    }
}
