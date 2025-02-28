package io.akka.sample.api;

import akka.http.javadsl.model.HttpResponse;
import akka.javasdk.annotations.Acl;
import akka.javasdk.annotations.http.HttpEndpoint;
import akka.javasdk.annotations.http.Get;
import akka.javasdk.annotations.http.Post;
import akka.javasdk.client.ComponentClient;
import akka.javasdk.http.HttpResponses;
import io.akka.sample.application.GameEntity;
import io.akka.sample.application.LeaderboardView;
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
        Optional<String> secondPlayerId,
        List<String> firstPlayerMoves,
        List<String> secondPlayerMoves,
        int firstPlayerScore,
        int secondPlayerScore,
        int completedRounds,
        int firstPlayerMoveCount,
        int secondPlayerMoveCount,
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

    @Get("/{gameId}")
    public CompletionStage<GetGameStateResponse> getGameState(String gameId) {
        return componentClient.forEventSourcedEntity(gameId)
            .method(GameEntity::getState)
            .invokeAsync()
            .thenApply(game -> {
                Optional<String> winnerId = switch (game.evaluateWinner()) {
                    case PLAYER_ONE_WINS -> Optional.of(game.firstPlayerId());
                    case PLAYER_TWO_WINS -> game.secondPlayerId();
                    default -> Optional.empty();
                };
                int completedRounds = Math.min(game.getFirstPlayerMoves().size(), game.getSecondPlayerMoves().size());
                List<String> firstPlayerMoves = game.getFirstPlayerMoves().stream()
                    .limit(completedRounds)
                    .map(Move::name)
                    .toList();
                List<String> secondPlayerMoves = game.getSecondPlayerMoves().stream()
                    .limit(completedRounds)
                    .map(Move::name)
                    .toList();
                return new GetGameStateResponse(
                    game.firstPlayerId(),
                    game.secondPlayerId(),
                    firstPlayerMoves,
                    secondPlayerMoves,
                    game.getFirstPlayerScore(),
                    game.getSecondPlayerScore(),
                    completedRounds,
                    game.getFirstPlayerMoves().size(),
                    game.getSecondPlayerMoves().size(),
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

    @Get("/leaderboard")
    public CompletionStage<LeaderboardView.Leaderboard> getLeaderboard() {
        return componentClient.forView()
            .method(LeaderboardView::getTopPlayers)
            .invokeAsync(10);  // Hardcoded to always return top 10 players, could be a query parameter later
    }

    @Get("/leaderboard/player/{playerId}")
    public CompletionStage<LeaderboardView.PlayerStats> getPlayerStats(String playerId) {
        return componentClient.forView()
            .method(LeaderboardView::getPlayerStats)
            .invokeAsync(playerId);
    }
}
