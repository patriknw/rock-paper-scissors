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
        if (state.player1Id().isPresent() && state.player2Id().isPresent()) {
            logger.info("Lobby is full. Creating GameEntity with Game ID: {}", state.gameId());
            var playerIds = new GameEntity.PlayerIds(state.player1Id().get(), state.player2Id().get());
            return effects().asyncEffect(
                componentClient.forEventSourcedEntity(state.gameId())
                    .method(GameEntity::startGame)
                    .invokeAsync(playerIds)
                    .thenApply(done -> {
                        logger.info("GameEntity created successfully for Game ID: {}", state.gameId());
                        return effects().done();
                    })

            );
        } else {
            logger.info("Lobby is not full. Ignoring state change.");
            return effects().ignore();
        }
    }
}
