package com.grok.tplayer.di;

import com.grok.tplayer.data.db.AppDatabase;
import com.grok.tplayer.data.db.TrackDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation"
})
public final class AppModule_ProvideTrackDaoFactory implements Factory<TrackDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideTrackDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TrackDao get() {
    return provideTrackDao(dbProvider.get());
  }

  public static AppModule_ProvideTrackDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideTrackDaoFactory(dbProvider);
  }

  public static TrackDao provideTrackDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTrackDao(db));
  }
}
