package com.example.clipboardsync

import android.content.Intent
import android.service.quicksettings.TileService

class ClipboardTileService : TileService() {
    override fun onClick() {
        super.onClick()

        // Android 10+ prevents reading clipboard from background services like TileService.
        // The officially supported workaround is to launch a transparent Activity 
        // which acts as the foreground component, reads the clipboard, and immediately closes.
        
        val intent = Intent(this, TileActionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // This method tells the notification shade to collapse and launch the intent
        startActivityAndCollapse(intent)
    }
}
