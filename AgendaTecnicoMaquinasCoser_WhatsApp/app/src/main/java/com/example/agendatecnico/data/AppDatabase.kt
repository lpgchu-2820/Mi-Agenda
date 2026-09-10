package com.example.agendatecnico.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val city: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "machines",
    foreignKeys = [ForeignKey(
        entity = Client::class,
        parentColumns = ["id"],
        childColumns = ["clientId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("clientId")]
)
data class Machine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val brand: String = "",
    val model: String = "",
    val serialNumber: String = "",
    val machineType: String = "",
    val condition: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "repairs",
    foreignKeys = [ForeignKey(
        entity = Machine::class,
        parentColumns = ["id"],
        childColumns = ["machineId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("machineId")]
)
data class Repair(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineId: Long,
    val date: String,
    val description: String,
    val diagnosis: String = "",
    val parts: String = "",
    val labor: String = "",
    val total: Double = 0.0,
    val status: String = "Pendiente"
)

@Entity(
    tableName = "photos",
    foreignKeys = [ForeignKey(
        entity = Repair::class,
        parentColumns = ["id"],
        childColumns = ["repairId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("repairId")]
)
data class RepairPhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repairId: Long,
    val uri: String
)

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name")
    fun observeAll(): Flow<List<Client>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun get(id: Long): Client?

    @Insert suspend fun insert(client: Client): Long
    @Update suspend fun update(client: Client)
    @Delete suspend fun delete(client: Client)
}

@Dao
interface MachineDao {
    @Query("SELECT * FROM machines WHERE clientId = :clientId ORDER BY brand, model")
    fun observeForClient(clientId: Long): Flow<List<Machine>>

    @Query("SELECT * FROM machines WHERE id = :id")
    suspend fun get(id: Long): Machine?

    @Insert suspend fun insert(machine: Machine): Long
    @Update suspend fun update(machine: Machine)
    @Delete suspend fun delete(machine: Machine)
}

@Dao
interface RepairDao {
    @Query("SELECT * FROM repairs WHERE machineId = :machineId ORDER BY date DESC")
    fun observeForMachine(machineId: Long): Flow<List<Repair>>

    @Query("SELECT * FROM repairs WHERE id = :id")
    suspend fun get(id: Long): Repair?

    @Insert suspend fun insert(repair: Repair): Long
    @Update suspend fun update(repair: Repair)
    @Delete suspend fun delete(repair: Repair)
}

@Dao
interface PhotoDao {
    @Query("SELECT * FROM photos WHERE repairId = :repairId ORDER BY id DESC")
    fun observeForRepair(repairId: Long): Flow<List<RepairPhoto>>

    @Insert suspend fun insert(photo: RepairPhoto): Long
    @Delete suspend fun delete(photo: RepairPhoto)
}

@Database(
    entities = [Client::class, Machine::class, Repair::class, RepairPhoto::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun machineDao(): MachineDao
    abstract fun repairDao(): RepairDao
    abstract fun photoDao(): PhotoDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agenda_tecnico.db"
                ).build().also { INSTANCE = it }
            }
    }
}
