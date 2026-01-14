package com.example.buteykoexercises.di

import android.content.Context
import androidx.room.Room
import com.example.buteykoexercises.data.local.AppDatabase
import com.example.buteykoexercises.data.local.dao.ControlPauseDao
import com.example.buteykoexercises.data.local.dao.ExerciseDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "buteyko_db"
        ).build()
    }

    @Provides
    fun provideControlPauseDao(db: AppDatabase): ControlPauseDao = db.controlPauseDao()

    @Provides
    fun provideExerciseDao(db: AppDatabase): ExerciseDao = db.exerciseDao()
}
