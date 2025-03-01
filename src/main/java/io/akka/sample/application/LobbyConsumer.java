package io.akka.sample.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.akka.sample.domain.LobbyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("lobbyConsumer")
@Consume.FromKeyValueEntity(LobbyEntity.class)
public class LobbyConsumer extends Consumer {

  private static final Logger logger = LoggerFactory.getLogger(LobbyConsumer.class);
  private final ComponentClient componentClient;

  public LobbyConsumer(ComponentClient componentClient) {
    this.componentClient = componentClient;
  }

  public Effect onStateChange(LobbyState state) {
    // Check for same player in both slots
    if (state.player1Id().isPresent() && state.player2Id().isPresent() &&
        state.player1Id().get().equals(state.player2Id().get())) {
      logger.warn("Ignoring invalid state: Same player in both slots. Game ID: {}", state.gameId());
      return effects().ignore();
    }

    if (state.player1Id().isPresent() && state.player2Id().isEmpty()) {
      logger.info("First player joined. Creating GameEntity with Game ID: {}", state.gameId());
      return effects().asyncEffect(
          componentClient.forEventSourcedEntity(state.gameId())
              .method(GameEntity::createGame)
              .invokeAsync(new GameEntity.CreateGameRequest(state.player1Id().get()))
              .thenApply(done -> {
                logger.info("GameEntity created successfully for Game ID: {}", state.gameId());
                return effects().done();
              })
      );
    } else if (state.player1Id().isPresent() && state.player2Id().isPresent()) {
      logger.info("Second player joined. Starting game with Game ID: {}", state.gameId());
      var playerIds = new GameEntity.PlayerIds(state.player1Id().get(), state.player2Id().get());
      return effects().asyncEffect(
          componentClient.forEventSourcedEntity(state.gameId())
              .method(GameEntity::startGame)
              .invokeAsync(playerIds)
              .thenApply(done -> {
                logger.info("Game started successfully for Game ID: {}", state.gameId());
                return effects().done();
              })
      );
    } else {
      logger.info("Lobby is empty. Ignoring state change.");
      return effects().ignore();
    }
  }
}
