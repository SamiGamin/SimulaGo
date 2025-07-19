package com.martinez.simulago.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class) // Le dice a Hilt que estas dependencias vivirán mientras la app viva (Singleton)
object DatabaseModule {

    @Provides
    @Singleton // Asegura que solo se cree UNA instancia de la base de datos en toda la app
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "simulago_database" // Nombre del archivo de la base de datos
        )
            .fallbackToDestructiveMigration()
            .build()
    }


    @Provides
    @Singleton // El DAO también será singleton porque la base de datos lo es
    fun provideSimulationDao(appDatabase: AppDatabase): SimulationDao {
        // Hilt ve que necesita una 'appDatabase' para esta receta,
        // mira la receta de arriba, la ejecuta, y nos pasa el resultado.
        return appDatabase.simulationDao()
    }
}