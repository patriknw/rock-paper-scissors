package io.akka.sample.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import io.akka.sample.domain.LobbyState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

import static akka.Done.done;

@ComponentId("lobby")
public class LobbyEntity extends KeyValueEntity<LobbyState> {
    private static final Logger logger = LoggerFactory.getLogger(LobbyEntity.class);

    @Override
    public LobbyState emptyState() {
        return new LobbyState(UUID.randomUUID().toString());
    }

    public Effect<Done> joinLobby(String playerId) {
        LobbyState currentState = currentState();
        if (currentState.player1Id().isEmpty()) {
            logger.info("Player {} is joining an empty lobby.", playerId);
            return effects()
                .updateState(currentState.withPlayer1(playerId))
                .thenReply(done());
        } else if (currentState.player2Id().isEmpty()) {
            logger.info("Player {} is joining the lobby with player {}.", playerId, currentState.player1Id().get());
            return effects()
                .updateState(currentState.withPlayer2(playerId))
                .thenReply(done());
        } else {
            logger.info("Lobby is full. Creating a new lobby for player {}.", playerId);
            return effects()
                .updateState(new LobbyState(Optional.of(playerId), Optional.empty(), UUID.randomUUID().toString()))
                .thenReply(done());
        }
    }

    public ReadOnlyEffect<LobbyState> getLobby() {
        return effects().reply(currentState());
    }
}
