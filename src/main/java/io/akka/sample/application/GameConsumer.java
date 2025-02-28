package io.akka.sample.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.consumer.Consumer;
import io.akka.sample.domain.GameEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ComponentId("gameConsumer")
@Consume.FromEventSourcedEntity(GameEntity.class)
public class GameConsumer extends Consumer {
    private static final Logger logger = LoggerFactory.getLogger(GameConsumer.class);
    private final ComponentClient componentClient;

    public GameConsumer(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

  public Effect onEvent(GameEvent event) {
    return switch (event) {
      case GameEvent.GameOver evt -> onGameOver(evt);
      case GameEvent.GameCreated __ -> effects().done();
      case GameEvent.GameStarted __ -> effects().done();
      case GameEvent.MoveMade __ -> effects().done();
    };
    }

    private Effect onGameOver(GameEvent.GameOver event) {
        String gameId = messageContext().eventSubject().get();
        logger.info("Processing game over event for game {}, winner: {}, loser: {}", 
            gameId, event.winnerId(), event.loserId());

        // Update winner statistics
        var winnerUpdate = componentClient.forKeyValueEntity(event.winnerId())
            .method(PlayerEntity::gameWon)
            .invokeAsync(gameId);

        // Update loser statistics
        var loserUpdate = componentClient.forKeyValueEntity(event.loserId())
            .method(PlayerEntity::gameLost)
            .invokeAsync(gameId);

        return effects().asyncEffect(
            winnerUpdate.thenCompose(__ -> loserUpdate)
                .thenApply(__ -> {
                    logger.info("Updated game statistics for game {}", gameId);
                    return effects().done();
                })
        );
    }
}
