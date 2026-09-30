package com.grok.tplayer.data.scanner;

import android.content.Context;
import com.grok.tplayer.data.db.TrackDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class LibraryScanner_Factory implements Factory<LibraryScanner> {
  private final Provider<Context> contextProvider;

  private final Provider<TrackDao> trackDaoProvider;

  private final Provider<MetadataExtractor> metadataExtractorProvider;

  public LibraryScanner_Factory(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<MetadataExtractor> metadataExtractorProvider) {
    this.contextProvider = contextProvider;
    this.trackDaoProvider = trackDaoProvider;
    this.metadataExtractorProvider = metadataExtractorProvider;
  }

  @Override
  public LibraryScanner get() {
    return newInstance(contextProvider.get(), trackDaoProvider.get(), metadataExtractorProvider.get());
  }

  public static LibraryScanner_Factory create(Provider<Context> contextProvider,
      Provider<TrackDao> trackDaoProvider, Provider<MetadataExtractor> metadataExtractorProvider) {
    return new LibraryScanner_Factory(contextProvider, trackDaoProvider, metadataExtractorProvider);
  }

  public static LibraryScanner newInstance(Context context, TrackDao trackDao,
      MetadataExtractor metadataExtractor) {
    return new LibraryScanner(context, trackDao, metadataExtractor);
  }
}
