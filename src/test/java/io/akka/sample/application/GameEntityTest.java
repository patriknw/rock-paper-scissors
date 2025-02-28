package io.akka.sample.application;

import akka.Done;
import akka.javasdk.testkit.EventSourcedTestKit;
import io.akka.sample.application.GameEntity.CreateGameRequest;
import io.akka.sample.application.GameEntity.MoveRequest;
import io.akka.sample.application.GameEntity.PlayerIds;
import io.akka.sample.domain.Game;
import io.akka.sample.domain.GameEvent;
import io.akka.sample.domain.GameEvent.*;
import io.akka.sample.domain.Game.Move;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameEntityTest {

    private EventSourcedTestKit<Game, GameEvent, GameEntity> testKit;

    @BeforeEach
    public void setup() {
        testKit = EventSourcedTestKit.of(GameEntity::new);
    }

    @Test
    public void testCreateGame() {
        var player1Id = "player1";

        var result = testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));

        assertEquals(Done.getInstance(), result.getReply());

        var gameCreatedEvent = result.getNextEventOfType(GameCreated.class);
        assertEquals(player1Id, gameCreatedEvent.player1Id());
    }

    @Test
    public void testCreateGameIdempotent() {
        var player1Id = "player1";

        // First creation
        testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));

        // Second creation with same player should be idempotent
        var result = testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));
        assertEquals(Done.getInstance(), result.getReply());
        assertFalse(result.didPersistEvents());
    }

    @Test
    public void testCreateGameWithDifferentPlayerFails() {
        var player1Id = "player1";
        var player2Id = "player2";

        testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));
        var result = testKit.call(entity -> entity.createGame(new CreateGameRequest(player2Id)));

        assertTrue(result.isError());
    }

    @Test
    public void testStartGameAfterCreation() {
        var player1Id = "player1";
        var player2Id = "player2";

        testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));
        var result = testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));

        assertEquals(Done.getInstance(), result.getReply());

        var gameStartedEvent = result.getNextEventOfType(GameStarted.class);
        assertEquals(player1Id, gameStartedEvent.player1Id());
        assertEquals(player2Id, gameStartedEvent.player2Id());
    }

    @Test
    public void testStartGameIdempotent() {
        var player1Id = "player1";
        var player2Id = "player2";

        testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));

        // Second start with same players should be idempotent
        var result = testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));

        assertEquals(Done.getInstance(), result.getReply());
        assertFalse(result.didPersistEvents());
    }

    @Test
    public void testStartGameWithDifferentPlayersFails() {
        var player1Id = "player1";
        var player2Id = "player2";
        var player3Id = "player3";

        testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));
        var result = testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player3Id)));

        assertTrue(result.isError());
    }

    @Test
    public void testCannotMoveBeforeSecondPlayerJoins() {
        var player1Id = "player1";

        testKit.call(entity -> entity.createGame(new CreateGameRequest(player1Id)));
        var result = testKit.call(entity -> entity.makeMove(new MoveRequest(player1Id, Move.ROCK)));

        assertTrue(result.isError());
        assertEquals("Cannot make moves until second player joins", result.getError());
    }

    @Test
    public void testMakeMoveAndGameOver() {
        var player1Id = "player1";
        var player2Id = "player2";

        testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));

        for (int i = 0; i < 2; i++) {
            testKit.call(entity -> entity.makeMove(new MoveRequest(player1Id, Move.ROCK)));
            testKit.call(entity -> entity.makeMove(new MoveRequest(player2Id, Move.SCISSORS)));
        }

        var result = testKit.call(entity -> entity.makeMove(new MoveRequest(player1Id, Move.ROCK)));
        assertEquals(Done.getInstance(), result.getReply());

        var moveMadeEvent = result.getNextEventOfType(MoveMade.class);
        assertEquals(player1Id, moveMadeEvent.playerId());
        assertEquals(Move.ROCK, moveMadeEvent.move());

        var gameOverEvent = result.getNextEventOfType(GameOver.class);
        assertEquals(player1Id, gameOverEvent.winnerId());
    }

    @Test
    public void testErrorSamePlayers() {
        var playerId = "player1";

        var result = testKit.call(entity -> entity.startGame(new PlayerIds(playerId, playerId)));

        assertTrue(result.isError());
        assertEquals("Cannot start a game with the same player as both participants.", result.getError());
    }

    @Test
    public void testInvalidMoveOrder() {
        var player1Id = "player1";
        var player2Id = "player2";

        testKit.call(entity -> entity.startGame(new PlayerIds(player1Id, player2Id)));
        testKit.call(entity -> entity.makeMove(new MoveRequest(player1Id, Move.ROCK)));

        var result = testKit.call(entity -> entity.makeMove(new MoveRequest(player1Id, Move.PAPER)));

        assertTrue(result.isError());
        assertEquals("Invalid move order: a player cannot have more than one move more than the other player.", result.getError());
    }
}
