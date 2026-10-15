package com.grok.tplayer.ui.navigation;

import com.grok.tplayer.data.repository.MusicRepository;
import com.grok.tplayer.player.PlayerController;
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
public final class StartDestinationViewModel_Factory implements Factory<StartDestinationViewModel> {
  private final Provider<MusicRepository> repositoryProvider;

  private final Provider<PlayerController> playerControllerProvider;

  public StartDestinationViewModel_Factory(Provider<MusicRepository> repositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    this.repositoryProvider = repositoryProvider;
    this.playerControllerProvider = playerControllerProvider;
  }

  @Override
  public StartDestinationViewModel get() {
    return newInstance(repositoryProvider.get(), playerControllerProvider.get());
  }

  public static StartDestinationViewModel_Factory create(
      Provider<MusicRepository> repositoryProvider,
      Provider<PlayerController> playerControllerProvider) {
    return new StartDestinationViewModel_Factory(repositoryProvider, playerControllerProvider);
  }

  public static StartDestinationViewModel newInstance(MusicRepository repository,
      PlayerController playerController) {
    return new StartDestinationViewModel(repository, playerController);
  }
}
