package com.unirem.core.api

/**
 * Versioned capability matrix (Story 1.8, AD-4/AD-5).
 *
 * A capability is earned per [CapabilityKey] — one [CapabilityInput.Capability]
 * plus one [CapabilityScope] — never assumed from a model table. Matching is
 * byte-exact on model and firmware strings: no trim, no case-fold, no
 * normalization. The only fallback is the literal `*unknown*` asset row, which
 * matches any model/firmware for its key when no exact row matches.
 *
 * Resolution precedence per key (first match wins):
 * 1. declared-absent capability → [CapabilityLabel.NotSupported] (all scopes).
 * 2. probe hard-fail for this key → [CapabilityLabel.NotSupported].
 * 3. matrix Fail for this key → [CapabilityLabel.NotSupported]; a probe Pass
 *    never promotes a Fail.
 * 4. H/J 2014-15 generation → [CapabilityLabel.BestEffort] (never higher).
 * 5. matrix Pass → [CapabilityLabel.Works].
 * 6. matrix Partial/Ambiguous, or a probe Pass → [CapabilityLabel.BestEffort].
 * 7. otherwise → [CapabilityLabel.NotTested].
 *
 * The function is total and pure: one label per requested key, JVM-only, no
 * Android import, no stored label, no UI copy. UI copy lives in `:app`.
 */
enum class CapabilityScope {
    Device,
    NativeField,
    AppGrid,
    PointerBrowser,
    PointerAppGrid,
    FormatJpeg,
    FormatMp3,
    FormatH264Aac,
    FormatHevc,
    Subtitles,
    Seek,
}

/** Earned display label per key. Epic 2 details read all four; V1 cards map. */
enum class CapabilityLabel {
    Works,
    NotTested,
    NotSupported,
    BestEffort,
}

/**
 * One capability plus one scope. No free-string scope: every matrix row,
 * probe result, and resolver input shares this key type.
 */
data class CapabilityKey(
    val capability: CapabilityInput.Capability,
    val scope: CapabilityScope,
)

/**
 * Resolver input tuple. Release and debug share it; only the row source
 * differs (asset only in release, asset plus same-version overlay in debug).
 */
data class CapabilityInput(
    val keys: Set<CapabilityKey>,
    val assetRows: List<Row>,
    val overlayRows: List<Row> = emptyList(),
    val declaredAbsent: Set<Capability> = emptySet(),
    val probeResults: Map<CapabilityKey, Result> = emptyMap(),
    val model: String,
    val firmware: String,
    val networkType: NetworkType,
    val generation: Generation = Generation.Unknown,
    val matrixVersion: Int,
    val debug: Boolean = false,
) {
    /** Closed capability vocabulary. */
    enum class Capability {
        Keys,
        Text,
        Pointer,
        Wake,
        Cast,
        Mirror,
        Voice,
    }

    /** Observed matrix result vocabulary. Socket writes never count. */
    enum class Result {
        Pass,
        Partial,
        Ambiguous,
        Fail,
        NotTested,
    }

    /** TV generation. H/J 2014-15 never exceeds BestEffort. */
    enum class Generation {
        Tizen2016Plus,
        HJ2014_15,
        Unknown,
    }

    /**
     * One matrix row: exact model/firmware/network/key plus the observed
     * result, all pinned to one [matrixVersion]. Overlay rows carry the same
     * shape; staleness is decided by [matrixVersion] at resolve time.
     */
    data class Row(
        val model: String,
        val firmware: String,
        val networkType: NetworkType,
        val key: CapabilityKey,
        val result: Result,
        val matrixVersion: Int,
    )

    /** Versioned asset: top-level version plus rows pinned to it. */
    data class Asset(
        val matrixVersion: Int,
        val rows: List<Row>,
    )

    /**
     * Total pure resolver. Returns exactly one label per requested key.
     * Byte-exact matching; overlay wins only in debug with the same version.
     */
    fun resolve(): Map<CapabilityKey, CapabilityLabel> {
        val winners = effectiveResults()
        return keys.associateWith { key -> labelFor(key, winners[key]) }
    }

    private fun effectiveResults(): Map<CapabilityKey, Result?> {
        val byExactAsset = assetRows.filter { it.matches(model, firmware, networkType) }
            .associate { it.key to it.result }
        val fallback = assetRows.filter { it.isFallback() }
            .associate { it.key to it.result }
        val winners = HashMap<CapabilityKey, Result?>()
        for (key in keys) {
            // Debug overlays need a valid asset version: a corrupt asset
            // (matrixVersion 0) can never earn labels through an overlay.
            val overlay = if (debug && matrixVersion > 0) {
                overlayRows.firstOrNull {
                    it.matrixVersion == matrixVersion && it.matches(model, firmware, networkType) && it.key == key
                }?.result
            } else {
                null
            }
            winners[key] = overlay ?: byExactAsset[key] ?: fallback[key]
        }
        return winners
    }

    private fun labelFor(key: CapabilityKey, matrix: Result?): CapabilityLabel {
        if (key.capability in declaredAbsent) return CapabilityLabel.NotSupported
        if (probeResults[key] == Result.Fail) return CapabilityLabel.NotSupported
        if (matrix == Result.Fail) return CapabilityLabel.NotSupported
        if (generation == Generation.HJ2014_15) return CapabilityLabel.BestEffort
        return when (matrix) {
            Result.Pass -> CapabilityLabel.Works
            Result.Partial, Result.Ambiguous -> CapabilityLabel.BestEffort
            else -> if (probeResults[key] == Result.Pass) {
                CapabilityLabel.BestEffort
            } else {
                CapabilityLabel.NotTested
            }
        }
    }

    /** Exact 4-tuple match. No trim, fold, or normalization. */
    private fun Row.matches(
        model: String,
        firmware: String,
        networkType: NetworkType,
    ): Boolean = this.model == model &&
        this.firmware == firmware &&
        this.networkType == networkType

    /**
     * Literal fallback row: model `*`, firmware `*unknown*`. The fallback
     * matches any model, firmware, and transport for its key when no exact
     * row matches; it never matches a known firmware to a different row.
     * Per the spec the one required fallback is `Unknown`-networked.
     */
    private fun Row.isFallback(): Boolean =
        model == MatrixRules.FALLBACK_MODEL && firmware == MatrixRules.FALLBACK_FIRMWARE
}

/**
 * Schema rules for the versioned matrix asset: minimal JSON parsing,
 * structural validation, and the allowed capability/scope pairs.
 *
 * All parsing is hand-rolled pure Kotlin (no client dependency) so `:core`
 * stays JVM-only and the same code serves release load and build checks.
 */
object MatrixRules {

    const val FALLBACK_MODEL = "*"
    const val FALLBACK_FIRMWARE = "*unknown*"

    /** The only pairs a row may claim. Anything else is an unknown pair. */
    val allowedScopes: Map<CapabilityInput.Capability, Set<CapabilityScope>> = mapOf(
        CapabilityInput.Capability.Keys to setOf(CapabilityScope.Device),
        CapabilityInput.Capability.Text to setOf(CapabilityScope.NativeField, CapabilityScope.AppGrid),
        CapabilityInput.Capability.Pointer to setOf(CapabilityScope.PointerBrowser, CapabilityScope.PointerAppGrid),
        CapabilityInput.Capability.Wake to setOf(CapabilityScope.Device),
        CapabilityInput.Capability.Cast to setOf(
            CapabilityScope.Device,
            CapabilityScope.FormatJpeg,
            CapabilityScope.FormatMp3,
            CapabilityScope.FormatH264Aac,
            CapabilityScope.FormatHevc,
            CapabilityScope.Subtitles,
            CapabilityScope.Seek,
        ),
        CapabilityInput.Capability.Mirror to setOf(CapabilityScope.Device),
        CapabilityInput.Capability.Voice to setOf(CapabilityScope.Device),
    )

    /** All valid keys, for harnesses that list per-key state. */
    val allKeys: List<CapabilityKey> = allowedScopes.flatMap { (capability, scopes) ->
        scopes.map { scope -> CapabilityKey(capability, scope) }
    }.sortedWith(compareBy({ it.capability.name }, { it.scope.name }))

    /**
     * Parses the asset JSON. Throws [MatrixFormatException] on malformed
     * input (bad JSON, missing fields, unknown enum names, blank strings).
     * Messages name structure only, never values.
     */
    fun parseAsset(json: String): CapabilityInput.Asset {
        val value = MatrixJson.parse(json)
        val root = (value as? MatrixJson.Value.Obj)?.fields
            ?: throw MatrixFormatException("asset root must be an object")
        val version = (root["matrixVersion"] as? MatrixJson.Value.Num)?.value?.toInt()
            ?: throw MatrixFormatException("asset misses integer matrixVersion")
        if (version <= 0) throw MatrixFormatException("asset matrixVersion must be positive")
        val rawRows = (root["rows"] as? MatrixJson.Value.Arr)?.values
            ?: throw MatrixFormatException("asset misses rows array")
        if (rawRows.isEmpty()) throw MatrixFormatException("asset rows must not be empty")
        val rows = rawRows.mapIndexed { index, raw -> parseRow(raw, index) }
        return CapabilityInput.Asset(matrixVersion = version, rows = rows)
    }

    private fun parseRow(raw: MatrixJson.Value, index: Int): CapabilityInput.Row {
        val obj = (raw as? MatrixJson.Value.Obj)?.fields
            ?: throw MatrixFormatException("row $index must be an object")
        fun text(field: String): String {
            val value = (obj[field] as? MatrixJson.Value.Str)?.value
                ?: throw MatrixFormatException("row $index misses string $field")
            if (value.isEmpty()) throw MatrixFormatException("row $index has blank $field")
            return value
        }
        val model = text("model")
        val firmware = text("firmware")
        if (model.isBlank() || firmware.isBlank()) {
            throw MatrixFormatException("row $index has blank identity")
        }
        val network = parseEnum<NetworkType>(text("networkType"), "row $index")
        val capability = parseEnum<CapabilityInput.Capability>(text("capability"), "row $index")
        val scope = parseEnum<CapabilityScope>(text("scope"), "row $index")
        val result = parseEnum<CapabilityInput.Result>(text("result"), "row $index")
        val rowVersion = (obj["matrixVersion"] as? MatrixJson.Value.Num)?.value?.toInt()
            ?: throw MatrixFormatException("row $index misses integer matrixVersion")
        return CapabilityInput.Row(
            model = model,
            firmware = firmware,
            networkType = network,
            key = CapabilityKey(capability, scope),
            result = result,
            matrixVersion = rowVersion,
        )
    }

    private inline fun <reified T : Enum<T>> parseEnum(raw: String, where: String): T {
        return enumValues<T>().firstOrNull { it.name == raw }
            ?: throw MatrixFormatException("$where has unknown value")
    }

    /**
     * Structural validation of a parsed asset. Returns one message per
     * violation (empty means valid). Checks: uniform row versions, duplicate
     * exact keys, the required Keys/Device Partial fallback, allowed
     * capability/scope pairs, wildcard discipline, and URL-like values that
     * never belong in an identity field.
     */
    fun validate(asset: CapabilityInput.Asset): List<String> {
        val errors = mutableListOf<String>()
        asset.rows.forEachIndexed { index, row ->
            if (row.matrixVersion != asset.matrixVersion) {
                errors.add("row $index pins a different matrixVersion")
            }
        }
        val seen = HashSet<String>()
        asset.rows.forEachIndexed { index, row ->
            val key = listOf(row.model, row.firmware, row.networkType.name, row.key.capability.name, row.key.scope.name)
                .joinToString("\u0001")
            if (!seen.add(key)) errors.add("row $index duplicates an exact key")
        }
        val hasFallback = asset.rows.any { row ->
            row.model == FALLBACK_MODEL &&
                row.firmware == FALLBACK_FIRMWARE &&
                row.networkType == NetworkType.Unknown &&
                row.key == CapabilityKey(CapabilityInput.Capability.Keys, CapabilityScope.Device) &&
                row.result == CapabilityInput.Result.Partial
        }
        if (!hasFallback) errors.add("asset misses the Keys/Device Partial fallback")
        asset.rows.forEachIndexed { index, row ->
            if (allowedScopes[row.key.capability]?.contains(row.key.scope) != true) {
                errors.add("row $index pairs an unknown capability with scope")
            }
            if ((row.model == FALLBACK_MODEL) != (row.firmware == FALLBACK_FIRMWARE)) {
                errors.add("row $index mixes a wildcard with an exact identity")
            }
            if ((row.model == FALLBACK_MODEL || row.firmware == FALLBACK_FIRMWARE) &&
                row.networkType != NetworkType.Unknown
            ) {
                errors.add("row $index pins a wildcard to an exact transport")
            }
            if (row.model.contains("://") || row.firmware.contains("://")) {
                errors.add("row $index carries a URL-like identity")
            }
        }
        return errors
    }
}

/** Malformed matrix asset. Carries structure names only, never values. */
class MatrixFormatException(message: String) : Exception(message)

/** Minimal JSON reader for the fixed asset shape. Internal implementation. */
internal object MatrixJson {

    sealed interface Value {
        data class Str(val value: String) : Value
        data class Num(val value: Long) : Value
        data class Bool(val value: Boolean) : Value
        data object Null : Value
        data class Arr(val values: List<Value>) : Value
        data class Obj(val fields: Map<String, Value>) : Value {
            operator fun get(key: String): Value? = fields[key]
        }
    }

    fun parse(text: String): Value {
        val reader = Reader(text)
        reader.skipGaps()
        val value = reader.readValue()
        reader.skipGaps()
        if (!reader.atEnd()) throw MatrixFormatException("asset has trailing content")
        return value
    }

    private class Reader(val text: String) {
        var pos: Int = 0

        fun atEnd(): Boolean = pos >= text.length

        fun skipGaps() {
            while (!atEnd() && text[pos].isWhitespace()) pos++
        }

        fun readValue(): Value {
            if (atEnd()) throw MatrixFormatException("asset ends mid-value")
            return when (text[pos]) {
                '{' -> readObject()
                '[' -> readArray()
                '"' -> Value.Str(readString())
                't' -> readLiteral("true", Value.Bool(true))
                'f' -> readLiteral("false", Value.Bool(false))
                'n' -> readLiteral("null", Value.Null)
                '-', in '0'..'9' -> readNumber()
                else -> throw MatrixFormatException("asset has an unexpected token")
            }
        }

        private fun readObject(): Value {
            pos++
            val fields = LinkedHashMap<String, Value>()
            skipGaps()
            if (!atEnd() && text[pos] == '}') {
                pos++
                return Value.Obj(fields)
            }
            while (true) {
                skipGaps()
                if (atEnd() || text[pos] != '"') throw MatrixFormatException("asset object needs string keys")
                val key = readString()
                if (key in fields) throw MatrixFormatException("asset object repeats a key")
                skipGaps()
                if (atEnd() || text[pos] != ':') throw MatrixFormatException("asset object needs a colon")
                pos++
                skipGaps()
                fields[key] = readValue()
                skipGaps()
                if (atEnd()) throw MatrixFormatException("asset object ends mid-row")
                when (text[pos]) {
                    ',' -> pos++
                    '}' -> {
                        pos++
                        return Value.Obj(fields)
                    }
                    else -> throw MatrixFormatException("asset object needs a separator")
                }
            }
        }

        private fun readArray(): Value {
            pos++
            val values = ArrayList<Value>()
            skipGaps()
            if (!atEnd() && text[pos] == ']') {
                pos++
                return Value.Arr(values)
            }
            while (true) {
                skipGaps()
                values.add(readValue())
                skipGaps()
                if (atEnd()) throw MatrixFormatException("asset array ends mid-row")
                when (text[pos]) {
                    ',' -> pos++
                    ']' -> {
                        pos++
                        return Value.Arr(values)
                    }
                    else -> throw MatrixFormatException("asset array needs a separator")
                }
            }
        }

        private fun readString(): String {
            pos++
            val out = StringBuilder()
            while (true) {
                if (atEnd()) throw MatrixFormatException("asset string never ends")
                val c = text[pos++]
                when (c) {
                    '"' -> return out.toString()
                    '\\' -> {
                        if (atEnd()) throw MatrixFormatException("asset escape never ends")
                        when (val e = text[pos++]) {
                            '"', '\\', '/' -> out.append(e)
                            'b' -> out.append('\b')
                            'f' -> out.append('\u000C')
                            'n' -> out.append('\n')
                            'r' -> out.append('\r')
                            't' -> out.append('\t')
                            'u' -> {
                                if (pos + 4 > text.length) throw MatrixFormatException("asset escape never ends")
                                val hex = text.substring(pos, pos + 4)
                                val code = hex.toIntOrNull(16)
                                    ?: throw MatrixFormatException("asset has a bad escape")
                                out.append(code.toChar())
                                pos += 4
                            }
                            else -> throw MatrixFormatException("asset has a bad escape")
                        }
                    }
                    else -> out.append(c)
                }
            }
        }

        private fun readLiteral(word: String, value: Value): Value {
            if (!text.startsWith(word, pos)) throw MatrixFormatException("asset has an unexpected token")
            pos += word.length
            return value
        }

        private fun readNumber(): Value {
            val start = pos
            if (!atEnd() && text[pos] == '-') pos++
            while (!atEnd() && text[pos].isDigit()) pos++
            val raw = text.substring(start, pos)
            val parsed = raw.toLongOrNull() ?: throw MatrixFormatException("asset has a bad number")
            return Value.Num(parsed)
        }
    }
}
