package io.akka.sample.application;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.sample.domain.Game;
import io.akka.sample.domain.LobbyState;
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
  }
}
