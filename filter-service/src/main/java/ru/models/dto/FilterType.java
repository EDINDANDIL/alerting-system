package ru.models.dto;

public enum FilterType {
    IMPULSE;

    public static FilterType fromPath(String path) {
        return FilterType.valueOf(path.toUpperCase());
    }
}
