package com.martinez.simulago.data.local

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "simulago_database"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun provideSimulationDao(appDatabase: AppDatabase): SimulationDao {
        return appDatabase.simulationDao()
    }

    @Provides
    @Singleton
    fun providePaymentDao(appDatabase: AppDatabase): PaymentDao {
        return appDatabase.paymentDao()
    }
}
