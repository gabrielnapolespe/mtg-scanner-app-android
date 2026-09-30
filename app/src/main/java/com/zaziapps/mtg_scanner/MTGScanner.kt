package com.zaziapps.mtg_scanner
import android.app.Application
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig

class MTGScanner : Application(), CameraXConfig.Provider {

    override fun getCameraXConfig(): CameraXConfig {
        // Camera 2 engine initialization
        return CameraXConfig.Builder.fromConfig(Camera2Config.defaultConfig()).build()
    }
}