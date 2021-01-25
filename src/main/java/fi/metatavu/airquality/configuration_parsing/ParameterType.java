package fi.metatavu.airquality.configuration_parsing;

import com.google.gson.annotations.SerializedName;

public enum ParameterType {
        @SerializedName(value = "Threshold", alternate = {"threshold", "THRESHOLD"})
        THRESHOLD,
        @SerializedName(value = "Penalty", alternate = {"penalty", "PENALTY"})
        PENALTY
}
