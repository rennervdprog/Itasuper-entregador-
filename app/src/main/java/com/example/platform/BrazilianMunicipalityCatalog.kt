package com.example.platform

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer
import java.util.Locale

/** Município válido exibido ao motoboy; o banco recebe somente [name] pela RPC existente. */
data class BrazilianMunicipality(
    val name: String,
    val state: String,
    val normalizedName: String = normalizeMunicipality(name),
    val normalizedDisplayName: String = normalizeMunicipality("$name $state")
) {
    val displayName: String get() = "$name — $state"
}

/** Catálogo local baseado na relação oficial de municípios do IBGE, carregado sob demanda. */
object BrazilianMunicipalityCatalog {
    private const val ASSET_NAME = "br_municipalities.json"
    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var cachedMunicipalities: List<BrazilianMunicipality>? = null

    suspend fun load(context: Context): List<BrazilianMunicipality> = cachedMunicipalities ?: withContext(Dispatchers.IO) {
        cachedMunicipalities ?: context.applicationContext.assets.open(ASSET_NAME)
            .bufferedReader()
            .use { reader ->
                json.decodeFromString<MunicipalityCatalogPayload>(reader.readText())
                    .municipalities
                    .map { BrazilianMunicipality(it.name, it.state) }
                    .also { cachedMunicipalities = it }
            }
    }

    /**
     * Retorna sugestões em ordem de relevância sem reordenar o catálogo inteiro.
     * Os campos normalizados são calculados uma única vez no carregamento do catálogo.
     */
    fun search(
        municipalities: List<BrazilianMunicipality>,
        query: String,
        limit: Int = 12
    ): List<BrazilianMunicipality> {
        val normalizedQuery = normalizeMunicipality(query)
        if (normalizedQuery.length < 2) return emptyList()

        val exact = municipalities.firstOrNull { it.normalizedName == normalizedQuery }
        val startsWith = municipalities.asSequence()
            .filter { it.normalizedName.startsWith(normalizedQuery) && it != exact }
            .take(limit)
            .toList()

        val remaining = (limit - startsWith.size - if (exact != null) 1 else 0).coerceAtLeast(0)
        val contains = if (remaining == 0) {
            emptyList()
        } else {
            municipalities.asSequence()
                .filter {
                    it != exact &&
                        !it.normalizedName.startsWith(normalizedQuery) &&
                        (it.normalizedName.contains(normalizedQuery) || it.normalizedDisplayName.contains(normalizedQuery))
                }
                .take(remaining)
                .toList()
        }

        return buildList {
            exact?.let(::add)
            addAll(startsWith)
            addAll(contains)
        }
    }

    fun findByName(
        municipalities: List<BrazilianMunicipality>,
        city: String
    ): BrazilianMunicipality? {
        val normalizedCity = normalizeMunicipality(city)
        if (normalizedCity.isBlank()) return null
        return municipalities.firstOrNull { it.normalizedName == normalizedCity }
    }

    @Serializable
    private data class MunicipalityCatalogPayload(
        val municipalities: List<MunicipalityPayload>
    )

    @Serializable
    private data class MunicipalityPayload(
        val name: String,
        val state: String
    )
}

private fun normalizeMunicipality(value: String): String = Normalizer
    .normalize(value.trim(), Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .lowercase(Locale.ROOT)
