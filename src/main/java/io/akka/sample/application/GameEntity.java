package io.akka.sample.application;

import akka.Done;
import akka.javasdk.annotations.ComponentId;
import akka.javasdk.eventsourcedentity.EventSourcedEntity;
import io.akka.sample.domain.Game;
import io.akka.sample.domain.GameEvent;
import io.akka.sample.domain.GameEvent.*;
import io.akka.sample.domain.Game.Move;
import io.akka.sample.domain.Game.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static akka.Done.done;

@ComponentId("game")
public class GameEntity extends EventSourcedEntity<Game, GameEvent> {
    private static final Logger logger = LoggerFactory.getLogger(GameEntity.class);

    public record PlayerIds(String player1Id, String player2Id) {}

    public record MoveRequest(String playerId, Move move) {}

    public Effect<Done> startGame(PlayerIds playerIds) {
        String player1Id = playerIds.player1Id();
        String player2Id = playerIds.player2Id();

        if (player1Id.equals(player2Id)) {
            return errorSamePlayers();
        } else if (currentState() == null) {
            logger.info("Starting game between {} and {}", player1Id, player2Id);
            return effects()
                .persist(new GameStarted(player1Id, player2Id))
                .thenReply(__ -> done());
        } else {
            return errorGameAlreadyStarted();
        }
    }

    public Effect<Done> makeMove(MoveRequest moveRequest) {
        if (currentState() == null) {
            return errorNotFound();
        }

        String playerId = moveRequest.playerId();
        Move move = moveRequest.move();

        Game currentGame = currentState();
        int nbrOfPlayer1Moves = currentGame.getFirstPlayerMoves().size();
        int nbrOfPlayer2Moves = currentGame.getSecondPlayerMoves().size();

        if ((playerId.equals(currentGame.firstPlayerId()) && nbrOfPlayer1Moves > nbrOfPlayer2Moves) ||
            (playerId.equals(currentGame.secondPlayerId()) && nbrOfPlayer2Moves > nbrOfPlayer1Moves)) {
            return errorInvalidMoveOrder();
        }

        Game updatedGame = currentGame.addMove(playerId, move);
        Result result = updatedGame.evaluateWinner();

        if (result == Result.PLAYER_ONE_WINS || result == Result.PLAYER_TWO_WINS) {
            String winnerId = result == Result.PLAYER_ONE_WINS ? updatedGame.firstPlayerId() : updatedGame.secondPlayerId();
            return effects().persist(new MoveMade(playerId, move), new GameOver(winnerId))
                .thenReply(__ -> done());
        } else {
            return effects().persist(new MoveMade(playerId, move))
                .thenReply(__ -> done());
        }
    }

    public ReadOnlyEffect<Game> getState() {
        if (currentState() == null) {
            return errorNotFound();
        }
        return effects().reply(currentState());
    }

    @Override
    public Game applyEvent(GameEvent event) {
        return switch (event) {
            case GameStarted evt -> new Game(evt.player1Id(), evt.player2Id());
            case MoveMade evt -> currentState().addMove(evt.playerId(), evt.move());
            case GameOver evt -> currentState(); // No state change needed for GameOver
        };
    }

    private <T> ReadOnlyEffect<T> errorNotFound() {
        return effects().error(
            "No game found for id '" + commandContext().entityId() + "'");
    }

    private <T> Effect<T> errorGameAlreadyStarted() {
        return effects().error(
            "Game already started id '" + commandContext().entityId() + "'");
    }

    private <T> Effect<T> errorSamePlayers() {
        return effects().error(
            "Cannot start a game with the same player as both participants.");
    }

    private <T> Effect<T> errorInvalidMoveOrder() {
        return effects().error(
            "Invalid move order: a player cannot have more than one move more than the other player.");
    }
}
