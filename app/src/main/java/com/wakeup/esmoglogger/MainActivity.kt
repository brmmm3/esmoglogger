@file:Suppress("PrivatePropertyName")

package com.wakeup.esmoglogger

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.wakeup.esmoglogger.data.Recording
import com.wakeup.esmoglogger.databinding.ActivityMainBinding
import com.wakeup.esmoglogger.location.LocationHandler
import com.wakeup.esmoglogger.serialcommunication.SerialCommunication
import com.wakeup.esmoglogger.serialcommunication.SharedSerialData
import com.wakeup.esmoglogger.ui.log.SharedLogData
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.osmdroid.config.Configuration
import java.io.File


/* https://hcfricke.com/2018/09/19/emf-11-cornet-ed88t-plus-ein-tri-meter-unter-200e-taugt-es-was/

    WLAN: 2,4-2,48 GHz, 5,15-5,72 GHz
    DECT: 1880-1900 Mhz
    GSM: 890-915, 935-960, 1.710-1.785 und 1.805-1.880 MHz
    UMTS : 1.920-1.980 und 2.110-2.170 MHz
    LTE: um die 800 MHz, 1,8 GHz, 2 GHz und 2,6 GHz
    TETRA: um die 380-410MHz
*/


class MainActivity : AppCompatActivity() {
    private var binding: ActivityMainBinding? = null
    private var serial: SerialCommunication? = null
    private lateinit var locationHandler: LocationHandler
    private val viewModel: SharedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set OSMDroid configuration before inflating the layout
        val osmConfig = Configuration.getInstance()
        osmConfig.userAgentValue = applicationContext.packageName + "/1.0"
        osmConfig.load(applicationContext, getPreferences(MODE_PRIVATE))
        // Set custom cache directory
        val cacheDir = File(cacheDir, "osmdroid_cache")
        cacheDir.mkdirs()
        osmConfig.osmdroidTileCache = cacheDir

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_home, R.id.navigation_chart, R.id.navigation_map, R.id.navigation_log
        ).build()
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        setupWithNavController(binding!!.navView, navController)

        binding!!.navView.menu.findItem(R.id.navigation_map)?.let { menuItem ->
            menuItem.isEnabled = true
            menuItem.icon?.clearColorFilter()
            menuItem.icon?.alpha = 255
        }

        // Setup USB serial communication
        SharedLogData.addLog("Setup Serial")
        serial = SerialCommunication(this)
        SharedSerialData.command.observe(this) { command ->
            if (command) {
                serial?.setupConnection()
            } else {
                serial?.closeConnection()
            }
        }
        this.lifecycleScope.launch {
            SharedSerialData.esmog.collect { lvlFrq ->
                viewModel.addESmog(lvlFrq.first, lvlFrq.second)
            }
        }

        locationHandler = LocationHandler(this) { location ->
            viewModel.addLocation(location)
        }

        viewModel.gps.observe(this) { enabled ->
            if (enabled) {
                locationHandler.initialize()
                locationHandler.resume()
            } else {
                locationHandler.pause()
            }
        }

        addAllSavedRecordings()

        val prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE)
        if (prefs.getBoolean(PREFS_DARKMODE, true)) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serial?.cleanup()
    }

    fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun addAllSavedRecordings() {
        viewModel.recordings.clear()
        val directory = this.filesDir
        for (file in directory.listFiles()!!) {
            if (file.isFile && file.name.startsWith("ESMOG-") && file.extension == "json") {
                viewModel.recordings.add(Recording.fromJson(JSONObject(file.readText()), file.name, file.length()))
            }
        }
    }
}
