package dev.ripiters.servercore.config;

import net.createmod.catnip.config.ConfigBase;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class CommonConfig extends ConfigBase {

    public ModConfigSpec.ConfigValue<String> modpackVersion;
    public ModConfigSpec.ConfigValue<String> versionCheckUrl;

    public ModConfigSpec.ConfigValue<List<? extends String>> allowedAdmins;
    public ModConfigSpec.ConfigValue<List<? extends String>> suppressedLogs;

    public ConfigBool enableJoinMessage = b(true, "enableJoinMessage", "Enable custom join message on chat");
    public ModConfigSpec.ConfigValue<String> joinMessage;

    public final ConfigGroup trains = group(0, "trains", "Create Train Limits");
    public final ConfigInt maxTrainsPerPlayer = i(1, 0, 100, "maxTrainsPerPlayer", "Maximum trains per player");

    public final ConfigGroup worldgen = group(0, "worldgen", "World Generation Settings");
    public final ConfigBool preventSpawnDungeons = b(true, "preventSpawnDungeons", "Prevent dungeons from generating near 0,0 spawn area");
    public final ConfigInt spawnExclusionRadius = i(600, 0, 100000, "spawnExclusionRadius", "Radius in blocks around 0,0 where dungeons are forbidden");
    public final ConfigInt largeDungeonSpacing = i(320, 10, 1000, "largeDungeonSpacing", "Spacing for large dungeons");
    public final ConfigInt largeDungeonSeparation = i(220, 5, 999, "largeDungeonSeparation", "Separation for large dungeons");

    public final ConfigInt villageSpacing = i(68, 10, 1000, "villageSpacing", "Spacing for minecraft villages (Vanilla default: 34)");
    public final ConfigInt villageSeparation = i(16, 5, 999, "villageSeparation", "Separation for minecraft villages (Vanilla default: 8)");

    public final ConfigGroup ivp = group(0, "ivp", "Improved Village Placement Settings");
    public final ConfigBool ivpEnabled = b(true, "ivpEnabled", "Master switch for Improved Village Placement.");
    public final ConfigBool ivpAutoDetectModdedVillages = b(true, "autoDetectModdedVillages", "Automatically target non-vanilla structures whose path contains village, town, settlement, or hamlet.");
    public final ConfigInt ivpMaxTerrainVariation = i(20, 0, 64, "maxTerrainVariation", "Maximum difference, in blocks, between the lowest and highest sampled height.");
    public final ConfigInt ivpSampleRadius = i(64, 8, 256, "sampleRadius", "Horizontal radius around the structure start position that is sampled.");
    public final ConfigInt ivpSampleStep = i(16, 4, 64, "sampleStep", "Distance in blocks between terrain samples.");
    public final ConfigBool ivpLogFilteredStructures = b(false, "logFilteredStructures", "Write a debug log entry whenever a structure is rejected.");

    public ModConfigSpec.ConfigValue<List<? extends String>> ivpStructurePatterns;
    public ModConfigSpec.ConfigValue<List<? extends String>> ivpExcludedStructurePatterns;

    private static final List<String> DEFAULT_STRUCTURE_PATTERNS = List.of(
            "minecraft:village_*",
            "towns_and_towers:*village*",
            "towns_and_towers:*town*",
            "epic_structures_villages:*village*",
            "nova_structures:*village*",
            "terralith:*village*",
            "ctov:*village*"
    );

    @Override
    public void registerAll(ModConfigSpec.Builder builder) {
        super.registerAll(builder);
        builder.pop();

        builder.push("modpack");
        modpackVersion = builder
                .comment("Current installed version of the modpack")
                .define("modpackVersion", "1.0.0");
        versionCheckUrl = builder
                .comment("Raw GitHub URL to version.json file")
                .define("versionCheckUrl", "https://raw.githubusercontent.com/ripiters/modpack-info/refs/heads/main/version.json");
        builder.pop();

        builder.push("admin");
        allowedAdmins = builder
                .comment("Server admin UUID / username list")
                .defineListAllowEmpty(
                        "allowedAdmins",
                        List.of(),
                        () -> "",
                        obj -> obj instanceof String
                );
        builder.pop();

        builder.push("logging");
        suppressedLogs = builder
                .comment("List of keywords for filtering in the server console")
                .defineListAllowEmpty(
                        "suppressedLogs",
                        List.of(),
                        () -> "",
                        obj -> obj instanceof String
                );
        builder.pop();

        builder.push("chat");
        joinMessage = builder
                .comment("Format for player join message. Use %player% as nickname placeholder.")
                .define("joinMessage", "<#55FF55>Witaj <#FFD700>%player%<#55FF55> na serwerze!");
        builder.pop();

        builder.push("worldgen").push("ivp");
        ivpStructurePatterns = builder
                .comment("Structure ids or glob patterns to filter. '*' matches any number of characters and '?' matches one character.")
                .translation("servercore.config.ivp.structure_patterns")
                .defineListAllowEmpty(
                        "structurePatterns",
                        DEFAULT_STRUCTURE_PATTERNS,
                        () -> "minecraft:village_*",
                        value -> value instanceof String && isValidPattern((String) value)
                );

        ivpExcludedStructurePatterns = builder
                .comment("Structure ids or glob patterns that override the include rules and are never filtered.")
                .translation("servercore.config.ivp.excluded_structure_patterns")
                .defineListAllowEmpty(
                        "excludedStructurePatterns",
                        List.of(),
                        () -> "minecraft:village_desert",
                        value -> value instanceof String && isValidPattern((String) value)
                );
        builder.pop().pop();
    }

    public int getLargeDungeonSpacingValue() {
        return largeDungeonSpacing.get();
    }

    public int getLargeDungeonSeparationValue() {
        return largeDungeonSeparation.get();
    }

    public int getVillageSpacingValue() {
        return villageSpacing.get();
    }

    public int getVillageSeparationValue() {
        return villageSeparation.get();
    }

    public List<GlobPattern> getIvpIncludePatterns() {
        return compilePatterns(ivpStructurePatterns.get());
    }

    public List<GlobPattern> getIvpExcludePatterns() {
        return compilePatterns(ivpExcludedStructurePatterns.get());
    }

    private static boolean isValidPattern(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String test = value.trim();
        if (test.startsWith("#")) {
            test = test.substring(1);
        }

        if (test.isBlank()) {
            return false;
        }

        String resourceLocationCandidate = test
                .replace('*', 'a')
                .replace('?', 'b');

        return ResourceLocation.tryParse(resourceLocationCandidate) != null;
    }

    private static List<GlobPattern> compilePatterns(List<? extends String> values) {
        List<GlobPattern> result = new ArrayList<>();

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                result.add(new GlobPattern(value.trim()));
            }
        }

        return List.copyOf(result);
    }

    @Override
    public String getName() {
        return "common";
    }

    public static final class GlobPattern {

        private final String original;
        private final Pattern pattern;

        public GlobPattern(String original) {
            this.original = original;
            this.pattern = Pattern.compile(
                    toRegex(normalize(original)),
                    Pattern.CASE_INSENSITIVE
            );
        }

        public boolean matches(ResourceLocation id) {
            return pattern.matcher(id.toString()).matches();
        }

        private static String normalize(String value) {
            return value.startsWith("#")
                    ? value.substring(1)
                    : value;
        }

        private static String toRegex(String glob) {
            StringBuilder out = new StringBuilder("^");

            for (int i = 0; i < glob.length(); i++) {
                char c = glob.charAt(i);

                switch (c) {
                    case '*' -> out.append(".*");
                    case '?' -> out.append('.');
                    default -> {
                        if ("\\.^$|()[]{}+".indexOf(c) >= 0) {
                            out.append('\\');
                        }

                        out.append(c);
                    }
                }
            }

            return out.append('$').toString();
        }

        @Override
        public String toString() {
            return original;
        }
    }
}