package com.akeshari.takecontrol.di

import android.content.Context
import androidx.room.Room
import com.akeshari.takecontrol.data.database.TakeControlDatabase
import com.akeshari.takecontrol.data.database.dao.PermissionHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TakeControlDatabase {
        return Room.databaseBuilder(
            context,
            TakeControlDatabase::class.java,
            "take_control_db"
        ).build()
    }

    @Provides
    fun providePermissionHistoryDao(database: TakeControlDatabase): PermissionHistoryDao {
        return database.permissionHistoryDao()
    }
}
