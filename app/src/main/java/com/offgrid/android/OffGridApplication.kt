package com.offgrid.android

import android.app.Application
import com.offgrid.android.nostr.RelayDirectory
import com.offgrid.android.ui.theme.ThemePreferenceManager
import com.offgrid.android.net.ArtiTorManager

/**
 * Main application class for OffGrid Android
 */
class OffGridApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Tor first so any early network goes over Tor
        try {
            val torProvider = ArtiTorManager.getInstance()
            torProvider.init(this)
        } catch (_: Exception){}

        // Initialize relay directory (loads assets/nostr_relays.csv)
        RelayDirectory.initialize(this)

        // Initialize LocationNotesManager dependencies early so sheet subscriptions can start immediately
        try { com.offgrid.android.nostr.LocationNotesInitializer.initialize(this) } catch (_: Exception) { }

        // Initialize favorites persistence early so MessageRouter/NostrTransport can use it on startup
        try {
            com.offgrid.android.favorites.FavoritesPersistenceService.initialize(this)
        } catch (_: Exception) { }

        // Warm up Nostr identity to ensure npub is available for favorite notifications
        try {
            com.offgrid.android.nostr.NostrIdentityBridge.getCurrentNostrIdentity(this)
        } catch (_: Exception) { }

        // Initialize theme preference
        ThemePreferenceManager.init(this)

        // Initialize debug preference manager (persists debug toggles)
        try { com.offgrid.android.ui.debug.DebugPreferenceManager.init(this) } catch (_: Exception) { }

        // Initialize Geohash Registries for persistence
        try {
            com.offgrid.android.nostr.GeohashAliasRegistry.initialize(this)
            com.offgrid.android.nostr.GeohashConversationRegistry.initialize(this)
        } catch (_: Exception) { }

        // Initialize mesh service preferences
        try { com.offgrid.android.service.MeshServicePreferences.init(this) } catch (_: Exception) { }

        // Proactively start the foreground service to keep mesh alive
        try { com.offgrid.android.service.MeshForegroundService.start(this) } catch (_: Exception) { }

        // TorManager already initialized above
    }
}
