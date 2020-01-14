package com.ocadotechnology.trafficlightsimulation.util;

public class PropertiesUtil {
    public static String getPropertyName(Enum<?> key) {
        Class<?> clazz = key.getClass();
        return clazz.getCanonicalName().replace(clazz.getPackage().getName() + ".", "")
                + "."
                + key.name();
    }

}
