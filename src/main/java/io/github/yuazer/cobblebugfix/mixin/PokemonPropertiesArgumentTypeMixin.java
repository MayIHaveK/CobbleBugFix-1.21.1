package io.github.yuazer.cobblebugfix.mixin;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.cobblemon.mod.common.pokemon.properties.PropertiesCompletionProvider;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.yuazer.cobblebugfix.util.PokemonTranslationAliases;
import net.minecraft.commands.SharedSuggestionProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mixin(value = PokemonPropertiesArgumentType.class, remap = false)
public abstract class PokemonPropertiesArgumentTypeMixin {

    @Inject(
            method = "parse",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void cobblebugfix$rewriteTranslatedSpecies(
            StringReader reader,
            CallbackInfoReturnable<PokemonProperties> cir
    ) {
        String properties = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        cir.setReturnValue(PokemonProperties.Companion.parse(PokemonTranslationAliases.rewritePropertiesInput(properties)));
    }

    @Inject(
            method = "listSuggestions",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private <S> void cobblebugfix$suggestTranslatedSpecies(
            CommandContext<S> context,
            SuggestionsBuilder builder,
            CallbackInfoReturnable<CompletableFuture<Suggestions>> cir
    ) {
        String[] sections = builder.getRemainingLowerCase().split(" ");
        if (sections.length == 0) {
            cir.setReturnValue(SharedSuggestionProvider.suggest(speciesAndPropertySuggestions(), builder));
            return;
        }

        String currentSection = sections[sections.length - 1];
        int assignIndex = currentSection.indexOf('=');
        if (assignIndex >= 0) {
            String propertyKey = currentSection.substring(0, assignIndex);
            if (PokemonTranslationAliases.isSpeciesKey(propertyKey)) {
                String currentValue = currentSection.substring(assignIndex + 1);
                cir.setReturnValue(suggestSpeciesValues(currentValue, builder));
            }
            return;
        }

        if (sections.length >= 2) {
            Set<String> usedKeys = new LinkedHashSet<>();
            for (String section : sections) {
                int keyAssignIndex = section.indexOf('=');
                if (keyAssignIndex > 0) {
                    usedKeys.add(section.substring(0, keyAssignIndex));
                }
            }
            cir.setReturnValue(PropertiesCompletionProvider.INSTANCE.suggestKeys(currentSection, usedKeys, builder));
            return;
        }

        cir.setReturnValue(SharedSuggestionProvider.suggest(speciesAndPropertySuggestions(), builder));
    }

    private static Iterable<String> speciesAndPropertySuggestions() {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>(PokemonTranslationAliases.getSpeciesSuggestions());
        suggestions.addAll(PropertiesCompletionProvider.INSTANCE.keys());
        return suggestions;
    }

    private static CompletableFuture<Suggestions> suggestSpeciesValues(String currentValue, SuggestionsBuilder builder) {
        String normalizedValue = currentValue.toLowerCase(Locale.ROOT);
        for (String suggestion : PokemonTranslationAliases.getSpeciesSuggestions()) {
            if (!suggestion.toLowerCase(Locale.ROOT).startsWith(normalizedValue)) {
                continue;
            }
            String suffix = suggestion.substring(currentValue.length());
            builder.suggest(builder.getRemaining() + suffix);
        }
        return builder.buildFuture();
    }
}
