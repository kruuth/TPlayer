package com.grok.tplayer.player;

import com.grok.tplayer.data.preferences.UserPreferences;
import com.grok.tplayer.data.repository.MusicRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MusicService_MembersInjector implements MembersInjector<MusicService> {
  private final Provider<MusicRepository> musicRepositoryProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public MusicService_MembersInjector(Provider<MusicRepository> musicRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.musicRepositoryProvider = musicRepositoryProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  public static MembersInjector<MusicService> create(
      Provider<MusicRepository> musicRepositoryProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new MusicService_MembersInjector(musicRepositoryProvider, userPreferencesProvider);
  }

  @Override
  public void injectMembers(MusicService instance) {
    injectMusicRepository(instance, musicRepositoryProvider.get());
    injectUserPreferences(instance, userPreferencesProvider.get());
  }

  @InjectedFieldSignature("com.grok.tplayer.player.MusicService.musicRepository")
  public static void injectMusicRepository(MusicService instance, MusicRepository musicRepository) {
    instance.musicRepository = musicRepository;
  }

  @InjectedFieldSignature("com.grok.tplayer.player.MusicService.userPreferences")
  public static void injectUserPreferences(MusicService instance, UserPreferences userPreferences) {
    instance.userPreferences = userPreferences;
  }
}
