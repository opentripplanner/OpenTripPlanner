package fi.metatavu.airquality.configuration_parsing;

import com.google.gson.annotations.SerializedName;

public enum TimeUnit {
    @SerializedName(value = "SEC")
    SECONDS,
    @SerializedName(value = "HR")
    HOURS,
    @SerializedName(value = "MS_EPOCH")
    MS_EPOCH
}
