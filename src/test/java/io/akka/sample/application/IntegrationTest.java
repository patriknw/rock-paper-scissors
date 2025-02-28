package io.akka.sample.application;

import akka.javasdk.testkit.TestKitSupport;
import io.akka.sample.domain.LobbyState;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    await(componentClient.forKeyValueEntity(lobbyId)
        .method(LobbyEntity::joinLobby)
        .invokeAsync(player1Id));

    // Join second player to the lobby
    await(componentClient.forKeyValueEntity(lobbyId)
        .method(LobbyEntity::joinLobby)
        .invokeAsync(player2Id));

    // Retrieve the lobby state to get the game ID
    LobbyState lobbyState = await(componentClient.forKeyValueEntity(lobbyId)
        .method(LobbyEntity::getLobby)
        .invokeAsync());

    assertTrue(lobbyState.player1Id().isPresent());
    assertTrue(lobbyState.player2Id().isPresent());

    String gameId = lobbyState.gameId();
    assertNotNull(gameId);

    // Verify that a new GameEntity has been created with the correct player IDs
    Awaitility.await()
        .atMost(5, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
      var game = await(componentClient.forEventSourcedEntity(gameId)
          .method(GameEntity::getState)
          .invokeAsync());
      assertEquals(player1Id, game.firstPlayerId());
      assertEquals(player2Id, game.secondPlayerId());
    });

    logger.info("GameEntity created successfully with Game ID: {}", gameId);
  }
}
