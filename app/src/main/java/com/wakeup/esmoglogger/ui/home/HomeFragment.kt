package com.wakeup.esmoglogger.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.SparseBooleanArray
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
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
import androidx.core.util.size

class HomeFragment : Fragment() {
    private val viewModel: SharedViewModel by activityViewModels()
    private var binding: FragmentHomeBinding? = null
    private lateinit var buttonRecord: MaterialButton
    private lateinit var buttonDelete: MaterialButton
    private lateinit var buttonSave: MaterialButton
    private lateinit var recordingsListView: ListView
    private lateinit var actionButtonsRecordings: LinearLayout
    private lateinit var buttonLoadRecordings: MaterialButton
    private lateinit var buttonDeleteRecordings: MaterialButton
    private lateinit var buttonShareRecordings: MaterialButton
    private var actionMode: ActionMode? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    private var startTime: LocalDateTime = LocalDateTime.now()

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

        buttonRecord = root.findViewById<MaterialButton>(R.id.button_record)
        val setNotesButton = root.findViewById<MaterialButton>(R.id.button_set_notes)
        buttonDelete = root.findViewById<MaterialButton>(R.id.button_delete)
        buttonSetEnabled(buttonDelete, false)
        buttonSave = root.findViewById<MaterialButton>(R.id.button_save)
        buttonSetEnabled(buttonSave, false)

        if (viewModel.recording.value == true) {
            buttonRecord.setIconResource(R.drawable.stop_32p)
        } else {
            buttonRecord.setIconResource(R.drawable.record_32p)
        }
        buttonRecord.setOnClickListener { v: View? ->
            if (viewModel.recording.value == true) {
                handler.removeCallbacks(updateTimeRunnable)
                SharedSerialData.stop()
                buttonRecord.setIconResource(R.drawable.record_32p)
                gpsEnabled.isEnabled = true
                val currentDateTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                showTextInputDialog(
                    "Name of recording", "Enter name of recording",
                    "stop", currentDateTime.format(formatter), true
                )
            } else {
                SharedSerialData.start()
                buttonRecord.setIconResource(R.drawable.stop_32p)
                gpsEnabled.isEnabled = false
                startTime = LocalDateTime.now()
                viewModel.startRecording()
                view?.findViewById<TextView>(R.id.textview_recorded_time)?.text = "0 s"
                handler.post(updateTimeRunnable)
            }
        }

        setNotesButton?.setOnClickListener { v: View? ->
            showTextInputDialog(
                "Notes", "Enter notes",
                "notes", "", false
            )
        }

        buttonDelete.setOnClickListener { v: View? ->
            showConfirmationDialog()
        }

        buttonSave.setOnClickListener { v: View? ->
            val currentDateTime = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd.HH_mm_ss")
            showTextInputDialog(
                "Filename", "Enter filename without extension",
                "save", "ESMOG-${currentDateTime.format(formatter)}", true
            )
        }

        recordingsListView = root.findViewById(R.id.recordings_list_view)

        fileListAdapter = FileListAdapter(
            requireContext(),
            recordingsListView,
            viewModel.dataSeriesList
        )
        recordingsListView.adapter = fileListAdapter
        recordingsListView.choiceMode = ListView.CHOICE_MODE_MULTIPLE

        recordingsListView.setOnItemClickListener { _, _, position, _ ->
            val text = fileListAdapter.getItem(position) ?: return@setOnItemClickListener
            println(text)
            recordingsListView.setItemChecked(position, recordingsListView.isItemChecked(position))
            fileListAdapter.notifyDataSetChanged()
            updateButtonVisibility()
        }

        recordingsListView.setOnItemLongClickListener { _, _, position, _ ->
            recordingsListView.setItemChecked(position, !recordingsListView.isItemChecked(position))
            fileListAdapter.notifyDataSetChanged()
            updateButtonVisibility()
            true
        }

        actionButtonsRecordings = root.findViewById<LinearLayout>(R.id.action_buttons_recordings)
        buttonLoadRecordings = root.findViewById<MaterialButton>(R.id.button_load_recordings)
        buttonDeleteRecordings = root.findViewById<MaterialButton>(R.id.button_delete_recordings)
        buttonShareRecordings = root.findViewById<MaterialButton>(R.id.button_share_recordings)

        buttonLoadRecordings.setOnClickListener {  }

        buttonDeleteRecordings.setOnClickListener {
            val selectedItems = getSelectedItems()
            val fileIndex = HashMap<String, Int>()
            viewModel.dataSeriesList.forEachIndexed { index, fileInfo ->
                fileIndex.put(fileInfo.name, index)
            }
            val sdCard = Environment.getExternalStorageDirectory()
            selectedItems.reversed().forEach { name ->
                if (File(sdCard, "${Environment.DIRECTORY_DOCUMENTS}/${name}").delete()) {
                    fileIndex.get(name)?.let { index -> viewModel.dataSeriesList.removeAt(index) }
                }
            }
            clearSelections()
            fileListAdapter.notifyDataSetChanged()
            updateButtonVisibility()
        }

        buttonShareRecordings.setOnClickListener {
            val selectedItems = getSelectedItems()
            val shareText = selectedItems.joinToString(", ")
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Share items"))
            clearSelections()
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
                buttonSetEnabled(buttonRecord, true)
                buttonSetEnabled(buttonDelete, false)
                buttonSetEnabled(buttonSave, false)
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
                            buttonSetEnabled(buttonRecord, false)
                            buttonSetEnabled(buttonDelete, true)
                            buttonSetEnabled(buttonSave, true)
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
                                buttonSetEnabled(buttonRecord, true)
                                buttonSetEnabled(buttonDelete, false)
                                buttonSetEnabled(buttonSave, false)
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
                    viewModel.stopRecording("")
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

    private class FileListAdapter(context: Context, private val listView: ListView, private val files: List<FileInfo>) :
        ArrayAdapter<FileInfo>(context, R.layout.list_item_file, files) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_file, parent, false)

            val isSelected = listView.checkedItemPositions[position]
            view.setBackgroundColor(
                if (isSelected) ContextCompat.getColor(context, android.R.color.holo_blue_light)
                else ContextCompat.getColor(context, android.R.color.transparent)
            )

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

    private fun updateButtonVisibility() {
        actionButtonsRecordings.visibility = if (recordingsListView.checkedItemCount > 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun getSelectedItems(): List<String> {
        val selectedItems = mutableListOf<String>()
        val checkedPositions: SparseBooleanArray = recordingsListView.checkedItemPositions
        for (i in 0 until checkedPositions.size) {
            if (checkedPositions.valueAt(i)) {
                val position = checkedPositions.keyAt(i)
                selectedItems.add(viewModel.dataSeriesList[position].name)
            }
        }
        return selectedItems
    }

    private fun deleteSelectedItems() {
        val selectedItems = getSelectedItems()
        //items.removeAll(selectedItems)
        fileListAdapter.notifyDataSetChanged()
        //recordingsListView.clearChoices()
    }

    private fun clearSelections() {
        recordingsListView.clearChoices()
        fileListAdapter.notifyDataSetChanged()
        updateButtonVisibility()
    }
}
