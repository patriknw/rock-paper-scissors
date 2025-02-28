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

import java.util.Optional;
import static akka.Done.done;

@ComponentId("game")
public class GameEntity extends EventSourcedEntity<Game, GameEvent> {
    private static final Logger logger = LoggerFactory.getLogger(GameEntity.class);

    public record PlayerIds(String player1Id, String player2Id) {}
    public record CreateGameRequest(String player1Id) {}
    public record MoveRequest(String playerId, Move move) {}

    public Effect<Done> createGame(CreateGameRequest request) {
        if (currentState() != null) {
            if (currentState().firstPlayerId().equals(request.player1Id())) {
                return effects().reply(done());
            }
            return errorGameAlreadyStarted();
        }

        logger.info("Creating game for player {}", request.player1Id());
        return effects()
            .persist(new GameCreated(request.player1Id()))
            .thenReply(__ -> done());
    }

    public Effect<Done> startGame(PlayerIds playerIds) {
        String player1Id = playerIds.player1Id();
        String player2Id = playerIds.player2Id();

        if (player1Id.equals(player2Id)) {
            return errorSamePlayers();
        }

        if (currentState() != null) {
            if (currentState().firstPlayerId().equals(player1Id) &&
                currentState().secondPlayerId().map(id -> id.equals(player2Id)).orElse(false)) {
                return effects().reply(done());
            }
            if (currentState().secondPlayerId().isEmpty()) {
                logger.info("Adding second player {} to game", player2Id);
                return effects()
                    .persist(new GameStarted(player1Id, player2Id))
                    .thenReply(__ -> done());
            }
            return errorGameAlreadyStarted();
        }

        logger.info("Starting game between {} and {}", player1Id, player2Id);
        return effects()
            .persist(new GameStarted(player1Id, player2Id))
            .thenReply(__ -> done());
    }

    public Effect<Done> makeMove(MoveRequest moveRequest) {
        logger.info("Making move for player {} with move {}", moveRequest.playerId(), moveRequest.move());
        if (currentState() == null) {
            return errorNotFound();
        }

        if (currentState().secondPlayerId().isEmpty()) {
            return effects().error("Cannot make moves until second player joins");
        }

        String playerId = moveRequest.playerId();
        Move move = moveRequest.move();

        Game currentGame = currentState();
        int nbrOfPlayer1Moves = currentGame.getFirstPlayerMoves().size();
        int nbrOfPlayer2Moves = currentGame.getSecondPlayerMoves().size();

        if ((playerId.equals(currentGame.firstPlayerId()) && nbrOfPlayer1Moves > nbrOfPlayer2Moves) ||
            (playerId.equals(currentGame.secondPlayerId().get()) && nbrOfPlayer2Moves > nbrOfPlayer1Moves)) {
            return errorInvalidMoveOrder();
        }

        Game updatedGame = currentGame.addMove(playerId, move);
        Result result = updatedGame.evaluateWinner();

        if (result == Result.PLAYER_ONE_WINS || result == Result.PLAYER_TWO_WINS) {
            String winnerId = result == Result.PLAYER_ONE_WINS ?
                updatedGame.firstPlayerId() :
                updatedGame.secondPlayerId().orElseThrow();
            String loserId = result == Result.PLAYER_ONE_WINS ?
                updatedGame.secondPlayerId().orElseThrow() :
                updatedGame.firstPlayerId();
            return effects().persist(new MoveMade(playerId, move), new GameOver(winnerId, loserId))
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
            case GameCreated evt -> new Game(evt.player1Id(), Optional.empty());
            case GameStarted evt -> new Game(evt.player1Id(), Optional.of(evt.player2Id()));
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
