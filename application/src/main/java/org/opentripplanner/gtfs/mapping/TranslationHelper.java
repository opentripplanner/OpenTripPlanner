package org.opentripplanner.gtfs.mapping;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.onebusaway.csv_entities.schema.annotations.CsvField;
import org.onebusaway.csv_entities.schema.annotations.CsvFieldNameConvention;
import org.onebusaway.csv_entities.schema.annotations.CsvFields;
import org.onebusaway.gtfs.model.FeedInfo;
import org.onebusaway.gtfs.model.Translation;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.i18n.TranslatedString;

/**
 * This helper is util for translating wanted text found in GTFS's translation.txt -file.
 * <p>
 * If translation not found then it will return given default value.
 */

final class TranslationHelper {

  private static final String TABLE_FEED_INFO = "feed_info";
  private final Map<String, Map<String, List<Translation>>> translationMap = new HashMap<>();
  private String feedLanguage = null;

  void importTranslations(Collection<Translation> allTranslations, Collection<FeedInfo> feedInfos) {
    if (feedInfos.iterator().hasNext()) {
      feedLanguage = feedInfos.iterator().next().getLang();
    }

    Map<String, List<Translation>> byTableName = allTranslations
      .stream()
      .collect(Collectors.groupingBy(Translation::getTableName));

    for (Map.Entry<String, List<Translation>> i : byTableName.entrySet()) {
      String tableName = i.getKey();
      if (tableName.equals(TABLE_FEED_INFO)) {
        // will create with following structure:
        // {<tableName>={""=[Translation@1, ..., Translation@Z]}}
        translationMap.put(
          tableName,
          i.getValue().stream().collect(Collectors.groupingBy(t -> ""))
        );
      } else {
        // will create with following structure:
        // {<tableName>_by_record={<recordId@1>=[Translation@1, ..., Translation@Z], ..., <recordId@Z>=[Translation@1, ..., Translation@Z]}}
        // or with record sub id (if exists)
        // {<tableName>_by_record={<recordId@1_recordSubId@1>=[Translation@1, ..., Translation@Z], ..., <recordId@Z_recordSubId@Z>=[Translation@1, ..., Translation@Z]}}
        translationMap.put(
          tableName + "_by_record",
          i
            .getValue()
            .stream()
            .filter(t -> t.getFieldValue() == null)
            .collect(
              Collectors.groupingBy(t ->
                t.getRecordSubId() != null
                  ? String.join("_", t.getRecordId(), t.getRecordSubId())
                  : t.getRecordId()
              )
            )
        );
        // will create with following structure:
        // {<tableName>_by_field={<field@1>=[Translation@1, ..., Translation@Z], ..., <field@Z>=[Translation@1, ..., Translation@Z]}}
        translationMap.put(
          tableName + "_by_field",
          i
            .getValue()
            .stream()
            .filter(t -> t.getFieldValue() != null && t.getRecordId() == null)
            .collect(Collectors.groupingBy(Translation::getFieldValue))
        );
      }
    }
  }

  @Nullable
  I18NString getTranslation(
    Class<?> clazz,
    String fieldName,
    String recordId,
    @Nullable String defaultValue
  ) {
    return getTranslation(clazz, fieldName, recordId, null, defaultValue);
  }

  @Nullable
  I18NString getTranslation(
    Class<?> clazz,
    String fieldName,
    String recordId,
    @Nullable String recordSubId,
    @Nullable String defaultValue
  ) {
    if (defaultValue == null) {
      return null;
    }

    Field field;
    try {
      field = clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException e) {
      throw new RuntimeException(e);
    }

    CsvFields csvFields = field.getDeclaringClass().getAnnotation(CsvFields.class);
    String fileName = csvFields.filename();
    String prefix = csvFields.prefix();
    CsvFieldNameConvention fieldNameConvention = csvFields.fieldNameConvention();

    // Fields with default name are initialized with an empty string.
    // Without any configuration, no annotation is used.
    String csvFieldName = Optional.ofNullable(field.getAnnotation(CsvField.class))
      .map(CsvField::name)
      .orElse("");

    String tableName = fileName.replace(".txt", "");
    String translationFieldName = csvFieldName.isEmpty()
      ? prefix + getObjectFieldNameAsCSVFieldName(field.getName(), fieldNameConvention)
      : csvFieldName;

    List<Translation> translationList = null;
    String key = recordSubId != null ? String.join("_", recordId, recordSubId) : recordId;
    if (tableName.equals(TABLE_FEED_INFO)) {
      Map<String, List<Translation>> feeds = translationMap.get(tableName);
      if (feeds != null) {
        translationList = feeds.get("");
      }
    } else {
      Map<String, List<Translation>> translationsByKey = translationMap.get(
        tableName + "_by_record"
      );
      if (translationsByKey != null) {
        translationList = translationsByKey.get(key);
      }
      if (translationList == null) {
        Map<String, List<Translation>> translationsByValue = translationMap.get(
          tableName + "_by_field"
        );
        if (translationsByValue != null) {
          translationList = translationsByValue.get(defaultValue);
        }
      }
    }
    if (translationList != null) {
      HashMap<String, String> hm = new HashMap<>();
      hm.put(null, defaultValue);
      hm.put(feedLanguage, defaultValue);
      for (Translation translation : translationList) {
        if (translationFieldName.equals(translation.getFieldName())) {
          hm.put(translation.getLanguage(), translation.getTranslation());
        }
      }
      return TranslatedString.getI18NString(hm, true, false);
    }
    return NonLocalizedString.ofNullable(defaultValue);
  }

  private String getObjectFieldNameAsCSVFieldName(
    String fieldName,
    CsvFieldNameConvention fieldNameConvention
  ) {
    if (fieldNameConvention == CsvFieldNameConvention.CAMEL_CASE) return fieldName;

    if (fieldNameConvention == CsvFieldNameConvention.CAPITALIZED_CAMEL_CASE) {
      return fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
    }

    StringBuilder b = new StringBuilder();
    boolean wasUpperCase = false;

    for (int i = 0; i < fieldName.length(); i++) {
      char c = fieldName.charAt(i);
      boolean isUpperCase = Character.isUpperCase(c);
      if (isUpperCase) c = Character.toLowerCase(c);
      if (isUpperCase && !wasUpperCase) b.append('_');
      b.append(c);
      wasUpperCase = isUpperCase;
    }

    return b.toString();
  }
}
