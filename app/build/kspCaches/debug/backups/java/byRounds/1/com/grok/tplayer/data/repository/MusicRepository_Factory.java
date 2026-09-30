package com.grok.tplayer.data.repository;

import com.grok.tplayer.data.db.TrackDao;
import com.grok.tplayer.data.scanner.LibraryScanner;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class MusicRepository_Factory implements Factory<MusicRepository> {
  private final Provider<TrackDao> trackDaoProvider;

  private final Provider<LibraryScanner> scannerProvider;

  public MusicRepository_Factory(Provider<TrackDao> trackDaoProvider,
      Provider<LibraryScanner> scannerProvider) {
    this.trackDaoProvider = trackDaoProvider;
    this.scannerProvider = scannerProvider;
  }

  @Override
  public MusicRepository get() {
    return newInstance(trackDaoProvider.get(), scannerProvider.get());
  }

  public static MusicRepository_Factory create(Provider<TrackDao> trackDaoProvider,
      Provider<LibraryScanner> scannerProvider) {
    return new MusicRepository_Factory(trackDaoProvider, scannerProvider);
  }

  public static MusicRepository newInstance(TrackDao trackDao, LibraryScanner scanner) {
    return new MusicRepository(trackDao, scanner);
  }
}
