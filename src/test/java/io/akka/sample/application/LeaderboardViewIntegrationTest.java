package io.akka.sample.application;

import akka.javasdk.testkit.TestKitSupport;
import akka.javasdk.testkit.EventingTestKit.IncomingMessages;
import akka.javasdk.testkit.TestKit;
import io.akka.sample.domain.Player;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class LeaderboardViewIntegrationTest extends TestKitSupport {

  private IncomingMessages playerUpdates;

  @Override
  protected TestKit.Settings testKitSettings() {
    return TestKit.Settings.DEFAULT
        .withKeyValueEntityIncomingMessages("player");
  }

  @BeforeEach
  public void setup() {
    playerUpdates = testKit.getKeyValueEntityIncomingMessages("player");
  }

  @Test
  public void testLeaderboardUpdatesWithPlayerStats() {
    // Create players with different stats
    var player1 = new Player("player1", "Alice");
    player1 = player1.incrementWins("game1").incrementWins("game2");  // 2 wins, 0 losses

    var player2 = new Player("player2", "Bob");
    player2 = player2.incrementWins("game3").incrementLosses("game4"); // 1 win, 1 loss

    var player3 = new Player("player3", "Charlie");
    player3 = player3.incrementLosses("game5").incrementLosses("game6"); // 0 wins, 2 losses

    // Publish player states
    playerUpdates.publish(player1, "player1");
    playerUpdates.publish(player2, "player2");
    playerUpdates.publish(player3, "player3");

    // Verify leaderboard order and scores
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          LeaderboardView.Leaderboard leaderboard = await(
              componentClient.forView()
                  .method(LeaderboardView::getTopPlayers)
                  .invokeAsync(10)
          );

          // The leaderboard contains players from other tests, so filter out the ones from other tests.
          var players = leaderboard.players().stream()
              .filter(p -> p.playerId().equals("player1") || p.playerId().equals("player2") || p.playerId().equals("player3"))
              .toList();
          assertEquals(3, players.size());

          // Player 1 should be first (2 wins, 0 losses)
          assertEquals("player1", players.get(0).playerId());
          assertEquals(2, players.get(0).gamesWon());
          assertEquals(0, players.get(0).gamesLost());
          assertTrue(players.get(0).score() > 0);

          // Player 2 should be second (1 win, 1 loss)
          assertEquals("player2", players.get(1).playerId());
          assertEquals(1, players.get(1).gamesWon());
          assertEquals(1, players.get(1).gamesLost());
          assertTrue(players.get(1).score() > 0);

          // Player 3 should be last (0 wins, 2 losses)
          assertEquals("player3", players.get(2).playerId());
          assertEquals(0, players.get(2).gamesWon());
          assertEquals(2, players.get(2).gamesLost());
          assertEquals(0.0, players.get(2).score());
        });
  }

  @Test
  public void testGetPlayerStats() {
    // Create and publish a player with some stats
    var player = new Player("player4", "David")
        .incrementWins("game7")
        .incrementWins("game8")
        .incrementLosses("game9");

    playerUpdates.publish(player, "player4");

    // Verify individual player stats
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          LeaderboardView.PlayerStats stats = await(
              componentClient.forView()
                  .method(LeaderboardView::getPlayerStats)
                  .invokeAsync("player4")
          );

          assertEquals("player4", stats.playerId());
          assertEquals("David", stats.playerName());
          assertEquals(2, stats.gamesWon());
          assertEquals(1, stats.gamesLost());
          assertTrue(stats.score() > 0);
        });
  }

  @Test
  public void testScoreCalculation() {
    // Create players with same win rate but different number of games
    var player1 = new Player("player5", "Eve");
    for (int i = 0; i < 10; i++) {
      player1 = player1.incrementWins("game" + (i + 10));  // 10 wins, 0 losses
    }

    var player2 = new Player("player6", "Frank");
    player2 = player2.incrementWins("game20");  // 1 win, 0 losses

    playerUpdates.publish(player1, "player5");
    playerUpdates.publish(player2, "player6");

    // Verify that player with more games has higher score despite same win rate
    Awaitility.await()
        .atMost(10, TimeUnit.SECONDS)
        .ignoreExceptions()
        .untilAsserted(() -> {
          LeaderboardView.Leaderboard leaderboard = await(
              componentClient.forView()
                  .method(LeaderboardView::getTopPlayers)
                  .invokeAsync(10)
          );

          // The leaderboard contains players from other tests, so filter out the ones from other tests.
          var players = leaderboard.players().stream()
              .filter(p -> p.playerId().equals("player5") || p.playerId().equals("player6"))
              .toList();
          assertEquals(2, players.size());
          assertEquals("player5", players.get(0).playerId());
          assertEquals("player6", players.get(1).playerId());
          assertTrue(players.get(0).score() > players.get(1).score());
        });
  }
}
