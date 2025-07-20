package com.martinez.simulago.ui.home

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.martinez.simulago.data.local.SavedSimulation
import com.martinez.simulago.data.remote.UpdateInfo
import com.martinez.simulago.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@Suppress("DEPRECATION")
@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val viewModel: HomeViewModel by viewModels()
    private lateinit var simulationAdapter: SimulationAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
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
                showConfirmationDialog(simulation, null)
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
        // Esta función encapsula la navegación a la tabla de detalles
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
            }
            .setNegativeButton("Cancelar") { _, _ ->
                viewModel.exitSelectionMode()

            }

            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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
            .setPositiveButton("Actualizar ahora") { _, _ ->
                // Abre el enlace de descarga en el navegador
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.updateUrl))
                startActivity(intent)
            }
            .setCancelable(false) // Evita que el usuario lo cierre sin elegir una opción
            .show()
    }

}