package com.auction.common.util;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class JsonUtil {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>)
                    (src, type, ctx) -> new JsonPrimitive(src.format(DTF)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>)
                    (json, type, ctx) -> LocalDateTime.parse(json.getAsString(), DTF))
            .create();

    private JsonUtil() {}
    public static String toJson(Object obj) { return GSON.toJson(obj); }
    public static <T> T fromJson(String json, Class<T> clazz) { return GSON.fromJson(json, clazz); }
}
