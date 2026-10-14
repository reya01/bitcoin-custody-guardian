package com.custodyguardian.data.model

import android.content.Context
import java.io.File

/**
 * Single source of truth for the model file used by the answer pipeline.
 * Replaces the old ChatPlaceholderScreen-local modelFile() helper.
 */
object ModelPaths {

    private var cached: ModelManifest? = null

    fun manifest(context: Context): ModelManifest? {
        cached?.let { return it }
        val manifest = try {
            ModelManifest.parse(
                context.assets.open("model_manifest.json").bufferedReader().readText(),
            )
        } catch (_: Exception) {
            null
        } ?: return null
        cached = manifest
        return manifest
    }

    /** The verified model file, or null (gate UI handles onboarding). */
    fun modelFile(context: Context): File? {
        val manifest = manifest(context) ?: return null
        return ModelLocations.resolve(context, manifest)
    }
}
