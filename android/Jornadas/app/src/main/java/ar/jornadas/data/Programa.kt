package ar.jornadas.data

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import retrofit2.http.GET
import kotlinx.coroutines.flow.Flow

data class FichaRemota(val id: String, val title: String, val track: String, val day: Int,
    val time: String, val room: String, val speaker: String, val summary: String)

interface ProgramaApi {
    @GET("programa") suspend fun consultar(): List<FichaRemota>
}

@Entity(tableName = "charlas")
data class Charla(@PrimaryKey val id: String, val titulo: String, val tema: String, val dia: Int,
    val hora: String, val sala: String, val ponente: String, val resumen: String,
    val favorita: Boolean = false)

@Entity(tableName = "edicion")
data class Edicion(@PrimaryKey val id: Int = 1)

@Dao
abstract class CarteleraDao {
    @Query("SELECT * FROM charlas ORDER BY dia, hora, sala")
    abstract fun observar(): Flow<List<Charla>>

    @Query("SELECT COUNT(*) > 0 FROM edicion")
    abstract fun hayEdicion(): Flow<Boolean>

    @Query("UPDATE charlas SET favorita = NOT favorita WHERE id = :id")
    abstract suspend fun alternar(id: String)

    @Query("UPDATE charlas SET titulo = :titulo, tema = :tema, dia = :dia, hora = :hora, " +
        "sala = :sala, ponente = :ponente, resumen = :resumen WHERE id = :id")
    protected abstract suspend fun actualizarDatos(id: String, titulo: String, tema: String,
        dia: Int, hora: String, sala: String, ponente: String, resumen: String): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    protected abstract suspend fun insertar(charla: Charla)

    @Query("SELECT id FROM charlas")
    protected abstract suspend fun ids(): List<String>

    @Query("DELETE FROM charlas WHERE id IN (:ids)")
    protected abstract suspend fun borrar(ids: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun marcarEdicion(edicion: Edicion)

    /** Reemplaza la cartelera por la instantánea recibida, en una sola transacción. */
    @Transaction
    open suspend fun reemplazar(fichas: List<Charla>) {
        val nuevas = fichas.associateBy { it.id }
        for (c in nuevas.values) {
            val filas = actualizarDatos(c.id, c.titulo, c.tema, c.dia, c.hora, c.sala, c.ponente, c.resumen)
            if (filas == 0) insertar(c.copy(favorita = false))
        }
        (ids() - nuevas.keys).chunked(500).forEach { borrar(it) }
        marcarEdicion(Edicion())
    }
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `edicion` (`id` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("INSERT OR IGNORE INTO edicion(id) SELECT 1 WHERE EXISTS (SELECT 1 FROM charlas)")
    }
}

@Database(entities = [Charla::class, Edicion::class], version = 2, exportSchema = false)
abstract class Archivo : RoomDatabase() {
    abstract fun cartelera(): CarteleraDao
}
