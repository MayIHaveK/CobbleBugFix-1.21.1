package io.github.yuazer.cobblebugfix.properties

import com.cobblemon.mod.common.api.moves.Moves
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty
import com.cobblemon.mod.common.api.properties.CustomPokemonPropertyType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.pokemon.Pokemon

private const val MOVE_SLOT_COUNT = 4

class ForceMovesProperty(private val moveNames: List<String?>) : CustomPokemonProperty {
    override fun asString(): String = "forcemoves=${moveNames.joinToString(",") { it ?: "null" }}"

    override fun apply(pokemon: Pokemon) {
        val moveSet = pokemon.moveSet
        for (slot in 0 until MOVE_SLOT_COUNT) {
            val moveName = moveNames[slot]
            val move = moveName?.let { Moves.getByName(it)?.create() }
            moveSet.setMove(slot, move)
        }
    }

    override fun apply(pokemonEntity: PokemonEntity) = apply(pokemonEntity.pokemon)

    override fun matches(pokemon: Pokemon): Boolean {
        val currentMoves = pokemon.moveSet.getMoves()
        for (slot in 0 until MOVE_SLOT_COUNT) {
            val expected = moveNames[slot]
            val current = currentMoves.getOrNull(slot)?.template?.name
            if (expected == null) {
                if (current != null) return false
            } else if (!expected.equals(current, ignoreCase = true)) {
                return false
            }
        }
        return true
    }
}

object ForceMovesPropertyType : CustomPokemonPropertyType<ForceMovesProperty> {
    override val keys = setOf("forcemoves")
    override val needsKey = true

    override fun fromString(value: String?): ForceMovesProperty? {
        val raw = value?.trim() ?: return null
        val inputParts = if (raw.isEmpty()) emptyList() else raw.split(",")
        if (inputParts.size > MOVE_SLOT_COUNT) return null

        val moveNamesByLower = Moves.names().associateBy { it.lowercase() }
        val resolved = MutableList<String?>(MOVE_SLOT_COUNT) { null }

        for (i in inputParts.indices) {
            val token = inputParts[i].trim()
            if (token.isEmpty() || token.equals("null", ignoreCase = true)) {
                resolved[i] = null
                continue
            }

            val canonical = moveNamesByLower[token.lowercase()] ?: return null
            resolved[i] = canonical
        }

        return ForceMovesProperty(resolved)
    }

    override fun examples() = listOf("Bite,Memento", "null,Bite,Memento", "Memento,Memento")
}
