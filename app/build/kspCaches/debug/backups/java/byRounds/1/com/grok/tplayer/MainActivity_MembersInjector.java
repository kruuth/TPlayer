package com.grok.tplayer;

import com.grok.tplayer.data.preferences.UserPreferences;
import com.grok.tplayer.player.PlayerController;
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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<PlayerController> playerControllerProvider;

  private final Provider<UserPreferences> userPreferencesProvider;

  public MainActivity_MembersInjector(Provider<PlayerController> playerControllerProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    this.playerControllerProvider = playerControllerProvider;
    this.userPreferencesProvider = userPreferencesProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<PlayerController> playerControllerProvider,
      Provider<UserPreferences> userPreferencesProvider) {
    return new MainActivity_MembersInjector(playerControllerProvider, userPreferencesProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectPlayerController(instance, playerControllerProvider.get());
    injectUserPreferences(instance, userPreferencesProvider.get());
  }

  @InjectedFieldSignature("com.grok.tplayer.MainActivity.playerController")
  public static void injectPlayerController(MainActivity instance,
      PlayerController playerController) {
    instance.playerController = playerController;
  }

  @InjectedFieldSignature("com.grok.tplayer.MainActivity.userPreferences")
  public static void injectUserPreferences(MainActivity instance, UserPreferences userPreferences) {
    instance.userPreferences = userPreferences;
  }
}
