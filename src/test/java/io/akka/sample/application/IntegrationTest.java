package io.akka.sample.application;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.sample.domain.Game;
import io.akka.sample.domain.Game.Move;
import io.akka.sample.domain.LobbyState;
import io.akka.sample.domain.Player;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class IntegrationTest extends TestKitSupport {
  private static final Logger logger = LoggerFactory.getLogger(IntegrationTest.class);

  @Test
  public void testLobbyAndGameEntityFlow() {
    String lobbyId = "lobby-1";
    String player1Id = "player1";
    String player2Id = "player2";

    // Create players
    await(componentClient.forKeyValueEntity(player1Id)
        .method(PlayerEntity::createPlayer)
        .invokeAsync(new Player(player1Id, "Alice")));

    await(componentClient.forKeyValueEntity(player2Id)
        .method(PlayerEntity::createPlayer)
        .invokeAsync(new Player(player2Id, "Bob")));

    // Join first player to the lobby
    LobbyState lobbyState1 = await(componentClient.forKeyValueEntity(lobbyId)
        .method(LobbyEntity::joinLobby)
        .invokeAsync(player1Id));

    // Verify first player and game creation
    assertTrue(lobbyState1.player1Id().isPresent());
    assertTrue(lobbyState1.player2Id().isEmpty());
    String gameId = lobbyState1.gameId();
    assertNotNull(gameId);

    // Verify GameEntity is created with only first player
    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          Game game = await(componentClient.forEventSourcedEntity(gameId)
              .method(GameEntity::getState)
              .invokeAsync());
          assertEquals(player1Id, game.firstPlayerId());
          assertTrue(game.secondPlayerId().isEmpty());
        });

    logger.info("GameEntity created with first player, Game ID: {}", gameId);

    // Join second player to the lobby
    LobbyState lobbyState2 = await(componentClient.forKeyValueEntity(lobbyId)
        .method(LobbyEntity::joinLobby)
        .invokeAsync(player2Id));

    // Verify both players are in the lobby
    assertTrue(lobbyState2.player1Id().isPresent());
    assertTrue(lobbyState2.player2Id().isPresent());
    assertEquals(gameId, lobbyState2.gameId());

    // Verify GameEntity is updated with both players
    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          Game game = await(componentClient.forEventSourcedEntity(gameId)
              .method(GameEntity::getState)
              .invokeAsync());
          assertEquals(player1Id, game.firstPlayerId());
          assertEquals(Optional.of(player2Id), game.secondPlayerId());
        });

    logger.info("Second player joined successfully, Game ID: {}", gameId);

    // Play game until player1 wins
    await(componentClient.forEventSourcedEntity(gameId)
        .method(GameEntity::makeMove)
        .invokeAsync(new GameEntity.MoveRequest(player1Id, Move.ROCK)));

    await(componentClient.forEventSourcedEntity(gameId)
        .method(GameEntity::makeMove)
        .invokeAsync(new GameEntity.MoveRequest(player2Id, Move.SCISSORS)));

    await(componentClient.forEventSourcedEntity(gameId)
        .method(GameEntity::makeMove)
        .invokeAsync(new GameEntity.MoveRequest(player1Id, Move.ROCK)));

    await(componentClient.forEventSourcedEntity(gameId)
        .method(GameEntity::makeMove)
        .invokeAsync(new GameEntity.MoveRequest(player2Id, Move.SCISSORS)));

    // Verify game statistics are updated
    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          Player player1 = await(componentClient.forKeyValueEntity(player1Id)
              .method(PlayerEntity::getPlayer)
              .invokeAsync());
          Player player2 = await(componentClient.forKeyValueEntity(player2Id)
              .method(PlayerEntity::getPlayer)
              .invokeAsync());

          assertEquals(1, player1.gamesWon());
          assertEquals(0, player1.gamesLost());
          assertTrue(player1.hasRecordedGame(gameId));

          assertEquals(0, player2.gamesWon());
          assertEquals(1, player2.gamesLost());
          assertTrue(player2.hasRecordedGame(gameId));
        });

    logger.info("Game completed and statistics updated, Game ID: {}", gameId);

    // Verify leaderboard is updated
    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          LeaderboardView.Leaderboard leaderboard = await(componentClient.forView()
              .method(LeaderboardView::getTopPlayers)
              .invokeAsync(10));

          var players = leaderboard.players().stream()
              .filter(p -> p.playerId().equals(player1Id) || p.playerId().equals(player2Id))
              .toList();

          assertEquals(2, players.size());

          var winner = players.stream()
              .filter(p -> p.playerId().equals(player1Id))
              .findFirst()
              .orElseThrow();
          var loser = players.stream()
              .filter(p -> p.playerId().equals(player2Id))
              .findFirst()
              .orElseThrow();

          assertEquals("Alice", winner.playerName());
          assertEquals(1, winner.gamesWon());
          assertEquals(0, winner.gamesLost());
          assertTrue(winner.score() > 0);

          assertEquals("Bob", loser.playerName());
          assertEquals(0, loser.gamesWon());
          assertEquals(1, loser.gamesLost());
          assertEquals(0.0, loser.score());

          // Winner should be ranked higher than loser
          assertTrue(winner.score() > loser.score());
        });

    logger.info("Leaderboard updated successfully");
  }
}
