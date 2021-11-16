package org.opentripplanner.ext.airquality.configuration;

import com.google.gson.annotations.SerializedName;

public enum TimeUnit {
    @SerializedName(value = "SEC")
    SECONDS,
    @SerializedName(value = "HR")
    HOURS,
    @SerializedName(value = "MS_EPOCH")
    MS_EPOCH
}
