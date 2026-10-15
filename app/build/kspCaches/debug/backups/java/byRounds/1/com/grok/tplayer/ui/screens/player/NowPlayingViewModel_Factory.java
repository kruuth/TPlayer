package com.grok.tplayer.ui.screens.player;

import com.grok.tplayer.data.preferences.UserPreferences;
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
public final class NowPlayingViewModel_Factory implements Factory<NowPlayingViewModel> {
  private final Provider<UserPreferences> prefsProvider;

  public NowPlayingViewModel_Factory(Provider<UserPreferences> prefsProvider) {
    this.prefsProvider = prefsProvider;
  }

  @Override
  public NowPlayingViewModel get() {
    return newInstance(prefsProvider.get());
  }

  public static NowPlayingViewModel_Factory create(Provider<UserPreferences> prefsProvider) {
    return new NowPlayingViewModel_Factory(prefsProvider);
  }

  public static NowPlayingViewModel newInstance(UserPreferences prefs) {
    return new NowPlayingViewModel(prefs);
  }
}
