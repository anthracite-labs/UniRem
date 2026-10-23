package com.unirem.core

import com.unirem.core.api.CapabilityInput
import com.unirem.core.api.CapabilityKey
import com.unirem.core.api.CapabilityLabel
import com.unirem.core.api.CapabilityScope
import com.unirem.core.api.MatrixFormatException
import com.unirem.core.api.MatrixRules
import com.unirem.core.api.NetworkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Resolver and schema tests for Story 1.8. Pure JVM; no Android import.
 */
class CapabilityMatrixTest {

    private val keysDevice = CapabilityKey(CapabilityInput.Capability.Keys, CapabilityScope.Device)

    private fun row(
        model: String = "QN90C",
        firmware: String = "T-PTLAKUC-1000.1",
        network: NetworkType = NetworkType.Wifi,
        key: CapabilityKey = keysDevice,
        result: CapabilityInput.Result = CapabilityInput.Result.Pass,
        version: Int = 1,
    ) = CapabilityInput.Row(model, firmware, network, key, result, version)

    private fun input(
        keys: Set<CapabilityKey> = setOf(keysDevice),
        assetRows: List<CapabilityInput.Row> = emptyList(),
        overlayRows: List<CapabilityInput.Row> = emptyList(),
        declaredAbsent: Set<CapabilityInput.Capability> = emptySet(),
        probeResults: Map<CapabilityKey, CapabilityInput.Result> = emptyMap(),
        model: String = "QN90C",
        firmware: String = "T-PTLAKUC-1000.1",
        network: NetworkType = NetworkType.Wifi,
        generation: CapabilityInput.Generation = CapabilityInput.Generation.Tizen2016Plus,
        version: Int = 1,
        debug: Boolean = false,
    ) = CapabilityInput(
        keys = keys,
        assetRows = assetRows,
        overlayRows = overlayRows,
        declaredAbsent = declaredAbsent,
        probeResults = probeResults,
        model = model,
        firmware = firmware,
        networkType = network,
        generation = generation,
        matrixVersion = version,
        debug = debug,
    )

    @Test
    fun exactPassResolvesWorks() {
        val out = input(assetRows = listOf(row(result = CapabilityInput.Result.Pass))).resolve()
        assertEquals(CapabilityLabel.Works, out[keysDevice])
    }

    @Test
    fun byteExactMatchingRejectsCaseWhitespaceAndLocaleVariants() {
        val asset = listOf(row(model = "QN90C", firmware = "T-PTLAKUC-1000.1"))
        assertEquals(
            CapabilityLabel.NotTested,
            input(assetRows = asset, model = "qn90c").resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.NotTested,
            input(assetRows = asset, model = "QN90C ").resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.NotTested,
            input(assetRows = asset, firmware = "t-ptlakuc-1000.1").resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.NotTested,
            input(assetRows = asset, firmware = "T-PTLAKUC-1000.1\n").resolve()[keysDevice],
        )
    }

    @Test
    fun fallbackOnlySeedResolvesBestEffort() {
        val fallback = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Unknown,
            result = CapabilityInput.Result.Partial,
        )
        val out = input(assetRows = listOf(fallback), model = "NEWMODEL", firmware = "T-NEW-9.9").resolve()
        assertEquals(CapabilityLabel.BestEffort, out[keysDevice])
    }

    @Test
    fun networkMismatchFallsBack() {
        val wifi = row(network = NetworkType.Wifi, result = CapabilityInput.Result.Pass)
        val fallback = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Unknown,
            result = CapabilityInput.Result.Partial,
        )
        val out = input(assetRows = listOf(wifi, fallback), network = NetworkType.Ethernet).resolve()
        assertEquals(CapabilityLabel.BestEffort, out[keysDevice])
    }

    @Test
    fun knownFirmwareWithoutRowIsNotTested() {
        val other = row(model = "OTHER", firmware = "T-OTHER-1")
        val out = input(assetRows = listOf(other)).resolve()
        assertEquals(CapabilityLabel.NotTested, out[keysDevice])
    }

    @Test
    fun partialAndAmbiguousResolveBestEffort() {
        assertEquals(
            CapabilityLabel.BestEffort,
            input(assetRows = listOf(row(result = CapabilityInput.Result.Partial))).resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.BestEffort,
            input(assetRows = listOf(row(result = CapabilityInput.Result.Ambiguous))).resolve()[keysDevice],
        )
    }

    @Test
    fun matrixFailResolvesNotSupportedAndProbePassNeverPromotesIt() {
        val fail = row(result = CapabilityInput.Result.Fail)
        assertEquals(
            CapabilityLabel.NotSupported,
            input(assetRows = listOf(fail)).resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.NotSupported,
            input(
                assetRows = listOf(fail),
                probeResults = mapOf(keysDevice to CapabilityInput.Result.Pass),
            ).resolve()[keysDevice],
        )
    }

    @Test
    fun declaredAbsentWinsForEveryScopeOfThatCapability() {
        val device = keysDevice
        val native = CapabilityKey(CapabilityInput.Capability.Text, CapabilityScope.NativeField)
        val appGrid = CapabilityKey(CapabilityInput.Capability.Text, CapabilityScope.AppGrid)
        val pass = CapabilityInput.Result.Pass
        val asset = listOf(
            row(key = device, result = pass),
            row(key = native, result = pass),
            row(key = appGrid, result = pass),
        )
        val out = input(
            keys = setOf(device, native, appGrid),
            assetRows = asset,
            declaredAbsent = setOf(CapabilityInput.Capability.Text),
        ).resolve()
        assertEquals(CapabilityLabel.Works, out[device])
        assertEquals(CapabilityLabel.NotSupported, out[native])
        assertEquals(CapabilityLabel.NotSupported, out[appGrid])
    }

    @Test
    fun probeHardFailResolvesNotSupportedForThisKeyOnly() {
        val native = CapabilityKey(CapabilityInput.Capability.Text, CapabilityScope.NativeField)
        val asset = listOf(
            row(key = keysDevice, result = CapabilityInput.Result.Pass),
            row(key = native, result = CapabilityInput.Result.Pass),
        )
        val out = input(
            keys = setOf(keysDevice, native),
            assetRows = asset,
            probeResults = mapOf(native to CapabilityInput.Result.Fail),
        ).resolve()
        assertEquals(CapabilityLabel.Works, out[keysDevice])
        assertEquals(CapabilityLabel.NotSupported, out[native])
    }

    @Test
    fun probePassWithoutRowResolvesBestEffort() {
        val out = input(
            probeResults = mapOf(keysDevice to CapabilityInput.Result.Pass),
        ).resolve()
        assertEquals(CapabilityLabel.BestEffort, out[keysDevice])
    }

    @Test
    fun hjGenerationNeverExceedsBestEffort() {
        val pass = row(result = CapabilityInput.Result.Pass)
        assertEquals(
            CapabilityLabel.BestEffort,
            input(assetRows = listOf(pass), generation = CapabilityInput.Generation.HJ2014_15).resolve()[keysDevice],
        )
        val partial = row(result = CapabilityInput.Result.Partial)
        assertEquals(
            CapabilityLabel.BestEffort,
            input(assetRows = listOf(partial), generation = CapabilityInput.Generation.HJ2014_15).resolve()[keysDevice],
        )
    }

    @Test
    fun hjGenerationKeepsNotSupported() {
        val fail = row(result = CapabilityInput.Result.Fail)
        assertEquals(
            CapabilityLabel.NotSupported,
            input(assetRows = listOf(fail), generation = CapabilityInput.Generation.HJ2014_15).resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.NotSupported,
            input(
                assetRows = listOf(row(result = CapabilityInput.Result.Pass)),
                declaredAbsent = setOf(CapabilityInput.Capability.Keys),
                generation = CapabilityInput.Generation.HJ2014_15,
            ).resolve()[keysDevice],
        )
    }

    @Test
    fun unknownGenerationPassResolvesWorks() {
        val out = input(
            assetRows = listOf(row(result = CapabilityInput.Result.Pass)),
            generation = CapabilityInput.Generation.Unknown,
        ).resolve()
        assertEquals(CapabilityLabel.Works, out[keysDevice])
    }

    @Test
    fun debugOverlayWinsExactKeyAtSameVersion() {
        val asset = row(result = CapabilityInput.Result.Fail)
        val overlay = row(result = CapabilityInput.Result.Pass)
        assertEquals(
            CapabilityLabel.NotSupported,
            input(assetRows = listOf(asset), overlayRows = listOf(overlay), debug = false).resolve()[keysDevice],
        )
        assertEquals(
            CapabilityLabel.Works,
            input(assetRows = listOf(asset), overlayRows = listOf(overlay), debug = true).resolve()[keysDevice],
        )
    }

    @Test
    fun staleOverlayIsIgnored() {
        val overlay = row(result = CapabilityInput.Result.Pass, version = 2)
        val out = input(overlayRows = listOf(overlay), debug = true, version = 1).resolve()
        assertEquals(CapabilityLabel.NotTested, out[keysDevice])
    }

    @Test
    fun oneLabelPerRequestedKey() {
        val native = CapabilityKey(CapabilityInput.Capability.Text, CapabilityScope.NativeField)
        val out = input(
            keys = setOf(keysDevice, native),
            assetRows = listOf(row(key = keysDevice, result = CapabilityInput.Result.Pass)),
        ).resolve()
        assertEquals(setOf(keysDevice, native), out.keys)
        assertEquals(CapabilityLabel.Works, out[keysDevice])
        assertEquals(CapabilityLabel.NotTested, out[native])
    }

    @Test
    fun parseRejectsMalformedAssets() {
        val bad = listOf(
            "",
            "{",
            "{\"matrixVersion\": 1}",
            "{\"matrixVersion\": \"1\", \"rows\": []}",
            "{\"matrixVersion\": 1, \"rows\": []}",
            "{\"matrixVersion\": 1, \"rows\": [{}]}",
            "{\"matrixVersion\": 1, \"rows\": [{\"model\": \"\", \"firmware\": \"F\", " +
                "\"networkType\": \"Wifi\", \"capability\": \"Keys\", \"scope\": \"Device\", " +
                "\"result\": \"Pass\", \"matrixVersion\": 1}]}",
            "{\"matrixVersion\": 1, \"rows\": [{\"model\": \"M\", \"firmware\": \"F\", " +
                "\"networkType\": \"Cable\", \"capability\": \"Keys\", \"scope\": \"Device\", " +
                "\"result\": \"Pass\", \"matrixVersion\": 1}]}",
        )
        for (json in bad) {
            try {
                MatrixRules.parseAsset(json)
                throw AssertionError("expected MatrixFormatException for: $json")
            } catch (e: MatrixFormatException) {
                // Expected.
            }
        }
    }

    @Test
    fun validateRejectsMismatchDuplicateMissingFallbackAndUnknownPair() {
        val fallback = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Unknown,
            result = CapabilityInput.Result.Partial,
        )
        assertTrue(MatrixRules.validate(CapabilityInput.Asset(1, listOf(fallback))).isEmpty())

        val versionMismatch = fallback.copy(matrixVersion = 2)
        assertTrue(MatrixRules.validate(CapabilityInput.Asset(1, listOf(versionMismatch))).isNotEmpty())

        assertTrue(
            MatrixRules.validate(CapabilityInput.Asset(1, listOf(fallback, fallback))).isNotEmpty(),
        )

        val noFallback = row(result = CapabilityInput.Result.Pass)
        assertTrue(MatrixRules.validate(CapabilityInput.Asset(1, listOf(noFallback))).isNotEmpty())

        val badPair = row(
            key = CapabilityKey(CapabilityInput.Capability.Keys, CapabilityScope.FormatJpeg),
            result = CapabilityInput.Result.Pass,
        )
        assertTrue(
            MatrixRules.validate(CapabilityInput.Asset(1, listOf(fallback, badPair))).isNotEmpty(),
        )

        val wildcardMix = fallback.copy(model = MatrixRules.FALLBACK_MODEL, firmware = "T-EXACT-1")
        assertTrue(
            MatrixRules.validate(CapabilityInput.Asset(1, listOf(fallback, wildcardMix))).isNotEmpty(),
        )
    }

    @Test
    fun parseAcceptsFallbackOnlySeed() {
        val json = "{\"matrixVersion\": 1, \"rows\": [{\"model\": \"*\", \"firmware\": \"*unknown*\"," +
            " \"networkType\": \"Unknown\", \"capability\": \"Keys\", \"scope\": \"Device\"," +
            " \"result\": \"Partial\", \"matrixVersion\": 1}]}"
        val asset = MatrixRules.parseAsset(json)
        assertEquals(1, asset.matrixVersion)
        assertEquals(1, asset.rows.size)
        assertTrue(MatrixRules.validate(asset).isEmpty())
        val out = input(assetRows = asset.rows).resolve()
        assertEquals(CapabilityLabel.BestEffort, out[keysDevice])
    }

    @Test
    fun universalFallbackMatchesUnknownFirmwareOnKnownTransport() {
        val fallback = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Unknown,
            result = CapabilityInput.Result.Partial,
        )
        val out = input(
            assetRows = listOf(fallback),
            model = "NEWMODEL",
            firmware = "T-NEW-9.9",
            network = NetworkType.Wifi,
        ).resolve()
        assertEquals(CapabilityLabel.BestEffort, out[keysDevice])
    }

    @Test
    fun validateRequiresUnknownNetworkFallback() {
        val wifiWildcard = row(
            model = MatrixRules.FALLBACK_MODEL,
            firmware = MatrixRules.FALLBACK_FIRMWARE,
            network = NetworkType.Wifi,
            result = CapabilityInput.Result.Partial,
        )
        assertTrue(
            MatrixRules.validate(CapabilityInput.Asset(1, listOf(wifiWildcard))).isNotEmpty(),
        )
    }

    @Test
    fun debugOverlayNeedsValidAssetVersion() {
        val overlay = row(result = CapabilityInput.Result.Pass, version = 0)
        val out = input(overlayRows = listOf(overlay), debug = true, version = 0).resolve()
        assertEquals(CapabilityLabel.NotTested, out[keysDevice])
    }

    @Test
    fun parseRejectsDuplicateObjectKeys() {
        val json = "{\"matrixVersion\": 1, \"matrixVersion\": 1, \"rows\": [" +
            "{\"model\": \"*\", \"firmware\": \"*unknown*\", \"networkType\": \"Unknown\"," +
            " \"capability\": \"Keys\", \"scope\": \"Device\"," +
            " \"result\": \"Partial\", \"matrixVersion\": 1}]}"
        try {
            MatrixRules.parseAsset(json)
            throw AssertionError("expected MatrixFormatException for duplicate keys")
        } catch (e: MatrixFormatException) {
            // Expected.
        }
    }
}
