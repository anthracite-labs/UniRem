package com.unirem.capability

import com.unirem.core.api.CapabilityInput
import com.unirem.core.api.CapabilityKey
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.NetworkType
import com.unirem.core.api.TvId
import com.unirem.tv.MatrixOverlayRow
import com.unirem.tv.networkTypeOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * The single capability display owner (Story 1.8, AD-4).
 *
 * Builds a [CapabilityInput] per durable TV from the stored record plus the
 * versioned asset rows plus the debug overlay (debug builds only), and
 * exposes one label map per [TvId] as a [StateFlow]. The flow recomputes
 * when the record, firmware, asset, overlay, or probe inputs change. Nothing
 * is stored: no label, alias, or row persists here.
 *
 * V1 scope: declared-absent sets and probe results are empty (no
 * declared-surface capture and no probe capture in this story); the resolver
 * still honors them for later epics.
 */
class CapabilityLabelTracker(
    private val externalScope: CoroutineScope,
    val isDebug: Boolean,
) {

    private val assets = MutableStateFlow(MatrixAsset.empty())
    private val overlays = MutableStateFlow<List<CapabilityInput.Row>>(emptyList())
    private val subjects = MutableStateFlow<Map<String, CapabilitySubject>>(emptyMap())
    private val labelFlows = HashMap<String, StateFlow<Map<CapabilityKey, CapabilityLabel>>>()

    /** Labels keyed by TvId hex. Empty map means no resolvable input yet. */
    val all: StateFlow<Map<String, Map<CapabilityKey, CapabilityLabel>>> = combine(
        assets,
        overlays,
        subjects,
    ) { asset, overlayRows, byId ->
        byId.mapValues { (_, subject) -> resolveFor(subject, asset, overlayRows, isDebug) }
    }.stateIn(externalScope, SharingStarted.Eagerly, emptyMap())

    /** Live labels for one TV. Empty until a subject is set. Cached per TvId. */
    fun labelsFor(tvIdHex: String): StateFlow<Map<CapabilityKey, CapabilityLabel>> =
        labelFlows.getOrPut(tvIdHex) {
            all.map { it[tvIdHex] ?: emptyMap() }
                .stateIn(externalScope, SharingStarted.Eagerly, emptyMap())
        }

    fun setAsset(asset: CapabilityInput.Asset) {
        assets.value = asset
    }

    fun setOverlays(rows: List<CapabilityInput.Row>) {
        overlays.value = rows
    }

    fun setSubject(subject: CapabilitySubject) {
        subjects.value = subjects.value + (subject.tvIdHex to subject)
    }

    fun removeSubject(tvIdHex: String) {
        subjects.value = subjects.value - tvIdHex
        labelFlows.remove(tvIdHex)
    }

    companion object {
        /** The one V1 card key: Keys/Device. */
        val CARD_KEY = CapabilityKey(
            CapabilityInput.Capability.Keys,
            com.unirem.core.api.CapabilityScope.Device,
        )

        /**
         * Pure label computation for one subject. Shared by the live flows
         * and by JVM tests; release builds pass `debug = false` so overlay
         * rows never resolve.
         */
        fun resolveFor(
            subject: CapabilitySubject,
            asset: CapabilityInput.Asset,
            overlayRows: List<CapabilityInput.Row>,
            debug: Boolean,
        ): Map<CapabilityKey, CapabilityLabel> = CapabilityInput(
            keys = MatrixRulesKeysV1,
            assetRows = asset.rows,
            overlayRows = overlayRows,
            declaredAbsent = emptySet(),
            probeResults = emptyMap(),
            model = subject.model,
            firmware = subject.firmware,
            networkType = subject.networkType,
            generation = subject.generation,
            matrixVersion = asset.matrixVersion,
            debug = debug,
        ).resolve()

        private val MatrixRulesKeysV1: Set<CapabilityKey> =
            com.unirem.core.api.MatrixRules.allKeys.toSet()

        /** Maps a stored record to resolver input. Firmware stays byte-exact. */
        fun subjectOf(
            tvId: TvId,
            model: String,
            firmware: String,
            networkTypeWire: String,
            generation: CapabilityInput.Generation = CapabilityInput.Generation.Unknown,
        ): CapabilitySubject = CapabilitySubject(
            tvIdHex = tvId.hex,
            model = model,
            firmware = firmware,
            networkType = networkTypeOf(networkTypeWire),
            generation = generation,
        )
    }
}

/** Minimal resolver input for one durable TV. Firmware is byte-exact. */
data class CapabilitySubject(
    val tvIdHex: String,
    val model: String,
    val firmware: String,
    val networkType: NetworkType,
    val generation: CapabilityInput.Generation = CapabilityInput.Generation.Unknown,
)

/** Maps one overlay row to resolver input; null when enums no longer parse. */
fun MatrixOverlayRow.toCapabilityRow(): CapabilityInput.Row? {
    val capability = try {
        CapabilityInput.Capability.valueOf(capability)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val scope = try {
        com.unirem.core.api.CapabilityScope.valueOf(scope)
    } catch (e: IllegalArgumentException) {
        return null
    }
    val result = try {
        CapabilityInput.Result.valueOf(result)
    } catch (e: IllegalArgumentException) {
        return null
    }
    return CapabilityInput.Row(
        model = model,
        firmware = firmware,
        networkType = networkTypeOf(networkType),
        key = CapabilityKey(capability, scope),
        result = result,
        matrixVersion = matrixVersion,
    )
}
