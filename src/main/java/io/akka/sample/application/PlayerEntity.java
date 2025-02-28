package io.akka.sample.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.keyvalueentity.KeyValueEntity;
import io.akka.sample.domain.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;

@ComponentId("player")
public class PlayerEntity extends KeyValueEntity<Player> {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEntity.class);

    public Effect<Done> createPlayer(Player playerState) {
        if (currentState() != null) {
            return effects().reply(done());
        }

        logger.info("Creating player with id: {} and name: {}", playerState.id(), playerState.name());
        return effects()
            .updateState(playerState)
            .thenReply(done());
    }

    public Effect<Done> updatePlayerName(String name) {
        if (currentState() == null) {
            return effects().error("Player not found for id '" + commandContext().entityId() + "'");
        }
        logger.info("Updating player name to: {}", name);
        return effects()
            .updateState(new Player(currentState().id(), name))
            .thenReply(done());
    }

    public ReadOnlyEffect<Player> getPlayer() {
        if (currentState() == null) {
            return effects().error("Player not found for id '" + commandContext().entityId() + "'");
        }
        return effects().reply(currentState());
    }
    
}
