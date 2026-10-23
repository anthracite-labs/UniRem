package com.unirem

import com.unirem.capability.MatrixAsset
import com.unirem.core.api.MatrixRules
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Asset loading, shipped-asset, and harness-copy tests for Story 1.8.
 * Pure JVM; no Android import.
 */
class MatrixAssetTest {

    private val repoRoot: Path = findRepoRoot()

    private fun assetText(): String =
        repoRoot.resolve("app/src/main/assets/verification-matrix.json").readText()

    @Test
    fun shippedAssetParsesAndValidatesClean() {
        val asset = MatrixRules.parseAsset(assetText())
        assertTrue(MatrixRules.validate(asset).isEmpty())
        assertTrue(asset.matrixVersion > 0)
        assertTrue(asset.rows.isNotEmpty())
        assertTrue(
            asset.rows.all { it.matrixVersion == asset.matrixVersion },
        )
    }

    @Test
    fun shippedAssetIsFallbackOnly() {
        val asset = MatrixRules.parseAsset(assetText())
        assertTrue(
            asset.rows.all {
                it.model == MatrixRules.FALLBACK_MODEL &&
                    it.firmware == MatrixRules.FALLBACK_FIRMWARE
            },
        )
    }

    @Test
    fun shippedAssetCarriesNoSecretsOrUrls() {
        val lowered = assetText().lowercase()
        for (token in listOf("eyj", "begin", "http", "token", "\"pin\"", "spki", "mac")) {
            assertFalse("asset must not contain $token", lowered.contains(token))
        }
    }

    @Test
    fun loadFromTextFallsBackToEmptyOnFault() {
        assertEquals(0, MatrixAsset.loadFromText("").matrixVersion)
        assertEquals(0, MatrixAsset.loadFromText("{broken").matrixVersion)
        val missingFallback = "{\"matrixVersion\": 3, \"rows\": [" +
            "{\"model\": \"M\", \"firmware\": \"F\", \"networkType\": \"Wifi\", " +
            "\"capability\": \"Keys\", \"scope\": \"Device\", " +
            "\"result\": \"Pass\", \"matrixVersion\": 3}]}"
        assertEquals(0, MatrixAsset.loadFromText(missingFallback).matrixVersion)
    }

    @Test
    fun loadFromTextAcceptsValidAsset() {
        val asset = MatrixAsset.loadFromText(assetText())
        assertTrue(asset.matrixVersion > 0)
        assertTrue(MatrixRules.validate(asset).isEmpty())
    }

    companion object {
        private fun findRepoRoot(): Path {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) return dir
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
        }
    }
}
