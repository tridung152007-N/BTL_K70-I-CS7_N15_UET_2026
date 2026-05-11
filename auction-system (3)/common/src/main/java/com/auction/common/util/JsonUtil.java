package com.auction.common.util;

import com.google.gson.Gson;

public final class JsonUtil {
    private static final Gson GSON = new Gson();
    private JsonUtil() {}
    public static String toJson(Object obj) { return GSON.toJson(obj); }
    public static <T> T fromJson(String json, Class<T> clazz) { return GSON.fromJson(json, clazz); }
}
