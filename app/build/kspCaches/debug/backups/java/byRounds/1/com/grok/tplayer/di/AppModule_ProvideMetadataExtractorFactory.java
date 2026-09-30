package com.grok.tplayer.di;

import android.content.Context;
import com.grok.tplayer.data.scanner.MetadataExtractor;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideMetadataExtractorFactory implements Factory<MetadataExtractor> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideMetadataExtractorFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public MetadataExtractor get() {
    return provideMetadataExtractor(contextProvider.get());
  }

  public static AppModule_ProvideMetadataExtractorFactory create(
      Provider<Context> contextProvider) {
    return new AppModule_ProvideMetadataExtractorFactory(contextProvider);
  }

  public static MetadataExtractor provideMetadataExtractor(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMetadataExtractor(context));
  }
}
