package com.wakeup.esmoglogger.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.data.DataSeries
import com.wakeup.esmoglogger.data.JsonViewModel
import com.wakeup.esmoglogger.data.SharedESmogData
import com.wakeup.esmoglogger.databinding.FragmentHomeBinding
import com.wakeup.esmoglogger.location.SharedLocationData
import com.wakeup.esmoglogger.serialcommunication.SharedSerialData
import com.wakeup.esmoglogger.ui.mapview.SharedMapData
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.getValue

class HomeFragment : Fragment() {
    private var binding: FragmentHomeBinding? = null
    private var recordButton: MaterialButton? = null
    private var deleteButton: MaterialButton? = null
    private var saveButton: MaterialButton? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var startTime: LocalDateTime = LocalDateTime.now()
    private var isRecording = false

    private lateinit var fileListAdapter: ArrayAdapter<String>

    private val jsonViewModel: JsonViewModel by viewModels()

    private var fileNameToSave: String? = null
    private var dataSeriesToSave: DataSeries? = null

    private val handler = Handler(Looper.getMainLooper())

    private val writeRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            fileNameToSave?.let {
                dataSeriesToSave?.let { dataSeries ->
                    jsonViewModel.saveJsonToStorage(
                        requireContext().contentResolver, requireContext(),
                        it, dataSeries
                    )
                }
            }
        } else {
            Toast.makeText(requireContext(), "Speicherzugriff verweigert", Toast.LENGTH_SHORT).show()
        }
    }

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            val dt = Duration.between(startTime, LocalDateTime.now())
            view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "${dt.toMillis() / 1000} s"
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // FusedLocationProviderClient initialisieren
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        // LocationCallback definieren
        locationCallback = object : LocationCallback() {
            @SuppressLint("SetTextI18n")
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    val latitude = location.latitude
                    val longitude = location.longitude
                    SharedESmogData.addGpsLocation(latitude, longitude)
                    view?.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=$latitude  Lng=$longitude"
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding!!.getRoot()

        val recordGpsCheckBox: MaterialCheckBox = root.findViewById(R.id.save_gps_checkbox)

        recordGpsCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                SharedMapData.sendCommand("start")
            } else {
                root.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=?  Lng=?"
                SharedMapData.sendCommand("stop")
            }
            SharedESmogData.setGpsRecording(isChecked)
        }
        SharedLocationData.location.observe(viewLifecycleOwner) { location ->
            root.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=${location.latitude}  Lng=${location.longitude}"
        }

        recordButton = root.findViewById<MaterialButton>(R.id.button_record)
        val setNotesButton = root.findViewById<MaterialButton>(R.id.button_set_notes)
        deleteButton = root.findViewById<MaterialButton>(R.id.button_delete)
        deleteButton?.isEnabled = false
        saveButton = root.findViewById<MaterialButton>(R.id.button_save)
        saveButton?.isEnabled = false

        if (isRecording) {
            recordButton?.setIconResource(R.drawable.stop_32p)
        } else {
            recordButton?.setIconResource(R.drawable.record_32p)
        }
        recordButton?.setOnClickListener { v: View? ->
            isRecording = !isRecording
            if (isRecording) {
                SharedSerialData.sendCommand("start")
                recordButton?.setIconResource(R.drawable.stop_32p)
                recordGpsCheckBox.isEnabled = false
                startTime = LocalDateTime.now()
                SharedESmogData.start()
                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                handler.post(updateTimeRunnable)
            } else {
                handler.removeCallbacks(updateTimeRunnable)
                SharedSerialData.sendCommand("stop")
                recordButton?.setIconResource(R.drawable.record_32p)
                recordGpsCheckBox.isEnabled = true
                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                showTextInputDialog(
                    "Name of recording", "Enter name of recording",
                    "stop", currentDateTime.format(formatter), true
                )
            }
        }

        setNotesButton?.setOnClickListener { v: View? ->
            showTextInputDialog(
                "Notes", "Enter notes",
                "notes", "", false
            )
        }

        deleteButton?.setOnClickListener { v: View? ->
            showConfirmationDialog()
        }

        saveButton?.setOnClickListener { v: View? ->
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH_mm_ss")
            showTextInputDialog(
                "Filename", "Enter filename without extension",
                "save", "ESMOG-${currentDateTime.format(formatter)}", true
            )
        }

        val recordingsListView: ListView = root.findViewById(R.id.recordings_list_view)

        val recordings = SharedESmogData.dataSeriesHistory.map { it.filename }.toCollection(ArrayList())
        fileListAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            recordings
        )
        recordingsListView.adapter = fileListAdapter

        recordingsListView.setOnItemClickListener { _, _, position, _ ->
            val text = fileListAdapter.getItem(position) ?: return@setOnItemClickListener
            println(text)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete recorded data")
            .setMessage("Do you want to delete recorded data?")
            .setPositiveButton("Yes") { _, _ ->
                recordButton?.isEnabled = true
                deleteButton?.isEnabled = false
                saveButton?.isEnabled = false
                SharedESmogData.clear()
                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun showTextInputDialog(title: String, hintText: String, command: String, default: String, singleLine: Boolean) {
        // EditText für die Eingabe erstellen
        val input = EditText(requireContext()).apply {
            hint = hintText
            isSingleLine = singleLine
        }
        input.setText(default)
        // Dialog mit MaterialAlertDialogBuilder erstellen
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(input) // EditText in den Dialog einfügen
            .setPositiveButton("OK") { _, _ ->
                // Eingabe verarbeiten
                val text = input.text.toString().trim()
                if (text.isNotEmpty()) {
                    when (command) {
                        "stop" -> {
                            SharedESmogData.stop(text)
                            deleteButton?.isEnabled = true
                            saveButton?.isEnabled = true
                            recordButton?.isEnabled = false
                        }
                        "save" -> {
                            try {
                                val fileName = "${text}.json"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || checkStoragePermission()) {
                                    jsonViewModel.saveJsonToStorage(
                                        requireContext().contentResolver, requireContext(),
                                        fileName, SharedESmogData.dataSeries
                                    )
                                } else {
                                    fileNameToSave = fileName
                                    dataSeriesToSave = SharedESmogData.dataSeries
                                    writeRequestLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                                SharedESmogData.saved(fileName)
                                updateRecordingsList()
                                deleteButton?.isEnabled = false
                                saveButton?.isEnabled = false
                                recordButton?.isEnabled = true
                                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        "notes" -> {
                            SharedESmogData.setNotes(text)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Bitte Text eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dialog schließen
                if (command == "stop") {
                    SharedESmogData.dataSeries.clear()
                    view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                }
            }
            .show()
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Keine Berechtigung erforderlich für MediaStore ab Android 10
        } else {
            requireContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun updateRecordingsList() {
        val recordings = SharedESmogData.dataSeriesHistory.map { it.filename }.toCollection(ArrayList())
        fileListAdapter.clear()
        fileListAdapter.addAll(recordings)
        fileListAdapter.notifyDataSetChanged()
    }
}
