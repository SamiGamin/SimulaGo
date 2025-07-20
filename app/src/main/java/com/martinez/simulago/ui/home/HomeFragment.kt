package com.martinez.simulago.ui.home

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.martinez.simulago.BuildConfig
import com.martinez.simulago.R
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.remote.UpdateInfo
import com.martinez.simulago.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.DecimalFormat
import kotlin.text.format

@Suppress("DEPRECATION")
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var simulationAdapter: SimulationAdapter
    private var progressMonitoringJob: Job? = null
    private var progressDialog: AlertDialog? = null
    private var updateProgressBar: ProgressBar? = null
    private var updateProgressText: TextView? = null
    private var updateDialogTitleTextView: TextView? = null
    private var downloadID: Long = 0L
    private val TAG = "HomeFragment_Update"
    private lateinit var onDownloadComplete: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing DownloadReceiver.")
        initializeDownloadReceiver()
    }
    private fun initializeDownloadReceiver() {
        onDownloadComplete = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "onDownloadComplete - Received action: ${intent?.action}")
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                Log.d(TAG, "onDownloadComplete - Received ID: $id, Expected ID: $downloadID")

                if (downloadID == id && intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                    Log.d(TAG, "onDownloadComplete: Broadcast for relevant download ID $id received.")
                    progressMonitoringJob?.cancel()
                    Log.d(TAG, "onDownloadComplete: Cancelled progressMonitoringJob.")

                    val downloadManager = context?.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val query = DownloadManager.Query().setFilterById(downloadID)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        Log.d(TAG, "onDownloadComplete: Download status is $status.")

                        if (DownloadManager.STATUS_SUCCESSFUL == status) {
                            Log.d(TAG, "onDownloadComplete: Download SUCCESSFUL. Attempting to dismiss dialog.")
                            progressDialog?.let {
                                if (it.isShowing) {
                                    Log.d(TAG, "onDownloadComplete: Dialog is showing, dismissing.")
                                    it.dismiss()
                                } else {
                                    Log.d(TAG, "onDownloadComplete: Dialog was not showing when trying to dismiss for success.")
                                }
                            } ?: Log.d(TAG, "onDownloadComplete: progressDialog was null on success.")

                            val uriStringIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                            val uriString = cursor.getString(uriStringIndex)
                            uriString?.let { installApk(Uri.parse(it), context) }
                                ?: Log.e(TAG, "Download URI is null")
                        } else {
                            val reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                            val reason = cursor.getInt(reasonIndex)
                            val reasonText = getDownloadErrorReason(reason)
                            Log.e(TAG, "Download failed. Status: $status, Reason: $reason ($reasonText)")

                            if (progressDialog?.isShowing == true) {
                                Log.d(TAG, "onDownloadComplete: Download FAILED. Updating dialog.")
                                updateProgressBar?.isVisible = false
                                updateDialogTitleTextView?.text = "Error en la Descarga"
                                updateProgressText?.text = "Razón: $reasonText"
                                progressDialog?.getButton(DialogInterface.BUTTON_POSITIVE)?.visibility = View.GONE
                                val negativeButton = progressDialog?.getButton(DialogInterface.BUTTON_NEGATIVE)
                                negativeButton?.text = "Cerrar"
                                negativeButton?.setOnClickListener { progressDialog?.dismiss() }
                                negativeButton?.visibility = View.VISIBLE
                            } else {
                                Log.d(TAG, "onDownloadComplete: Download FAILED but dialog was not showing. Showing Toast.")
                                Toast.makeText(context, "Error en la descarga: $reasonText", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Log.e(TAG, "onDownloadComplete: Cursor was empty for download ID $downloadID.")
                    }
                    cursor.close()
                    downloadID = 0L
                    Log.d(TAG, "onDownloadComplete: Reset downloadID to 0.")
                } else {
                    Log.d(TAG, "onDownloadComplete: Broadcast received but not for the current download ID or wrong action. Expected ID: $downloadID, Received ID: $id, Action: ${intent?.action}")
                }
            }
        }
    }

    private fun getDownloadErrorReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "No se puede reanudar"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "Dispositivo no encontrado"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "El archivo ya existe"
            DownloadManager.ERROR_FILE_ERROR -> "Error de archivo"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "Error de datos HTTP"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "Espacio insuficiente"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "Demasiadas redirecciones"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "Código HTTP no manejado"
            DownloadManager.ERROR_UNKNOWN -> "Error desconocido"
            else -> reason.toString()
        }
    }

    private fun installApk(localFileUri: Uri, context: Context) {
        // DownloadManager devuelve un URI local tipo file:///storage/... o content://downloads/...
        // Para FileProvider, necesitamos convertir el file:/// a un objeto File si es necesario.
        val fileToInstall: File
        if ("file".equals(localFileUri.scheme, ignoreCase = true)) {
            fileToInstall = File(localFileUri.path!!)
        } else {
            // Si DownloadManager devuelve un content URI (ej. content://downloads/my_downloads/...),
            // podríamos necesitar copiarlo a una ubicación que FileProvider pueda manejar,
            // o verificar si el instalador de paquetes puede manejar directamente ese content URI.
            // Por simplicidad, asumimos que podemos obtener una ruta de archivo.
            // Esta parte podría necesitar ajustes según el URI exacto que devuelva DownloadManager.
            // Una forma más robusta es copiar el archivo desde el InputStream del content URI a un archivo local en getExternalFilesDir.
            Log.d(TAG, "URI del DownloadManager no es 'file': $localFileUri. Intentando usar path directo.")
            val path = getPathFromUri(context, localFileUri) // Helper function to get path if it's a content URI
            if (path != null) {
                fileToInstall = File(path)
            } else {
                Toast.makeText(context, "No se pudo obtener la ruta del archivo descargado.", Toast.LENGTH_LONG).show()
                Log.e(TAG, "No se pudo obtener la ruta del archivo desde el URI: $localFileUri")
                return
            }
        }


        if (!fileToInstall.exists()) {
            Toast.makeText(context, "El archivo APK descargado no se encuentra.", Toast.LENGTH_LONG).show()
            Log.e(TAG, "El archivo APK no existe en la ruta: ${fileToInstall.absolutePath}")
            return
        }


        val authority = "${BuildConfig.APPLICATION_ID}.provider"
        val contentUri: Uri = FileProvider.getUriForFile(context, authority, fileToInstall)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Necesario si se llama desde un contexto no-Activity (como el BroadcastReceiver)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(context, "Habilita la instalación desde fuentes desconocidas para SimulaGo en Ajustes.", Toast.LENGTH_LONG).show()
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse(String.format("package:%s", context.packageName))
                }
                startActivity(settingsIntent) // El usuario deberá reintentar la actualización después de dar el permiso.
                return
            }
        }

        try {
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar la instalación del APK", e)
            Toast.makeText(context, "Error al iniciar la instalación: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    // Helper para obtener la ruta de un URI, puede ser necesario para algunos content URIs de DownloadManager
    @SuppressLint("Range")
    private fun getPathFromUri(context: Context, uri: Uri): String? {
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            val projection = arrayOf("_data") // DownloadManager.COLUMN_LOCAL_FILENAME o MediaStore.Images.Media.DATA
            var cursor: android.database.Cursor? = null
            try {
                cursor = context.contentResolver.query(uri, projection, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    // Intenta obtener la columna _data, que suele tener la ruta del archivo
                    // A veces DownloadManager.COLUMN_LOCAL_FILENAME también funciona, pero _data es más común.
                    val dataColumnIndex = cursor.getColumnIndex("_data")
                    if (dataColumnIndex != -1) {
                        return cursor.getString(dataColumnIndex)
                    }
                    // Como fallback, intenta con DownloadManager.COLUMN_LOCAL_FILENAME si existe
                    val localFilenameIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_FILENAME)
                    if (localFilenameIndex != -1) {
                        return cursor.getString(localFilenameIndex)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error obteniendo path desde content URI", e)
            } finally {
                cursor?.close()
            }
        } else if ("file".equals(uri.scheme, ignoreCase = true)) {
            return uri.path
        }
        return null
    }
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(onDownloadComplete, intentFilter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            requireActivity().registerReceiver(onDownloadComplete, intentFilter)
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        simulationAdapter = SimulationAdapter(
            onItemClicked = { simulation ->
                // La lógica ahora depende del MODO en el que estemos
                val isInSelectionMode = viewModel.uiState.value.isInSelectionMode

                if (isInSelectionMode) {
                    // Si estamos en "Modo Selección", un clic significa ACTIVAR
                    showConfirmationDialog(
                        simulation,
                        null
                    ) // Pasamos null porque sabemos que no hay crédito activo
                } else {
                    // Si NO estamos en "Modo Selección", un clic significa VER DETALLES
                    navigateToDetails(simulation)
                }
            },
            onDeleteClicked = { simulation ->
               viewModel.deleteSimulation(simulation)
            }
        )
        binding.rvSimulations.adapter = simulationAdapter
    }

    @SuppressLint("SetTextI18n")
    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Actualiza el adaptador primero
                    simulationAdapter.submitList(state.allSimulations)


                    val hasSimulations = state.allSimulations.isNotEmpty()

                    // Lógica de visibilidad principal
                    binding.tvEmptyState.isVisible = !hasSimulations
                    binding.rvSimulations.isVisible = hasSimulations
                    binding.tvTitle.isVisible =
                        hasSimulations // El título solo se muestra si hay una lista

                    if (hasSimulations) {
                        // Si hay simulaciones, decidimos qué tarjeta mostrar
                        binding.cardOnboarding.isVisible = state.activeCredit == null
                        binding.cardActiveCredit.isVisible = state.activeCredit != null
                    } else {
                        // Si no hay simulaciones, ocultamos ambas tarjetas
                        binding.cardOnboarding.isVisible = false
                        binding.cardActiveCredit.isVisible = false
                    }
                    if (state.isInSelectionMode) {
                        binding.tvTitle.text = "Selecciona un Crédito"
                    } else {
                        binding.tvTitle.text = "Mis Simulaciones Guardadas"
                    }

                    // Llenar datos de la tarjeta activa (sin cambios)
                    state.activeCredit?.let { activeCredit ->
                        binding.tvActiveCreditName.text =
                            "Crédito Activo: ${activeCredit.simulationName}"
                    }
                    state.updateAvailableInfo?.let { updateInfo ->
                        showUpdateDialog(updateInfo)
                        viewModel.onUpdateDialogShown() // Resetea el estado para no mostrarlo de nuevo
                    }
                }
            }
        }
    }

    private fun navigateToDetails(simulation: SavedSimulation) {
        if (simulation.isActiveCredit) {
            val action = HomeFragmentDirections.actionNavigationHomeToManageCreditFragment(simulation.id)
            findNavController().navigate(action)
        } else {
            val table = viewModel.getAmortizationTableForSimulation(simulation)
            val action = HomeFragmentDirections.actionNavigationHomeToAmortizationFragment(
                table.toTypedArray(),
                simulation.simulationName
            )
            findNavController().navigate(action)
        }
    }

    private fun setupListeners() {
        binding.btnChooseActive.setOnClickListener {
            viewModel.enterSelectionMode()
            Toast.makeText(
                context,
                "Selecciona una simulación de la lista para activarla",
                Toast.LENGTH_LONG
            ).show()
        }

        binding.btnManageCredit.setOnClickListener {
            viewModel.uiState.value.activeCredit?.let { activeCredit ->
                val action = HomeFragmentDirections.actionNavigationHomeToManageCreditFragment(activeCredit.id)
                findNavController().navigate(action)
            }
        }
    }


    // 6. NUEVA FUNCIÓN PARA MOSTRAR EL DIÁLOGO DE CONFIRMACIÓN
    private fun showConfirmationDialog(
        simulationToActivate: SavedSimulation,
        nothing: Nothing?
    ) {
        val dialogTitle =
            "Cambiar Crédito Activo"

        val dialogMessage =
            "¿Estás seguro de que quieres establecer '${simulationToActivate.simulationName}' como tu crédito activo?"


        MaterialAlertDialogBuilder(requireContext())
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setPositiveButton("Confirmar") { _, _ ->
                viewModel.onSetAsActiveCreditClicked(simulationToActivate)
                viewModel.exitSelectionMode()
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.exitSelectionMode()
            }.show()
    }

    override fun onResume() {
        super.onResume()
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (viewModel.uiState.value.isInSelectionMode) {
                        viewModel.exitSelectionMode()
                    } else {
                        // Permite el comportamiento normal del botón "Atrás"
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
            })
    }
    private fun showUpdateDialog(updateInfo: UpdateInfo) {
        val releaseNotes = updateInfo.releaseNotes.joinToString("\n• ")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("¡Nueva Versión Disponible! (${updateInfo.latestVersionName})")
            .setMessage("Novedades:\n• $releaseNotes")
            .setNegativeButton("Más tarde", null)
            .setPositiveButton("Actualizar ahora") { dialog, _ ->
                // Verificar permiso para instalar paquetes desconocidos en Android O+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    if (!requireContext().packageManager.canRequestPackageInstalls()) {
                        Toast.makeText(requireContext(), "Se requiere permiso para instalar actualizaciones. Habilítalo en Ajustes y reintenta.", Toast.LENGTH_LONG).show()
                        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                            data = Uri.parse(String.format("package:%s", requireContext().packageName))
                        }
                        startActivity(intent)
                        return@setPositiveButton // Detener aquí, el usuario debe dar permiso y reintentar
                    }
                }

                dialog.dismiss()
                showDownloadProgressDialog(updateInfo)
            }
            .setCancelable(false) // Evita que el usuario lo cierre sin elegir una opción
            .show()
    }
    private fun showDownloadProgressDialog(updateInfo: UpdateInfo) {
        Log.d("UPDATE_DEBUG", "URL recibida en el fragmento: ${updateInfo.updateUrl}")
        Log.d(TAG, "showDownloadProgressDialog: Iniciando descarga de actualización para ${updateInfo.latestVersionName}")
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_update_progress, null)
        updateProgressBar = dialogView.findViewById(R.id.pbUpdateProgress)
        updateProgressText = dialogView.findViewById(R.id.tvUpdateProgressText)
        updateDialogTitleTextView = dialogView.findViewById(R.id.tvUpdateDialogTitle)

        progressDialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        updateDialogTitleTextView?.text = "Descargando Actualización..."
        updateProgressText?.text = "Iniciando..."
        updateProgressBar?.isIndeterminate = true
        updateProgressBar?.isVisible = true

        progressDialog?.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancelar") { dialog, _ ->
            progressMonitoringJob?.cancel()
            val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            if (downloadID != 0L) {
                downloadManager.remove(downloadID)
                downloadID = 0L
            }
            dialog.dismiss()
        }

        val apkUrl = updateInfo.updateUrl
        val fileName = "SimulaGo_v${updateInfo.latestVersionName}.apk"
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Actualización SimulaGo")
            .setDescription("Descargando ${fileName}...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(requireContext(), Environment.DIRECTORY_DOWNLOADS, fileName)
            .setMimeType("application/vnd.android.package-archive")
        Log.i(TAG, "showDownloadProgressDialog: Attempting to download APK from URL: $apkUrl")
        val downloadManager = requireContext().getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        if(downloadID != 0L) {
            downloadManager.remove(downloadID)
        }
        downloadID = downloadManager.enqueue(request)

        if (downloadID == -1L) {
            Log.e(TAG, "Fallo al encolar la descarga con DownloadManager.")
            updateDialogTitleTextView?.text = "Error al Iniciar"
            updateProgressText?.text = "No se pudo iniciar la descarga. Verifica tu conexión o almacenamiento e inténtalo de nuevo."
            updateProgressBar?.isVisible = false
            progressDialog?.getButton(DialogInterface.BUTTON_NEGATIVE)?.text = "Cerrar"
            downloadID = 0L
            progressDialog?.show()
            return
        }

        Toast.makeText(requireContext(), "Iniciando descarga...", Toast.LENGTH_SHORT).show()

        progressMonitoringJob = viewLifecycleOwner.lifecycleScope.launch {
            var isDownloadRunning = true
            while (isActive && isDownloadRunning) {
                if (downloadID == 0L) {
                    isDownloadRunning = false
                    continue
                }
                val query = DownloadManager.Query().setFilterById(downloadID)
                val cursor: Cursor? = downloadManager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val totalBytesColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val downloadedBytesColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)

                    val status = cursor.getInt(statusColumn)
                    val totalBytes = cursor.getInt(totalBytesColumn)
                    val downloadedBytes = cursor.getInt(downloadedBytesColumn)
                    cursor.close()

                    if (status == DownloadManager.STATUS_RUNNING || status == DownloadManager.STATUS_PAUSED) {
                        if (totalBytes > 0) {
                            updateProgressBar?.isIndeterminate = false
                            val progress = (downloadedBytes * 100L / totalBytes).toInt()
                            updateProgressBar?.progress = progress
                            val mbFormat = DecimalFormat("#.##")
                            val downloadedMB = mbFormat.format(downloadedBytes / (1024.0 * 1024.0))
                            val totalMB = mbFormat.format(totalBytes / (1024.0 * 1024.0))
                            updateProgressText?.text = "$progress% ($downloadedMB MB / $totalMB MB)"
                        } else {
                            updateProgressBar?.isIndeterminate = true
                            updateProgressText?.text = "Descargando..."
                        }
                    } else {
                        isDownloadRunning = false
                    }
                } else {
                    Log.d(TAG, "Cursor nulo o vacío en monitoreo, posible finalización de descarga ID: $downloadID")
                    isDownloadRunning = false
                    if (cursor != null) cursor.close()
                }
                delay(1000)
            }
        }
        progressDialog?.show()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        // Cancelar el registro del receiver
        try {
            requireActivity().unregisterReceiver(onDownloadComplete)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Receiver no registrado o ya desregistrado: ${e.message}")
        }
        _binding = null
    }

}