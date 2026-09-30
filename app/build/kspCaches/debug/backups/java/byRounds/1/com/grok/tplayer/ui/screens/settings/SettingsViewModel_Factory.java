package com.grok.tplayer.ui.screens.settings;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<UserPreferences> preferencesProvider;

  private final Provider<MusicRepository> repositoryProvider;

  public SettingsViewModel_Factory(Provider<UserPreferences> preferencesProvider,
      Provider<MusicRepository> repositoryProvider) {
    this.preferencesProvider = preferencesProvider;
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(preferencesProvider.get(), repositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(Provider<UserPreferences> preferencesProvider,
      Provider<MusicRepository> repositoryProvider) {
    return new SettingsViewModel_Factory(preferencesProvider, repositoryProvider);
  }

  public static SettingsViewModel newInstance(UserPreferences preferences,
      MusicRepository repository) {
    return new SettingsViewModel(preferences, repository);
  }
}
