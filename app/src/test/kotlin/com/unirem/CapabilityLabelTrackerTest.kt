package com.unirem

import com.unirem.capability.CapabilityCopy
import com.unirem.capability.CapabilityLabelTracker
import com.unirem.capability.CapabilitySubject
import com.unirem.capability.toCapabilityRow
import com.unirem.core.api.CapabilityInput
import com.unirem.core.api.CapabilityKey
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.CapabilityScope
import com.unirem.core.api.MatrixRules
import com.unirem.core.api.NetworkType
import com.unirem.core.api.TvId
import com.unirem.tv.MatrixOverlayRow
import com.unirem.tv.RoomTvStore
import com.unirem.tv.TvDao
import com.unirem.tv.TvRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Owner, recompute, precedence, copy-mapping, and harness-copy tests for
 * Story 1.8. Pure JVM; no Android import.
 */
class CapabilityLabelTrackerTest {

    private val cardKey = CapabilityLabelTracker.CARD_KEY

    private fun asset(vararg rows: CapabilityInput.Row, version: Int = 1) =
        CapabilityInput.Asset(matrixVersion = version, rows = rows.toList())

    private fun row(
        model: String = "QN90C",
        firmware: String = "T-PTLAKUC-1000.1",
        network: NetworkType = NetworkType.Wifi,
        key: CapabilityKey = cardKey,
        result: CapabilityInput.Result = CapabilityInput.Result.Pass,
        version: Int = 1,
    ) = CapabilityInput.Row(model, firmware, network, key, result, version)

    private fun subject(
        hex: String = "hex-1",
        model: String = "QN90C",
        firmware: String = "T-PTLAKUC-1000.1",
        network: NetworkType = NetworkType.Wifi,
    ) = CapabilitySubject(hex, model, firmware, network)

    @Test
    fun cardKeyIsKeysDevice() {
        assertEquals(CapabilityInput.Capability.Keys, cardKey.capability)
        assertEquals(CapabilityScope.Device, cardKey.scope)
    }

    @Test
    fun resolveForExactPassYieldsFullControlPlate() {
        val labels = CapabilityLabelTracker.resolveFor(
            subject(),
            asset(row()),
            emptyList(),
            debug = false,
        )
        assertEquals(CapabilityLabel.Works, labels[cardKey])
        assertEquals(
            "Full control — confirms keys",
            CapabilityCopy.cardLine(labels[cardKey], "Full control — confirms keys", "Best-effort — can't confirm commands"),
        )
    }

    @Test
    fun resolveForFallbackYieldsBestEffortPlate() {
        val fallback = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Unknown,
            result = CapabilityInput.Result.Partial,
        )
        val labels = CapabilityLabelTracker.resolveFor(
            subject(model = "UNSEEN", firmware = "T-NEW-1"),
            asset(fallback),
            emptyList(),
            debug = false,
        )
        assertEquals(CapabilityLabel.BestEffort, labels[cardKey])
        assertEquals(
            "Best-effort — can't confirm commands",
            CapabilityCopy.cardLine(labels[cardKey], "Full control — confirms keys", "Best-effort — can't confirm commands"),
        )
    }

    @Test
    fun cardLineMapsOnlyWorksToFullControl() {
        for (label in listOf(CapabilityLabel.NotTested, CapabilityLabel.NotSupported, CapabilityLabel.BestEffort, null)) {
            assertEquals("best", CapabilityCopy.cardLine(label, "full", "best"))
        }
        assertEquals("full", CapabilityCopy.cardLine(CapabilityLabel.Works, "full", "best"))
    }

    @Test
    fun resolveForFirmwareChangeRecomputesWithoutStoredLabel() {
        val before = subject(firmware = "T-PTLAKUC-1000.1")
        val after = subject(firmware = "T-PTLAKUC-1000.2")
        val first = CapabilityLabelTracker.resolveFor(before, asset(row()), emptyList(), debug = false)
        val second = CapabilityLabelTracker.resolveFor(after, asset(row()), emptyList(), debug = false)
        assertEquals(CapabilityLabel.Works, first[cardKey])
        assertEquals(CapabilityLabel.NotTested, second[cardKey])
    }

    @Test
    fun resolveForDebugOverlayWinsAndStaleLoses() {
        val fail = row(result = CapabilityInput.Result.Fail)
        val pass = row(result = CapabilityInput.Result.Pass)
        val debugWin = CapabilityLabelTracker.resolveFor(subject(), asset(fail), listOf(pass), debug = true)
        assertEquals(CapabilityLabel.Works, debugWin[cardKey])
        val releaseIgnores = CapabilityLabelTracker.resolveFor(subject(), asset(fail), listOf(pass), debug = false)
        assertEquals(CapabilityLabel.NotSupported, releaseIgnores[cardKey])
        val stale = row(result = CapabilityInput.Result.Pass, version = 2)
        val staleIgnored = CapabilityLabelTracker.resolveFor(
            subject(), asset(fail, version = 1), listOf(stale), debug = true,
        )
        assertEquals(CapabilityLabel.NotSupported, staleIgnored[cardKey])
    }

    @Test
    fun subjectOfKeepsFirmwareByteExact() {
        val tvId = TvId("abcdef1234567890")
        val out = CapabilityLabelTracker.subjectOf(tvId, "QN90C ", "T-PTLAKUC-1000.1 ", "wifi")
        assertEquals("QN90C ", out.model)
        assertEquals("T-PTLAKUC-1000.1 ", out.firmware)
        assertEquals(NetworkType.Wifi, out.networkType)
        assertEquals("abcdef1234567890", out.tvIdHex)
    }

    @Test
    fun overlayRowMappingRoundTripsAndRejectsUnknownEnums() {
        val valid = MatrixOverlayRow(
            overlayKey = "k",
            model = "M",
            firmware = "F",
            networkType = "wifi",
            capability = "Keys",
            scope = "Device",
            result = "Pass",
            matrixVersion = 1,
            observedAtEpochSeconds = 1L,
        )
        val mapped = valid.toCapabilityRow()!!
        assertEquals(cardKey, mapped.key)
        assertEquals(CapabilityInput.Result.Pass, mapped.result)
        val bad = valid.copy(result = "Observed")
        assertNull(bad.toCapabilityRow())
        assertNull(valid.copy(capability = "Remote").toCapabilityRow())
        assertNull(valid.copy(scope = "Everywhere").toCapabilityRow())
    }

    @Test
    fun rowJsonCarriesMatrixFieldsOnly() {
        val json = CapabilityCopy.rowJson(row())
        assertEquals(
            "{\n" +
                "  \"model\": \"QN90C\",\n" +
                "  \"firmware\": \"T-PTLAKUC-1000.1\",\n" +
                "  \"networkType\": \"Wifi\",\n" +
                "  \"capability\": \"Keys\",\n" +
                "  \"scope\": \"Device\",\n" +
                "  \"result\": \"Pass\",\n" +
                "  \"matrixVersion\": 1\n" +
                "}",
            json,
        )
        assertTrue(!json.contains("note"))
    }

    @Test
    fun rowJsonEscapesQuotesAndBackslashes() {
        val json = CapabilityCopy.rowJson(row(model = "Q\"N", firmware = "T\\1"))
        assertTrue(json.contains("Q\\\"N"))
        assertTrue(json.contains("T\\\\1"))
        MatrixRules.parseAsset("{\"matrixVersion\": 7, \"rows\": [$json]}")
    }

    @Test
    fun trackerFlowRecomputesPerTvId(): Unit = runBlocking {
        val tracker = CapabilityLabelTracker(CoroutineScope(Dispatchers.Unconfined), isDebug = false)
        tracker.setAsset(asset(row()))
        tracker.setSubject(subject(hex = "a", firmware = "T-PTLAKUC-1000.1"))
        tracker.setSubject(subject(hex = "b", firmware = "T-OTHER-2"))
        val all = tracker.all.value
        assertEquals(CapabilityLabel.Works, all["a"]?.get(cardKey))
        assertEquals(CapabilityLabel.NotTested, all["b"]?.get(cardKey))
        tracker.removeSubject("b")
        assertTrue(tracker.all.value["b"] == null)
    }

    @Test
    fun recordFirmwareChangeFlipsLabelAndForgetClearsIt(): Unit = runBlocking {
        val dao = FakeTvDao()
        val store = RoomTvStore.forTests(dao)
        dao.upsert(
            TvRecord(
                tvId = "hexF",
                identityKind = "device-id",
                identityValue = "id",
                name = "TV",
                modelName = "",
                model = "QN90C",
                firmware = "T-PTLAKUC-1000.1",
                wifiMac = "",
                macProvenance = "",
                networkType = "wifi",
                networkInterface = "",
                alias = "hexF",
                pairedAtEpochSeconds = 1L,
            ),
        )
        val tracker = CapabilityLabelTracker(CoroutineScope(Dispatchers.Unconfined), isDebug = false)
        tracker.setAsset(asset(row()))
        suspend fun republish() {
            val fresh = store.byId(TvId("hexF"))!!
            tracker.setSubject(
                CapabilityLabelTracker.subjectOf(
                    TvId("hexF"), fresh.model, fresh.firmware, fresh.networkType,
                ),
            )
        }
        republish()
        assertEquals(CapabilityLabel.Works, tracker.labelsFor("hexF").value[cardKey])
        // Firmware change, as LaunchedEffect(records) republishes the subject:
        // the old exact row no longer matches and no label is stored.
        val stored = store.byId(TvId("hexF"))!!
        dao.upsert(stored.copy(firmware = "T-PTLAKUC-1000.2"))
        republish()
        assertEquals(CapabilityLabel.NotTested, tracker.labelsFor("hexF").value[cardKey])
        // Forget, as MainActivity runForget does:
        tracker.removeSubject("hexF")
        assertTrue(tracker.labelsFor("hexF").value.isEmpty())
    }

    /** In-memory TV DAO so the record pipeline runs on the JVM. */
    class FakeTvDao : TvDao {
        private val rows = LinkedHashMap<String, TvRecord>()
        private val flow = MutableStateFlow<List<TvRecord>>(emptyList())

        override fun observeAll(): Flow<List<TvRecord>> = flow

        override suspend fun all(): List<TvRecord> = rows.values.toList()

        override suspend fun byId(tvId: String): TvRecord? = rows[tvId]

        override suspend fun count(): Int = rows.size

        override suspend fun upsert(record: TvRecord) {
            rows[record.tvId] = record
            flow.value = rows.values.toList()
        }

        override suspend fun deleteById(tvId: String): Int {
            val removed = rows.remove(tvId) != null
            flow.value = rows.values.toList()
            return if (removed) 1 else 0
        }
    }
}
