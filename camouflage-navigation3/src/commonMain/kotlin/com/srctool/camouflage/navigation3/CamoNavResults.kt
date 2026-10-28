package com.srctool.camouflage.navigation3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Results passed from an overlay entry back to its caller (the Navigation 3 "returning results" recipe). The
 * overlay calls [set] before it removes itself; the caller either awaits [showForResult] or [consume]s it.
 * Results are keyed by the entry key's `toString()` (use data objects and data classes as keys), and they're
 * saved with the store, so their values must be saveable (strings, numbers, `@Parcelize` on Android).
 */
@Stable
public class CamoNavResults internal constructor(private val store: SnapshotStateMap<String, Any?>) {
    private val waiters = mutableMapOf<String, CompletableDeferred<Any?>>()

    /** Delivers [value] for [key]: to a waiting [showForResult], or to the next [consume]. */
    public fun <T> set(key: Any, value: T) {
        val id = key.toString()
        val waiter = waiters.remove(id)
        if (waiter != null) waiter.complete(value) else store[id] = value
    }

    /** Calls [onResult] once with the result for [key] when there is one, then forgets it. */
    @Suppress("UNCHECKED_CAST") // the caller names the type it set
    @Composable
    public fun <T> consume(key: Any, onResult: (T) -> Unit) {
        val id = key.toString()
        if (id !in store) return
        val value = store[id]
        LaunchedEffect(id, value) {
            store.remove(id)
            onResult(value as T)
        }
    }

    internal fun await(key: Any): CompletableDeferred<Any?> =
        CompletableDeferred<Any?>().also { waiters[key.toString()] = it }

    internal fun forget(key: Any, waiter: CompletableDeferred<Any?>) {
        val id = key.toString()
        if (waiters[id] === waiter) waiters.remove(id)
    }

    internal companion object {
        val Saver: Saver<CamoNavResults, Any> = Saver(
            save = { HashMap(it.store) },
            restore = {
                @Suppress("UNCHECKED_CAST") // saved by `save` above
                val saved = it as Map<String, Any?>
                CamoNavResults(mutableStateMapOf<String, Any?>().apply { putAll(saved) })
            },
        )
    }
}

/** A results store that survives configuration changes and process death. */
@Composable
public fun rememberCamoNavResults(): CamoNavResults =
    rememberSaveable(saver = CamoNavResults.Saver) { CamoNavResults(mutableStateMapOf()) }

/**
 * Adds [key] to this back stack and waits for its result: the value the overlay [CamoNavResults.set]s, or
 * `null` when the entry leaves without one (dismissed). Reads like Flutter's `await show(…)`. The back stack must
 * be snapshot state (`rememberNavBackStack`, `mutableStateListOf`) so a dismissal is noticed. For a result that
 * must survive process death while the overlay is open, use [CamoNavResults.consume] instead.
 */
@Suppress("UNCHECKED_CAST") // the caller names the type the overlay sets
public suspend fun <K : Any, T> MutableList<K>.showForResult(key: K, results: CamoNavResults): T? {
    val waiter = results.await(key)
    add(key)
    return try {
        coroutineScope {
            val popped = launch {
                snapshotFlow { key in this@showForResult }.first { !it }
                waiter.complete(null)
            }
            waiter.await().also { popped.cancel() } as T?
        }
    } finally {
        results.forget(key, waiter)
    }
}
