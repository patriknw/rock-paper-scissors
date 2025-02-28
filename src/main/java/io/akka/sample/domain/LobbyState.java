package io.akka.sample.domain;

import java.util.Optional;

/**
 * Represents the state of a lobby with two player slots and a game ID.
 */
public record LobbyState(Optional<String> player1Id, Optional<String> player2Id, String gameId) {

    public LobbyState(String gameId) {
        this(Optional.empty(), Optional.empty(), gameId);
    }

    public LobbyState withPlayer1(String player1Id) {
        return new LobbyState(Optional.of(player1Id), player2Id, gameId);
    }

    public LobbyState withPlayer2(String player2Id) {
        return new LobbyState(player1Id, Optional.of(player2Id), gameId);
    }
}
