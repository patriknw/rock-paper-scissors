package io.akka.sample.api;

import akka.http.javadsl.model.StatusCodes;
import akka.javasdk.testkit.TestKitSupport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class GameEndpointIntegrationTest extends TestKitSupport {
    private static final Logger logger = LoggerFactory.getLogger(GameEndpointIntegrationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testFullGameFlow() throws Exception {
        // Create Player 1
        var player1Request = new GameEndpoint.CreatePlayerRequest("player1", "Alice");
        var createPlayer1Response = await(httpClient.POST("/game/player")
            .withRequestBody(player1Request).invokeAsync());
        assertEquals(StatusCodes.CREATED, createPlayer1Response.status());

        // Create Player 2
        var player2Request = new GameEndpoint.CreatePlayerRequest("player2", "Bob");
        var createPlayer2Response = await(httpClient.POST("/game/player")
            .withRequestBody(player2Request).invokeAsync());
        assertEquals(StatusCodes.CREATED, createPlayer2Response.status());

        // Player 1 joins lobby
        var joinLobby1Request = new GameEndpoint.JoinLobbyRequest("player1");
        var joinLobby1Response = await(httpClient.POST("/game/lobby/lobby1/join")
            .withRequestBody(joinLobby1Request).invokeAsync());
        assertEquals(StatusCodes.OK, joinLobby1Response.status());

        // Player 2 joins lobby and retrieve game ID
        var joinLobby2Request = new GameEndpoint.JoinLobbyRequest("player2");
        var joinLobby2Response = await(httpClient.POST("/game/lobby/lobby1/join")
            .withRequestBody(joinLobby2Request).invokeAsync());
        assertEquals(StatusCodes.OK, joinLobby2Response.status());

        // Parse the game ID from the join lobby response
        String joinLobbyResponseBody = joinLobby2Response.body().utf8String();
        JsonNode joinLobbyJson = objectMapper.readTree(joinLobbyResponseBody);
        String gameId = joinLobbyJson.get("gameId").asText();

        // Wait for the game to start
        Awaitility.await()
            .atMost(5, TimeUnit.SECONDS)
            .ignoreExceptions()
            .untilAsserted(() -> {
                assertEquals(StatusCodes.OK, await(httpClient.GET("/game/" + gameId).invokeAsync()).status());
            });

        // Make moves until there is a winner
        for (int i = 0; i < 3; i++) {
            var move1Request = new GameEndpoint.MakeMoveRequest("player1", "ROCK");
            var move1Response = await(httpClient.POST("/game/" + gameId + "/move")
                .withRequestBody(move1Request).invokeAsync());
            assertEquals(StatusCodes.OK, move1Response.status());

            var move2Request = new GameEndpoint.MakeMoveRequest("player2", "SCISSORS");
            var move2Response = await(httpClient.POST("/game/" + gameId + "/move")
                .withRequestBody(move2Request).invokeAsync());
            assertEquals(StatusCodes.OK, move2Response.status());
        }

        // Get game state and verify winner
        var gameStateResponse = await(httpClient.GET("/game/" + gameId).invokeAsync());
        assertEquals(StatusCodes.OK, gameStateResponse.status());

        // Parse the response body to check for the winner
        String responseBody = gameStateResponse.body().utf8String();
        JsonNode gameStateJson = objectMapper.readTree(responseBody);
        String winnerId = gameStateJson.get("winnerId").asText();
        assertEquals("player1", winnerId);
    }
}
