package io.akka.sample.application;

import akka.javasdk.annotations.ComponentId;
import akka.javasdk.annotations.Consume;
import akka.javasdk.annotations.Query;
import akka.javasdk.view.TableUpdater;
import akka.javasdk.view.View;
import io.akka.sample.domain.Player;

import java.util.List;

@ComponentId("leaderboard_view")
public class LeaderboardView extends View {

  public record PlayerStats(
      String playerId,
      String playerName,
      int gamesWon,
      int gamesLost,
      double score
  ) {
    public static PlayerStats fromPlayer(Player player) {
      return new PlayerStats(
          player.id(),
          player.name(),
          player.gamesWon(),
          player.gamesLost(),
          calculateScore(player)
      );
    }

    private static double calculateScore(Player player) {
      int totalGames = player.gamesPlayed();
      if (totalGames == 0) return 0.0;

      double winRate = (double) player.gamesWon() / totalGames;
      double gamesPlayedFactor = Math.min(1.0, totalGames / 10.0);
      return winRate * gamesPlayedFactor * 100.0;
    }
  }

  public record Leaderboard(List<PlayerStats> players) {
  }

  @Consume.FromKeyValueEntity(PlayerEntity.class)
  public static class LeaderboardUpdater extends TableUpdater<PlayerStats> {
    public Effect<PlayerStats> onState(Player player) {
      return effects().updateRow(PlayerStats.fromPlayer(player));
    }
  }

  @Query("SELECT * AS players FROM leaderboard_view ORDER BY score DESC LIMIT :maxResults")
  public QueryEffect<Leaderboard> getTopPlayers(int maxResults) {
    return queryResult();
  }

  @Query("SELECT * FROM leaderboard_view WHERE playerId = :playerId")
  public QueryEffect<PlayerStats> getPlayerStats(String playerId) {
    return queryResult();
  }
}
