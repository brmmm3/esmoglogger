package com.wakeup.esmoglogger.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.switchmaterial.SwitchMaterial
import com.wakeup.esmoglogger.buttonSetEnabled
import com.wakeup.esmoglogger.FileInfo
import com.wakeup.esmoglogger.R
import com.wakeup.esmoglogger.SharedViewModel
import com.wakeup.esmoglogger.data.DataSeries
import com.wakeup.esmoglogger.data.JsonViewModel
import com.wakeup.esmoglogger.databinding.FragmentHomeBinding
import com.wakeup.esmoglogger.serialcommunication.SharedSerialData
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.getValue

class HomeFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private var binding: FragmentHomeBinding? = null
    private var recordButton: MaterialButton? = null
    private var deleteButton: MaterialButton? = null
    private var saveButton: MaterialButton? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var startTime: LocalDateTime = LocalDateTime.now()
    private var isRecording = false

    private lateinit var fileListAdapter: FileListAdapter

    private val jsonViewModel: JsonViewModel by viewModels()

    private var fileNameToSave: String? = null
    private var dataSeriesToSave: DataSeries? = null

    private val handler = Handler(Looper.getMainLooper())

    private val writeRequestLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            jsonViewModel.saveJsonToStorage(
                requireContext().contentResolver, requireContext(),
                fileNameToSave!!, dataSeriesToSave!!
            )
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
                    viewModel.addLocation(location)
                    view?.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=${location.latitude}\nLng=${location.longitude}\nAlt=${location.altitude}"
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

        val gpsEnabled: SwitchMaterial = root.findViewById(R.id.save_gps_enabled)

        gpsEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                viewModel.startGps()
            } else {
                root.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=?\nLng=?\nAlt=?"
                viewModel.stopGps()
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.locationAndESmogQueue.collect { value ->
                    root.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=${value.latitude}\nLng=${value.longitude}\nAlt=${value.altitude}"
                }
            }
        }
        val value = viewModel.location.value
        value?.let { root.findViewById<TextView>(R.id.textview_gps_location)?.text = "Lat=${it.latitude}\nLng=${value.longitude}\nAlt=${value.altitude}" }

        recordButton = root.findViewById<MaterialButton>(R.id.button_record)
        val setNotesButton = root.findViewById<MaterialButton>(R.id.button_set_notes)
        deleteButton = root.findViewById<MaterialButton>(R.id.button_delete)
        buttonSetEnabled(deleteButton, false)
        saveButton = root.findViewById<MaterialButton>(R.id.button_save)
        buttonSetEnabled(saveButton, false)

        if (isRecording) {
            recordButton?.setIconResource(R.drawable.stop_32p)
        } else {
            recordButton?.setIconResource(R.drawable.record_32p)
        }
        recordButton?.setOnClickListener { v: View? ->
            isRecording = !isRecording
            if (isRecording) {
                SharedSerialData.start()
                recordButton?.setIconResource(R.drawable.stop_32p)
                gpsEnabled.isEnabled = false
                startTime = LocalDateTime.now()
                viewModel.startRecording()
                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                handler.post(updateTimeRunnable)
            } else {
                handler.removeCallbacks(updateTimeRunnable)
                SharedSerialData.stop()
                recordButton?.setIconResource(R.drawable.record_32p)
                gpsEnabled.isEnabled = true
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

        fileListAdapter = FileListAdapter(
            requireContext(),
            viewModel.dataSeriesList
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
                buttonSetEnabled(recordButton, true)
                buttonSetEnabled(deleteButton, false)
                buttonSetEnabled(saveButton, false)
                viewModel.clear()
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
                            viewModel.stopRecording(text)
                            buttonSetEnabled(recordButton, false)
                            buttonSetEnabled(deleteButton, true)
                            buttonSetEnabled(saveButton, true)
                        }
                        "save" -> {
                            try {
                                val fileName = "${text}.json"
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || checkStoragePermission()) {
                                    jsonViewModel.saveJsonToStorage(
                                        requireContext().contentResolver, requireContext(),
                                        fileName, viewModel.dataSeries
                                    )
                                } else {
                                    fileNameToSave = fileName
                                    dataSeriesToSave = viewModel.dataSeries
                                    writeRequestLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                }
                                viewModel.saved(FileInfo(fileName, File(fileName).length()))
                                updateRecordingsList()
                                buttonSetEnabled(recordButton, true)
                                buttonSetEnabled(deleteButton, false)
                                buttonSetEnabled(saveButton, false)
                                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                        "notes" -> {
                            viewModel.setNotes(text)
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Bitte Text eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dialog schließen
                if (command == "stop") {
                    viewModel.dataSeries.clear()
                    view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                }
            }
            .show()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            true // Keine Berechtigung erforderlich für MediaStore ab Android 10
        } else {
            requireContext().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private class FileListAdapter(context: Context, private val files: List<FileInfo>) :
        ArrayAdapter<FileInfo>(context, R.layout.list_item_file, files) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)
            val fileInfo = files[position]

            val fileNameTextView = view.findViewById<TextView>(R.id.fileNameTextView)
            val fileSizeTextView = view.findViewById<TextView>(R.id.fileSizeTextView)

            fileNameTextView.text = fileInfo.name
            fileSizeTextView.text = formatFileSize(fileInfo.size)

            return view
        }

        private fun formatFileSize(size: Long): String {
            val kb = 1024.0
            val mb = kb * 1024
            val gb = mb * 1024
            return when {
                size >= gb -> "${DecimalFormat("#.##").format(size / gb)} GB"
                size >= mb -> "${DecimalFormat("#.##").format(size / mb)} MB"
                size >= kb -> "${DecimalFormat("#.##").format(size / kb)} KB"
                else -> "$size bytes"
            }
        }
    }

    private fun updateRecordingsList() {
        fileListAdapter.clear()
        fileListAdapter.addAll(viewModel.dataSeriesList)
        fileListAdapter.notifyDataSetChanged()
    }
}
