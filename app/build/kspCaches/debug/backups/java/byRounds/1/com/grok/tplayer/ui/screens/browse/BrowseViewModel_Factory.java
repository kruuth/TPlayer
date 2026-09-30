package com.grok.tplayer.ui.screens.browse;

import com.grok.tplayer.data.preferences.UserPreferences;
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
public final class BrowseViewModel_Factory implements Factory<BrowseViewModel> {
  private final Provider<MusicRepository> repositoryProvider;

  private final Provider<UserPreferences> preferencesProvider;

  public BrowseViewModel_Factory(Provider<MusicRepository> repositoryProvider,
      Provider<UserPreferences> preferencesProvider) {
    this.repositoryProvider = repositoryProvider;
    this.preferencesProvider = preferencesProvider;
  }

  @Override
  public BrowseViewModel get() {
    return newInstance(repositoryProvider.get(), preferencesProvider.get());
  }

  public static BrowseViewModel_Factory create(Provider<MusicRepository> repositoryProvider,
      Provider<UserPreferences> preferencesProvider) {
    return new BrowseViewModel_Factory(repositoryProvider, preferencesProvider);
  }

  public static BrowseViewModel newInstance(MusicRepository repository,
      UserPreferences preferences) {
    return new BrowseViewModel(repository, preferences);
  }
}
