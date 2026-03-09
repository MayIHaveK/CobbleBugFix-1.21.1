package io.github.yuazer.cobblebugfix.util;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class PokemonTranslationAliases {
    private static final String LANGUAGE = "zh_cn";
    private static final Object LOCK = new Object();

    private static volatile Map<String, String> aliasToSpecies = Map.of();
    private static volatile List<String> speciesSuggestions = List.of();
    private static volatile int cachedSpeciesCount = -1;

    private PokemonTranslationAliases() {
    }

    public static String rewritePropertiesInput(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String[] sections = input.split(" ");
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i];
            if (section.isBlank()) {
                continue;
            }

            if (i == 0 && !section.contains("=")) {
                sections[i] = rewriteStandaloneSpeciesToken(section);
                continue;
            }

            int assignIndex = section.indexOf('=');
            if (assignIndex <= 0) {
                continue;
            }

            String key = section.substring(0, assignIndex);
            if (!isSpeciesKey(key)) {
                continue;
            }

            String value = section.substring(assignIndex + 1);
            String rewritten = rewriteValue(value);
            if (!value.equals(rewritten)) {
                sections[i] = key + "=" + rewritten;
            }
        }

        return String.join(" ", sections);
    }

    public static boolean isSpeciesKey(String key) {
        return "species".equalsIgnoreCase(key);
    }

    public static List<String> getSpeciesSuggestions() {
        ensureLoaded();
        return speciesSuggestions;
    }

    public static String resolveSpeciesAlias(String input) {
        ensureLoaded();
        return aliasToSpecies.get(normalizeLookup(input));
    }

    private static String rewriteStandaloneSpeciesToken(String token) {
        String rewritten = rewriteValue(token);
        return rewritten != null ? rewritten : token;
    }

    private static String rewriteValue(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String prefix = "";
        String suffix = "";
        String inner = value;
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            prefix = "\"";
            suffix = "\"";
            inner = value.substring(1, value.length() - 1);
        }

        String resolved = resolveSpeciesAlias(inner);
        if (resolved == null) {
            return value;
        }
        return prefix + resolved + suffix;
    }

    private static void ensureLoaded() {
        int speciesCount = PokemonSpecies.INSTANCE.getSpecies().size();
        if (cachedSpeciesCount == speciesCount && !speciesSuggestions.isEmpty()) {
            return;
        }

        synchronized (LOCK) {
            speciesCount = PokemonSpecies.getSpecies().size();
            if (cachedSpeciesCount == speciesCount && !speciesSuggestions.isEmpty()) {
                return;
            }

            Map<String, String> aliases = new LinkedHashMap<>();
            Set<String> suggestions = new LinkedHashSet<>();
            Map<String, JsonObject> languageFiles = new LinkedHashMap<>();

            for (Species species : PokemonSpecies.getSpecies()) {
                String speciesId = asCommandSpeciesId(species);
                suggestions.add(speciesId);

                String namespace = namespaceOf(species);
                JsonObject language = languageFiles.computeIfAbsent(namespace, PokemonTranslationAliases::loadLanguageFile);
                if (language == null) {
                    continue;
                }

                JsonElement translatedName = language.get(translationKey(species));
                if (translatedName == null || !translatedName.isJsonPrimitive()) {
                    continue;
                }

                String alias = translatedName.getAsString().trim();
                if (alias.isEmpty()) {
                    continue;
                }

                suggestions.add(alias);
                aliases.putIfAbsent(normalizeLookup(alias), speciesId);
            }

            aliasToSpecies = Map.copyOf(aliases);
            speciesSuggestions = List.copyOf(suggestions);
            cachedSpeciesCount = speciesCount;
        }
    }

    private static JsonObject loadLanguageFile(String namespace) {
        String path = "assets/" + namespace + "/lang/" + LANGUAGE + ".json";
        try (InputStream stream = PokemonTranslationAliases.class.getClassLoader().getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }

            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return JsonParser.parseReader(reader).getAsJsonObject();
            }
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String translationKey(Species species) {
        return namespaceOf(species)
                + ".species."
                + species.getName().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "")
                + ".name";
    }

    private static String asCommandSpeciesId(Species species) {
        String identifier = species.getResourceIdentifier().toString();
        if (Cobblemon.MODID.equals(namespaceOf(species))) {
            return pathOf(identifier);
        }
        return identifier;
    }

    private static String namespaceOf(Species species) {
        return namespaceOf(species.getResourceIdentifier().toString());
    }

    private static String namespaceOf(String identifier) {
        int separator = identifier.indexOf(':');
        if (separator < 0) {
            return Cobblemon.MODID;
        }
        return identifier.substring(0, separator);
    }

    private static String pathOf(String identifier) {
        int separator = identifier.indexOf(':');
        if (separator < 0 || separator == identifier.length() - 1) {
            return identifier;
        }
        return identifier.substring(separator + 1);
    }

    private static String normalizeLookup(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
