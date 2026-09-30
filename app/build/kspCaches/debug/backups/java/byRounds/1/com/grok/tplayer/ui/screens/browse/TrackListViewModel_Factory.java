package com.grok.tplayer.ui.screens.browse;

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
public final class TrackListViewModel_Factory implements Factory<TrackListViewModel> {
  private final Provider<MusicRepository> repositoryProvider;

  public TrackListViewModel_Factory(Provider<MusicRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public TrackListViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static TrackListViewModel_Factory create(Provider<MusicRepository> repositoryProvider) {
    return new TrackListViewModel_Factory(repositoryProvider);
  }

  public static TrackListViewModel newInstance(MusicRepository repository) {
    return new TrackListViewModel(repository);
  }
}
