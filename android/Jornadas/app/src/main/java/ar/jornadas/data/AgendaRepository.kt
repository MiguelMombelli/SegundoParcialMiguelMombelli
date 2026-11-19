package ar.jornadas.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa = dao.observar()
    /** null = aún desconocido (Room no respondió), false = sin edición, true = edición descargada. */
    val edicion: Flow<Boolean?> = dao.hayEdicion()

    /** Petición en vuelo; también es la guarda atómica (compareAndSet). */
    val cargando = MutableStateFlow(false)
    /** true si la última sincronización falló. */
    val fallo = MutableStateFlow(false)

    suspend fun sincronizar() {
        if (!cargando.compareAndSet(false, true)) return
        fallo.value = false
        try {
            val fichas = api.consultar().map {
                Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
            }
            dao.reemplazar(fichas)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallo.value = true
        } finally {
            cargando.value = false
        }
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
