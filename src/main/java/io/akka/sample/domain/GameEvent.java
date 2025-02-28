package io.akka.sample.domain;

import io.akka.sample.domain.Game.Move;
import akka.javasdk.annotations.TypeName;

sealed public interface GameEvent {

    @TypeName("game-started")
    record GameStarted(String player1Id, String player2Id) implements GameEvent {}

    @TypeName("move-made")
    record MoveMade(String playerId, Move move) implements GameEvent {}

    @TypeName("game-over")
    record GameOver(String winnerId) implements GameEvent {}
}
