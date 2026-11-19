package ar.jornadas.ui

import android.app.Application
import androidx.lifecycle.*
import ar.jornadas.Servicios
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProgramaModel(app: Application, private val savedStateHandle: SavedStateHandle) : AndroidViewModel(app) {
    private val servicios = Servicios.get(app)
    val mesa = servicios.mesa
    private val repo = servicios.agenda
    val charlas = repo.programa.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    /** null = desconocido, false = sin edición descargada, true = hay edición. */
    val hayEdicion = repo.edicion.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    /** Filtros de lectura: sobreviven a rotación, pestañas y muerte del proceso. */
    val dia: StateFlow<Int> = savedStateHandle.getStateFlow("dia", 0)
    val tema: StateFlow<String> = savedStateHandle.getStateFlow("tema", "Todos")
    fun elegirDia(d: Int) { savedStateHandle["dia"] = d }
    fun elegirTema(t: String) { savedStateHandle["tema"] = t }
    val cargando: StateFlow<Boolean> = repo.cargando
    val fallo: StateFlow<Boolean> = repo.fallo

    /** Carga automática: exactamente una por proceso. */
    fun cargarAlIniciar() {
        if (servicios.cargaInicial.compareAndSet(false, true)) recargar()
    }

    /** Actualizar / Reintentar explícito. La guarda de petición en vuelo está en el repositorio. */
    fun recargar() {
        viewModelScope.launch { repo.sincronizar() }
    }

    fun marcar(id: String) { viewModelScope.launch { repo.cambiarFavorita(id) } }
}
