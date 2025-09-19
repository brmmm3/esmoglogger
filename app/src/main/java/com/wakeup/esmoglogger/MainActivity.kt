@file:Suppress("PrivatePropertyName")

package com.wakeup.esmoglogger

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wakeup.esmoglogger.data.Recording
import com.wakeup.esmoglogger.databinding.ActivityMainBinding
import com.wakeup.esmoglogger.location.LocationHandler
import com.wakeup.esmoglogger.serialcommunication.SerialCommunication
import com.wakeup.esmoglogger.serialcommunication.SharedSerialData
import com.wakeup.esmoglogger.ui.chartview.ChartViewFragment
import com.wakeup.esmoglogger.ui.cloud.CloudFragment
import com.wakeup.esmoglogger.ui.home.HomeFragment
import com.wakeup.esmoglogger.ui.mapview.MapViewFragment
import com.wakeup.esmoglogger.ui.settings.SettingsFragment
import com.wakeup.esmoglogger.ui.statistics.StatisticsFragment
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
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BottomNavAdapter
    private var selectedPosition = 0

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
        recyclerView = findViewById(R.id.custom_bottom_nav)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // Sample data for navigation items
        val navItems = listOf(
            NavItem(R.drawable.home_24p, "Home", HomeFragment::class.java),
            NavItem(R.drawable.chart_24p, "Chart", ChartViewFragment::class.java),
            NavItem(R.drawable.globe_blue_24p, "Map", MapViewFragment::class.java),
            NavItem(R.drawable.cloud_24p, "Cloud", CloudFragment::class.java),
            NavItem(R.drawable.statistics_24p, "Statistics", StatisticsFragment::class.java),
            NavItem(R.drawable.settings_24p, "Settings", SettingsFragment::class.java)
        )

        // Initialize adapter
        adapter = BottomNavAdapter(navItems) { position ->
            selectItem(position)
        }
        recyclerView.adapter = adapter

        // Load the initial fragment (Home)
        selectItem(0)

        // Setup USB serial communication
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
            viewModel.setLocationValid(true)
        }

        viewModel.gps.observe(this) { enabled ->
            if (enabled) {
                locationHandler.initialize()
                locationHandler.resume()
            } else {
                viewModel.setLocationValid(false)
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

    private fun selectItem(position: Int) {
        // Update selected position and notify adapter
        selectedPosition = position
        adapter.setSelectedPosition(position)

        // Switch fragment
        val fragmentClass = adapter.getItem(position).fragmentClass
        val fragment = fragmentClass.getDeclaredConstructor().newInstance() as Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
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
