package io.akka.sample.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a player with their game statistics and history.
 */
public record Player(
    String id,
    String name,
    int gamesWon,
    int gamesLost,
    List<String> recentGameIds
) {
  private static final int MAX_GAME_HISTORY = 10;

  /**
   * Creates a new player with initial statistics.
   */
  public Player(String id, String name) {
    this(id, name, 0, 0, new ArrayList<>());
  }

  /**
   * Returns the total number of games played.
   */
  public int gamesPlayed() {
    return gamesWon + gamesLost;
  }

  /**
   * Checks if the player has already recorded statistics for the given game.
   */
  public boolean hasRecordedGame(String gameId) {
    return recentGameIds.contains(gameId);
  }

  /**
   * Returns a new Player instance with an incremented games won count and updated game history.
   * Only records the game if it hasn't been recorded before.
   */
  public Player incrementWins(String gameId) {
    if (hasRecordedGame(gameId)) {
      return this;
    }

    List<String> newGameIds = new ArrayList<>(recentGameIds);
    newGameIds.add(gameId);
    if (newGameIds.size() > MAX_GAME_HISTORY) {
      newGameIds.remove(0);
    }

    return new Player(
        id,
        name,
        gamesWon + 1,
        gamesLost,
        Collections.unmodifiableList(newGameIds)
    );
  }

  /**
   * Returns a new Player instance with an incremented games lost count and updated game history.
   * Only records the game if it hasn't been recorded before.
   */
  public Player incrementLosses(String gameId) {
    if (hasRecordedGame(gameId)) {
      return this;
    }

    List<String> newGameIds = new ArrayList<>(recentGameIds);
    newGameIds.add(gameId);
    if (newGameIds.size() > MAX_GAME_HISTORY) {
      newGameIds.remove(0);
    }

    return new Player(
        id,
        name,
        gamesWon,
        gamesLost + 1,
        Collections.unmodifiableList(newGameIds)
    );
  }
}
