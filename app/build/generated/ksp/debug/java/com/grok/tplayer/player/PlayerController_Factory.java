package com.grok.tplayer.player;

import android.content.Context;
import com.grok.tplayer.data.preferences.UserPreferences;
import com.grok.tplayer.data.repository.MusicRepository;
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
public final class PlayerController_Factory implements Factory<PlayerController> {
  private final Provider<Context> contextProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  private final Provider<MusicRepository> musicRepositoryProvider;

  public PlayerController_Factory(Provider<Context> contextProvider,
      Provider<UserPreferences> userPreferencesProvider,
      Provider<MusicRepository> musicRepositoryProvider) {
    this.contextProvider = contextProvider;
    this.userPreferencesProvider = userPreferencesProvider;
    this.musicRepositoryProvider = musicRepositoryProvider;
  }

  @Override
  public PlayerController get() {
    return newInstance(contextProvider.get(), userPreferencesProvider.get(), musicRepositoryProvider.get());
  }

  public static PlayerController_Factory create(Provider<Context> contextProvider,
      Provider<UserPreferences> userPreferencesProvider,
      Provider<MusicRepository> musicRepositoryProvider) {
    return new PlayerController_Factory(contextProvider, userPreferencesProvider, musicRepositoryProvider);
  }

  public static PlayerController newInstance(Context context, UserPreferences userPreferences,
      MusicRepository musicRepository) {
    return new PlayerController(context, userPreferences, musicRepository);
  }
}
