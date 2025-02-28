package io.akka.sample.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import io.akka.sample.application.GameEntity;
import io.akka.sample.application.LobbyEntity;
import io.akka.sample.application.PlayerEntity;
import io.akka.sample.domain.Game.Move;
import io.akka.sample.domain.Player;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

@Acl(allow = @Acl.Matcher(principal = Acl.Principal.INTERNET))
@HttpEndpoint("/game")
public class GameEndpoint {

    public record CreatePlayerRequest(String id, String name) {}
    public record GetPlayerResponse(String id, String name) {}
    public record JoinLobbyRequest(String playerId) {}
    public record JoinLobbyResponse(Optional<String> player1Id, Optional<String> player2Id, String gameId) {}
    public record GetGameStateResponse(
        String firstPlayerId,
        String secondPlayerId,
        List<String> firstPlayerMoves,
        List<String> secondPlayerMoves,
        Optional<String> winnerId
    ) {}
    public record MakeMoveRequest(String playerId, String move) {}

    private final ComponentClient componentClient;

    public GameEndpoint(ComponentClient componentClient) {
        this.componentClient = componentClient;
    }

    @Post("/player")
    public CompletionStage<HttpResponse> createPlayer(CreatePlayerRequest request) {
        return componentClient.forKeyValueEntity(request.id())
            .method(PlayerEntity::createPlayer)
            .invokeAsync(new Player(request.id(), request.name()))
            .thenApply(__ -> HttpResponses.created());
    }

    @Get("/player/{playerId}")
    public CompletionStage<GetPlayerResponse> getPlayer(String playerId) {
        return componentClient.forKeyValueEntity(playerId)
            .method(PlayerEntity::getPlayer)
            .invokeAsync()
            .thenApply(playerState -> new GetPlayerResponse(playerState.id(), playerState.name()));
    }

    @Post("/lobby/{lobbyId}/join")
    public CompletionStage<JoinLobbyResponse> joinLobby(String lobbyId, JoinLobbyRequest request) {
        return componentClient.forKeyValueEntity(lobbyId)
            .method(LobbyEntity::joinLobby)
            .invokeAsync(request.playerId())
            .thenApply(lobbyState -> new JoinLobbyResponse(lobbyState.player1Id(), lobbyState.player2Id(), lobbyState.gameId()));
    }

    @Get("/lobby/{lobbyId}")
    public CompletionStage<JoinLobbyResponse> getLobbyState(String lobbyId) {
        return componentClient.forKeyValueEntity(lobbyId)
            .method(LobbyEntity::getLobby)
            .invokeAsync()
            .thenApply(lobbyState -> new JoinLobbyResponse(lobbyState.player1Id(), lobbyState.player2Id(), lobbyState.gameId()));
    }

    @Get("/{gameId}")
    public CompletionStage<GetGameStateResponse> getGameState(String gameId) {
        return componentClient.forEventSourcedEntity(gameId)
            .method(GameEntity::getState)
            .invokeAsync()
            .thenApply(game -> {
                Optional<String> winnerId = switch (game.evaluateWinner()) {
                    case PLAYER_ONE_WINS -> Optional.of(game.firstPlayerId());
                    case PLAYER_TWO_WINS -> Optional.of(game.secondPlayerId());
                    default -> Optional.empty();
                };
                return new GetGameStateResponse(
                    game.firstPlayerId(),
                    game.secondPlayerId(),
                    game.getFirstPlayerMoves().stream().map(Move::name).toList(),
                    game.getSecondPlayerMoves().stream().map(Move::name).toList(),
                    winnerId
                );
            });
    }

    @Post("/{gameId}/move")
    public CompletionStage<HttpResponse> makeMove(String gameId, MakeMoveRequest request) {
        return componentClient.forEventSourcedEntity(gameId)
            .method(GameEntity::makeMove)
            .invokeAsync(new GameEntity.MoveRequest(request.playerId(), Move.valueOf(request.move())))
            .thenApply(__ -> HttpResponses.ok());
    }
}
