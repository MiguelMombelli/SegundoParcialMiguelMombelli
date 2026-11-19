package ar.jornadas.data

import kotlinx.coroutines.flow.Flow

class AgendaRepository(private val api: ProgramaApi, private val dao: CarteleraDao) {
    val programa = dao.observar()
    /** null = aún desconocido (Room no respondió), false = sin edición, true = edición descargada. */
    val edicion: Flow<Boolean?> = dao.hayEdicion()

    suspend fun sincronizar() {
        val fichas = api.consultar().map {
            Charla(it.id, it.title, it.track, it.day, it.time, it.room, it.speaker, it.summary)
        }
        dao.reemplazar(fichas)
    }

    suspend fun cambiarFavorita(id: String) = dao.alternar(id)
}
