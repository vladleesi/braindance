// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.data.config.ApiConfig
import dev.vladleesi.braindanceapp.data.models.games.Cover
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.Genre
import dev.vladleesi.braindanceapp.data.models.games.Platform
import dev.vladleesi.braindanceapp.data.models.games.ReleaseDate
import dev.vladleesi.braindanceapp.data.models.games.Screenshot
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.models.games.Video
import dev.vladleesi.braindanceapp.data.models.games.Websites
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import kotlinx.serialization.descriptors.SerialDescriptor
import java.io.File
import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

/** docs/openapi.yaml lists exactly the endpoints and fields the client uses. */
class ApiContractTest {
    private val contract = Contract.parse(File(System.getProperty("apiContractFile")).readLines())

    @Test
    fun pathsMatchClientEndpoints() {
        val endpoints =
            ApiConfig.Endpoints::class.java.declaredFields
                .filter { field -> Modifier.isStatic(field.modifiers) && field.type == String::class.java }
                .map { field -> "/${field.get(null)}" }
        // GamerPowerRemote.giveaway appends the ID to the list path.
        val expected = endpoints + "/${ApiConfig.Endpoints.GIVEAWAYS}/{id}"
        assertEquals(expected.toSortedSet(), contract.paths.toSortedSet())
    }

    @Test
    fun schemasMatchClientModels() {
        val models: Map<String, SerialDescriptor> =
            mapOf(
                "TrendingGameItem" to TrendingGameItem.serializer().descriptor,
                "GameItem" to GameItem.serializer().descriptor,
                "SimilarGame" to SimilarGame.serializer().descriptor,
                "Cover" to Cover.serializer().descriptor,
                "ReleaseDate" to ReleaseDate.serializer().descriptor,
                "Genre" to Genre.serializer().descriptor,
                "Platform" to Platform.serializer().descriptor,
                "Websites" to Websites.serializer().descriptor,
                "Screenshot" to Screenshot.serializer().descriptor,
                "Video" to Video.serializer().descriptor,
                "GiveawayResponse" to GiveawayResponse.serializer().descriptor,
            )
        assertEquals(models.keys.toSortedSet(), contract.schemas.keys.toSortedSet())
        models.forEach { (name, descriptor) ->
            val fields = (0 until descriptor.elementsCount).map(descriptor::getElementName)
            assertEquals(fields.toSortedSet(), contract.schemas.getValue(name).toSortedSet(), name)
        }
    }
}

/** The keys of a block-style YAML document, enough to read its paths and schema properties without a YAML library. */
private class Contract(
    val paths: Set<String>,
    val schemas: Map<String, Set<String>>,
) {
    companion object {
        // A mapping key, optionally quoted, that does not start a list item.
        private val keyLine = Regex("""^( *)(?!- )(['"]?)([^'"\s#][^'"#]*?)\2:(?:\s+(.*))?$""")

        fun parse(lines: List<String>): Contract {
            val stack = ArrayDeque<Pair<Int, String>>()
            val keyPaths = mutableListOf<List<String>>()
            var blockScalarIndent: Int? = null
            for (line in lines) {
                if (line.isBlank() || line.trimStart().startsWith("#")) continue
                val indent = line.length - line.trimStart().length
                // Lines of a multi-line string (`>-`, `|`) belong to the key above them.
                if (blockScalarIndent != null && indent > blockScalarIndent) continue
                blockScalarIndent = null
                val match = keyLine.matchEntire(line) ?: continue
                while (stack.isNotEmpty() && stack.last().first >= indent) stack.removeLast()
                stack.addLast(indent to match.groupValues[3])
                keyPaths += stack.map { (_, key) -> key }
                if (match.groupValues[4].let { value -> value.startsWith(">") || value.startsWith("|") }) {
                    blockScalarIndent = indent
                }
            }
            val paths = keyPaths.filter { it.size == 2 && it[0] == "paths" }.map { it[1] }
            val schemaNames =
                keyPaths.filter { it.size == 3 && it[0] == "components" && it[1] == "schemas" }.map { it[2] }
            val properties =
                keyPaths
                    .filter { it.size == 5 && it[0] == "components" && it[1] == "schemas" && it[3] == "properties" }
                    .groupBy({ it[2] }, { it[4] })
            return Contract(paths.toSet(), schemaNames.associateWith { name -> properties[name].orEmpty().toSet() })
        }
    }
}
