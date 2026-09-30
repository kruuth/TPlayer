package com.grok.tplayer.ui.screens.source;

import com.grok.tplayer.data.repository.MusicRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class SourceViewModel_Factory implements Factory<SourceViewModel> {
  private final Provider<MusicRepository> repositoryProvider;

  public SourceViewModel_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SourceViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static SourceViewModel_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new SourceViewModel_Factory(repositoryProvider);
  }

  public static SourceViewModel newInstance(MusicRepository repository) {
    return new SourceViewModel(repository);
  }
}
