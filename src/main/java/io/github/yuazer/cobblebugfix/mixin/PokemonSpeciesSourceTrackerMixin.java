package io.github.yuazer.cobblebugfix.mixin;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.riding.RidingProperties;
import com.cobblemon.mod.common.api.riding.behaviour.RidingBehaviourSettings;
import com.cobblemon.mod.common.pokemon.Species;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.yuazer.cobblebugfix.tracker.PokemonSourceTracker;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Mixin(PokemonSpecies.class)
public abstract class PokemonSpeciesSourceTrackerMixin {
    private static final List<String> MODEL_VARIATION_DIRECTORIES = Arrays.asList(
            "bedrock/species",
            "bedrock/pokemon/resolvers",
            "bedrock/pokemon/variations"
    );

    @Inject(
            method = "reload(Lnet/minecraft/server/packs/resources/ResourceManager;)V",
            at = @At("TAIL")
    )
    private void cobblebugfix$trackSpeciesAndRidingSource(ResourceManager manager, CallbackInfo ci) {
        PokemonSourceTracker.clearSpeciesTracking();

        Map<ResourceLocation, Resource> resources = collectEffectiveResources(manager, "species");

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation jsonResource = entry.getKey();
            ResourceLocation speciesId = resolveSpeciesIdentifier(jsonResource);
            PokemonSourceTracker.recordSpeciesJsonSource(speciesId, jsonResource, entry.getValue());
        }

        Map<ResourceLocation, Resource> ridingAdditionSources = collectRidingAdditionSources(manager);

        for (Species species : PokemonSpecies.INSTANCE.getSpecies()) {
            RidingProperties riding = species.getStandardForm().getRiding();
            Map<?, RidingBehaviourSettings> behaviours = riding.getBehaviours();
            if (behaviours == null || behaviours.isEmpty()) {
                continue;
            }

            List<String> keys = new ArrayList<>();
            for (RidingBehaviourSettings settings : behaviours.values()) {
                keys.add(settings.getKey().toString());
            }

            PokemonSourceTracker.recordRidingSource(
                    species.getResourceIdentifier(),
                    keys,
                    ridingAdditionSources.get(species.getResourceIdentifier())
            );
        }

        trackModelSources(manager);
    }

    private Map<ResourceLocation, Resource> collectEffectiveResources(ResourceManager manager, String rootPath) {
        Map<ResourceLocation, Resource> out = new LinkedHashMap<>();
        Map<ResourceLocation, List<Resource>> stacks = manager.listResourceStacks(
                rootPath,
                id -> id.getPath().endsWith(".json")
        );
        for (Map.Entry<ResourceLocation, List<Resource>> entry : stacks.entrySet()) {
            List<Resource> stack = entry.getValue();
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            out.put(entry.getKey(), stack.get(stack.size() - 1));
        }
        return out;
    }

    private Map<ResourceLocation, Resource> collectRidingAdditionSources(ResourceManager manager) {
        Map<ResourceLocation, Resource> out = new HashMap<>();
        Map<ResourceLocation, Resource> additions = collectEffectiveResources(manager, "species_additions");
        for (Map.Entry<ResourceLocation, Resource> entry : additions.entrySet()) {
            Resource resource = entry.getValue();
            try (Reader reader = resource.openAsReader()) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                if (!root.has("target") || !root.has("riding")) {
                    continue;
                }
                ResourceLocation targetId = parseTargetSpecies(root.get("target").getAsString());
                if (targetId != null) {
                    out.put(targetId, resource);
                }
            } catch (Exception ignored) {
                // Ignore malformed additions and continue tracking other resources.
            }
        }
        return out;
    }

    private ResourceLocation parseTargetSpecies(String targetText) {
        if (targetText == null || targetText.isBlank()) {
            return null;
        }
        String value = targetText.trim();
        int split = value.indexOf(':');
        String namespace = split >= 0 ? value.substring(0, split) : "cobblemon";
        String path = split >= 0 ? value.substring(split + 1) : value;
        if (path.isBlank()) {
            return null;
        }
        try {
            return new ResourceLocation(namespace, path);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void trackModelSources(ResourceManager manager) {
        PokemonSourceTracker.clearModelTracking();

        Map<ResourceLocation, Resource> variationResources = collectEffectiveClientResources(manager, MODEL_VARIATION_DIRECTORIES);
        for (Map.Entry<ResourceLocation, Resource> entry : variationResources.entrySet()) {
            ResourceLocation jsonResource = entry.getKey();
            Resource resource = entry.getValue();
            ResourceLocation speciesId = resolveVariationSpeciesIdentifier(jsonResource, resource);
            if (speciesId == null || PokemonSpecies.getByIdentifier(speciesId) == null) {
                continue;
            }
            PokemonSourceTracker.recordModelJsonSource(speciesId, jsonResource, resource);
        }

        // Dedicated servers may not expose CLIENT_RESOURCES through this manager.
        // Fall back to scanning loaded mod assets directly.
        scanModelSourcesFromLoadedMods();
    }

    private Map<ResourceLocation, Resource> collectEffectiveClientResources(ResourceManager manager, List<String> directories) {
        Map<ResourceLocation, Resource> out = new LinkedHashMap<>();
        manager.listPacks().forEach(pack -> collectPackClientResources(pack, directories, out));
        return out;
    }

    private void collectPackClientResources(PackResources pack, List<String> directories, Map<ResourceLocation, Resource> out) {
        Set<String> namespaces = pack.getNamespaces(PackType.CLIENT_RESOURCES);
        for (String namespace : namespaces) {
            for (String directory : directories) {
                pack.listResources(PackType.CLIENT_RESOURCES, namespace, directory, (id, supplier) -> {
                    if (!id.getPath().endsWith(".json")) {
                        return;
                    }
                    out.put(id, new Resource(pack, supplier));
                });
            }
        }
    }

    private ResourceLocation resolveVariationSpeciesIdentifier(ResourceLocation jsonResource, Resource resource) {
        try (Reader reader = resource.openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            ResourceLocation parsed = resolveVariationSpeciesIdentifierFromJson(jsonResource, root);
            if (parsed != null) {
                return parsed;
            }
        } catch (Exception ignored) {
            // Fallback below.
        }
        return resolveSpeciesIdentifier(jsonResource);
    }

    private void scanModelSourcesFromLoadedMods() {
        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            for (Path rootPath : mod.getRootPaths()) {
                Path assetsRoot = rootPath.resolve("assets");
                if (!Files.isDirectory(assetsRoot)) {
                    continue;
                }
                try (var namespaceDirs = Files.list(assetsRoot)) {
                    namespaceDirs
                            .filter(Files::isDirectory)
                            .forEach(namespaceDir -> scanNamespaceModelSources(modId, namespaceDir));
                } catch (Exception ignored) {
                    // Ignore unreadable roots and continue scanning the remaining mods.
                }
            }
        }
    }

    private void scanNamespaceModelSources(String modId, Path namespaceDir) {
        String namespace = namespaceDir.getFileName().toString();
        for (String directory : MODEL_VARIATION_DIRECTORIES) {
            Path directoryPath = resolveRelativePath(namespaceDir, directory);
            if (directoryPath == null || !Files.isDirectory(directoryPath)) {
                continue;
            }
            try (var files = Files.walk(directoryPath)) {
                files.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".json"))
                        .forEach(file -> trackModelFileFromMod(modId, namespaceDir, namespace, file));
            } catch (Exception ignored) {
                // Ignore this directory and continue.
            }
        }
    }

    private void trackModelFileFromMod(String modId, Path namespaceDir, String namespace, Path file) {
        try {
            String relativePath = namespaceDir.relativize(file).toString().replace('\\', '/');
            ResourceLocation jsonResource = new ResourceLocation(namespace, relativePath);
            JsonObject root;
            try (Reader reader = Files.newBufferedReader(file)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            ResourceLocation speciesId = resolveVariationSpeciesIdentifierFromJson(jsonResource, root);
            if (speciesId == null || PokemonSpecies.getByIdentifier(speciesId) == null) {
                return;
            }
            PokemonSourceTracker.recordModelJsonSource(speciesId, jsonResource, "mod/" + modId);
        } catch (Exception ignored) {
            // Ignore malformed files and continue.
        }
    }

    private ResourceLocation resolveVariationSpeciesIdentifierFromJson(ResourceLocation jsonResource, JsonObject root) {
        if (root.has("species")) {
            ResourceLocation parsed = parseTargetSpecies(root.get("species").getAsString());
            if (parsed != null) {
                return parsed;
            }
        }
        if (root.has("name")) {
            ResourceLocation parsed = parseTargetSpecies(root.get("name").getAsString());
            if (parsed != null) {
                return parsed;
            }
        }
        return resolveSpeciesIdentifier(jsonResource);
    }

    private Path resolveRelativePath(Path root, String relative) {
        Path current = root;
        for (String segment : relative.split("/")) {
            if (segment.isEmpty()) {
                continue;
            }
            current = current.resolve(segment);
        }
        return current;
    }

    private ResourceLocation resolveSpeciesIdentifier(ResourceLocation jsonResource) {
        String full = jsonResource.toString();
        int separator = full.indexOf(':');
        String namespace = separator >= 0 ? full.substring(0, separator) : "minecraft";
        String path = separator >= 0 ? full.substring(separator + 1) : full;
        String fileName = new File(path).getName();
        String withoutExt = fileName.endsWith(".json")
                ? fileName.substring(0, fileName.length() - ".json".length())
                : fileName;
        return new ResourceLocation(namespace, withoutExt);
    }
}
