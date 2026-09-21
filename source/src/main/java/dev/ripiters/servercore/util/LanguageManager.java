package dev.ripiters.servercore.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

// fixme: rework
public class LanguageManager {

    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();
    private static final Gson GSON = new Gson();

    static {
        loadTranslations();
    }

    public static synchronized void loadTranslations() {
        TRANSLATIONS.clear();
        loadBuiltInLanguage("en_us");
        loadBuiltInLanguage("pl_pl");
        loadCustomLanguages();
    }

    private static void loadBuiltInLanguage(String langCode) {
        String resourcePath = "/assets/servercore/lang/" + langCode + ".json";
        try (InputStream stream = LanguageManager.class.getResourceAsStream(resourcePath)) {
            if (stream != null) {
                try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                    Map<String, String> map = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                    if (map != null) {
                        TRANSLATIONS.computeIfAbsent(langCode, k -> new HashMap<>()).putAll(map);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadCustomLanguages() {
        Path customDir = Paths.get("config", "servercore", "lang", "custom");
        try {
            if (!Files.exists(customDir)) {
                Files.createDirectories(customDir);
                return;
            }

            try (Stream<Path> stream = Files.walk(customDir)) {
                stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).forEach(p -> {
                            String fileName = p.getFileName().toString();
                            String langCode = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                            try (Reader reader = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
                                Map<String, String> customMap = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
                                if (customMap != null) {
                                    TRANSLATIONS.computeIfAbsent(langCode, k -> new HashMap<>()).putAll(customMap);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Component get(ServerPlayer player, String key, Object... args) {
        String lang = (player != null && player.clientInformation() != null) ? player.clientInformation().language().toLowerCase() : "en_us";
        Map<String, String> langMap = TRANSLATIONS.getOrDefault(lang, TRANSLATIONS.getOrDefault("en_us", new HashMap<>()));
        Map<String, String> fallbackMap = TRANSLATIONS.getOrDefault("en_us", new HashMap<>());
        String pattern = langMap.getOrDefault(key, fallbackMap.getOrDefault(key, key));

        if (args.length > 0) {
            return Component.literal(String.format(pattern, args));
        }
        return Component.literal(pattern);
    }
}