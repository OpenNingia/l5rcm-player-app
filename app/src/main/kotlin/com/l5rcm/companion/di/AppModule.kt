package com.l5rcm.companion.di

import android.content.Context
import androidx.room.Room
import com.l5rcm.companion.data.catalog.DatapackCatalog
import com.l5rcm.companion.data.datapack.AndroidXmlParserFactory
import com.l5rcm.companion.data.datapack.DatapackParser
import com.l5rcm.companion.data.datapack.XmlParserFactory
import com.l5rcm.companion.data.repository.AppPreferences
import com.l5rcm.companion.data.repository.CharacterRepository
import com.l5rcm.companion.data.repository.DatapackRepository
import com.l5rcm.companion.data.session.SessionDatabase
import com.l5rcm.companion.data.session.SessionRepository
import com.l5rcm.companion.data.session.SessionStateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    @Provides
    @Singleton
    fun provideXmlParserFactory(): XmlParserFactory = AndroidXmlParserFactory

    @Provides
    @Singleton
    fun provideDatapackParser(factory: XmlParserFactory): DatapackParser = DatapackParser(factory)

    @Provides
    @Singleton
    fun provideDatapacksDir(@ApplicationContext context: Context): File =
        File(context.filesDir, "datapacks").apply { mkdirs() }

    @Provides
    @Singleton
    fun provideAppPreferences(@ApplicationContext context: Context): AppPreferences =
        AppPreferences(context)

    @Provides
    @Singleton
    fun provideDatapackCatalog(client: OkHttpClient, datapacksDir: File): DatapackCatalog =
        DatapackCatalog(client, datapacksDir)

    @Provides
    @Singleton
    fun provideDatapackRepository(
        datapacksDir: File,
        parser: DatapackParser,
        catalog: DatapackCatalog,
        prefs: AppPreferences,
    ): DatapackRepository = DatapackRepository(datapacksDir, parser, catalog, prefs)

    @Provides
    @Singleton
    fun provideCharacterRepository(
        @ApplicationContext context: Context,
        prefs: AppPreferences,
    ): CharacterRepository = CharacterRepository(context, prefs)

    @Provides
    @Singleton
    fun provideSessionDatabase(@ApplicationContext context: Context): SessionDatabase =
        Room.databaseBuilder(context, SessionDatabase::class.java, "session.db").build()

    @Provides
    @Singleton
    fun provideSessionStateDao(db: SessionDatabase): SessionStateDao = db.sessionStateDao()

    @Provides
    @Singleton
    fun provideSessionRepository(dao: SessionStateDao): SessionRepository = SessionRepository(dao)
}
