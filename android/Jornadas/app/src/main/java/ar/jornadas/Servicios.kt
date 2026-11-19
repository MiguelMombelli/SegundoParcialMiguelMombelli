package ar.jornadas

import android.content.Context
import androidx.room.Room
import ar.jornadas.data.*
import ar.jornadas.lab.ServicioLocal
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.atomic.AtomicBoolean

/** Singleton por proceso: ServicioLocal, Room y el contador de llamadas viven lo que vive el proceso. */
class Servicios private constructor(context: Context) {
    val mesa = ServicioLocal(context)
    private val archivo = Room.databaseBuilder(context, Archivo::class.java, "cartelera.db")
        .addMigrations(MIGRATION_1_2).build()
    private val api = Retrofit.Builder().baseUrl("https://programa.invalid/")
        .client(OkHttpClient.Builder().addInterceptor(mesa).build())
        .addConverterFactory(GsonConverterFactory.create()).build().create(ProgramaApi::class.java)
    val agenda = AgendaRepository(api, archivo.cartelera())

    /** Una sola carga automática por proceso. */
    val cargaInicial = AtomicBoolean(false)

    companion object {
        @Volatile private var instancia: Servicios? = null

        fun get(context: Context): Servicios = instancia ?: synchronized(this) {
            instancia ?: Servicios(context.applicationContext).also { instancia = it }
        }
    }
}
