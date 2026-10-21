package com.misanthropy.slashh.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SwingSpecs {
    public record Spec(boolean stab, double tilt, double pitch, double x, double y, double z) {}

    private static final Logger LOGGER = LogUtils.getLogger();
    private static Map<String, List<Spec>> table = Map.of();

    private SwingSpecs() {}

    public static void reload(ResourceManager manager) {
        Map<String, List<Spec>> map = new HashMap<>();
        manager.listResources("bc_particle", path -> path.getPath().endsWith("particle_settings.json")).forEach((location, resource) -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject values = GsonHelper.getAsJsonObject(GsonHelper.parse(reader), "values");
                for (String animation : values.keySet()) {
                    JsonElement element = values.get(animation);
                    JsonArray entries = new JsonArray();
                    if (element.isJsonArray()) entries = element.getAsJsonArray();
                    else entries.add(element);
                    List<Spec> specs = new ArrayList<>();
                    for (JsonElement entry : entries) {
                        Spec spec = parse(entry.getAsJsonObject());
                        if (spec != null) specs.add(spec);
                    }
                    map.put(animation, List.copyOf(specs));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to read slash settings {}", location, e);
            }
        });
        table = map;
    }

    static @Nullable Spec parse(JsonObject json) {
        String type = GsonHelper.getAsString(json, "particle_type", "none");
        if (!type.equals("stab") && !type.startsWith("slash")) return null;
        return new Spec(type.equals("stab"), GsonHelper.getAsFloat(json, "roll_set", 0.0f), GsonHelper.getAsFloat(json, "pitch_addition", 0.0f),
                GsonHelper.getAsFloat(json, "x_addition", 0.0f), GsonHelper.getAsFloat(json, "y_addition", 0.0f),
                GsonHelper.getAsFloat(json, "z_addition", 0.0f));
    }

    public static List<Spec> specs(String animation) {
        List<Spec> tuned = table.get(animation);
        if (tuned != null) return tuned;
        Spec guess = guess(animation);
        return guess == null ? List.of() : List.of(guess);
    }

    static @Nullable Spec guess(String animation) {
        String id = animation.toLowerCase(Locale.ROOT);
        id = id.substring(id.indexOf(':') + 1);
        List<String> words = List.of(id.split("[_./]"));
        if (has(id, "throw", "kick", "pong", "gun")) return null;
        if (has(id, "stab", "thrust", "pierce", "punch")) return new Spec(true, 0.0, 0.0, 0.0, -0.1, 0.0);
        if (has(id, "spin", "whirl", "rotate")) return arc(180.0);
        if (has(id, "slam")) return arc(-86.0);
        if (has(id, "uppercut", "upward", "heavy_up")) return arc(75.0);
        boolean mirrored = words.contains("left") || words.contains("lr");
        if (has(id, "vert", "updown")) return arc(mirrored ? -100.0 : -80.0);
        if (has(id, "uncross")) return arc(-60.0);
        if (has(id, "cross")) return arc(-120.0);
        return arc(mirrored ? 180.0 : 0.0);
    }

    public static Spec arc(double tilt) {
        return new Spec(false, tilt, 0.0, 0.0, -0.1, 0.0);
    }

    private static boolean has(String id, String... keys) {
        for (String key : keys) {
            if (id.contains(key)) return true;
        }
        return false;
    }
}
