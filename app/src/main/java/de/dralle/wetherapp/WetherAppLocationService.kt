package de.dralle.wetherapp

import android.app.Service
import android.content.Intent
import android.os.IBinder

class WetherAppLocationService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
         return null;
    }
}