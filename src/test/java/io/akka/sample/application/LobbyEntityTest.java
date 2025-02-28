package io.akka.sample.application;

import akka.javasdk.testkit.KeyValueEntityTestKit;
import io.akka.sample.domain.LobbyState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LobbyEntityTest {

    private KeyValueEntityTestKit<LobbyState, LobbyEntity> testKit;

    @BeforeEach
    public void setup() {
        testKit = KeyValueEntityTestKit.of(LobbyEntity::new);
    }

    @Test
    public void testJoinLobbyFirstPlayer() {
        var playerId = "player1";
        var result = testKit.call(entity -> entity.joinLobby(playerId));

        var state = result.getReply();
        assertEquals(Optional.of(playerId), state.player1Id());
        assertTrue(state.player2Id().isEmpty());
    }

    @Test
    public void testJoinLobbySecondPlayer() {
        testKit.call(entity -> entity.joinLobby("player1"));
        var result = testKit.call(entity -> entity.joinLobby("player2"));

        var state = result.getReply();
        assertEquals(Optional.of("player1"), state.player1Id());
        assertEquals(Optional.of("player2"), state.player2Id());
    }

    @Test
    public void testJoinLobbyFull() {
        testKit.call(entity -> entity.joinLobby("player1"));
        testKit.call(entity -> entity.joinLobby("player2"));
        var initialState = testKit.getState();
        var initialGameId = initialState.gameId();

        var result = testKit.call(entity -> entity.joinLobby("player3"));

        var newState = result.getReply();
        assertEquals(Optional.of("player3"), newState.player1Id());
        assertTrue(newState.player2Id().isEmpty());
        assertNotEquals(initialGameId, newState.gameId());
    }

    @Test
    public void testGetLobby() {
        testKit.call(entity -> entity.joinLobby("player1"));
        var result = testKit.call(LobbyEntity::getLobby);

        var state = result.getReply();
        assertEquals(Optional.of("player1"), state.player1Id());
        assertTrue(state.player2Id().isEmpty());
    }
}
