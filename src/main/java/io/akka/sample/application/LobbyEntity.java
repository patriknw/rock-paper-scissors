package io.akka.sample.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import io.akka.sample.domain.LobbyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@ComponentId("lobby")
public class LobbyEntity extends KeyValueEntity<LobbyState> {
    private static final Logger logger = LoggerFactory.getLogger(LobbyEntity.class);

    @Override
    public LobbyState emptyState() {
        return new LobbyState(UUID.randomUUID().toString());
    }

    public Effect<LobbyState> joinLobby(String playerId) {
        LobbyState currentState = currentState();
        LobbyState updatedState;
        if (currentState.player1Id().isEmpty()) {
            logger.info("Player {} is joining an empty lobby.", playerId);
            updatedState = currentState.withPlayer1(playerId);
        } else if (currentState.player2Id().isEmpty()) {
            logger.info("Player {} is joining the lobby with player {}.", playerId, currentState.player1Id().get());
            updatedState = currentState.withPlayer2(playerId);
        } else {
            logger.info("Lobby is full. Creating a new lobby for player {}.", playerId);
            updatedState = new LobbyState(Optional.of(playerId), Optional.empty(), UUID.randomUUID().toString());
        }
        return effects()
            .updateState(updatedState)
            .thenReply(updatedState);
    }

    public ReadOnlyEffect<LobbyState> getLobby() {
        return effects().reply(currentState());
    }
}
