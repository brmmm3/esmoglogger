package com.wakeup.esmoglogger

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.wakeup.esmoglogger.databinding.ActivityMainBinding
import com.wakeup.esmoglogger.serialcommunication.SerialCommunication
import com.wakeup.esmoglogger.data.SharedESmogData
import com.wakeup.esmoglogger.serialcommunication.SharedSerialData
import com.wakeup.esmoglogger.ui.chartview.SharedChartData
import com.wakeup.esmoglogger.ui.log.SharedLogData

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
    private var time = 0.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.getRoot())

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration.Builder(
            R.id.navigation_home, R.id.navigation_chart, R.id.navigation_log
        ).build()
        //val navController = findNavController(this, R.id.nav_host_fragment_activity_main)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        val navController = navHostFragment.navController
        setupActionBarWithNavController(this, navController, appBarConfiguration)
        setupWithNavController(binding!!.navView, navController)
        // Setup USB serial communication
        SharedLogData.addLog("Setup Serial")
        serial = SerialCommunication(this)
        SharedESmogData.data.observe(this) { value ->
            // value = Pair(SignalLevel, Frequency)
            SharedChartData.add(Pair(time, value))
            //SharedLogData.addLog("Received: $value")
            time += 0.5f
        }
        SharedSerialData.command.observe(this) { command ->
            if (command == "start") {
                time = 0f
                serial?.setupConnection()
            } else if (command == "stop") {
                serial?.closeConnection()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serial?.cleanup()
    }
}