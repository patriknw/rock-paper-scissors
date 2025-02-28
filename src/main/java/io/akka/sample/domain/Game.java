package io.akka.sample.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record Game(
    String firstPlayerId,
    Optional<String> secondPlayerId,
    List<Move> firstPlayerMoves,
    List<Move> secondPlayerMoves
) {
    private static final int WINNING_SCORE = 2;

    public Game(String firstPlayerId, Optional<String> secondPlayerId) {
        this(firstPlayerId, secondPlayerId, new ArrayList<>(), new ArrayList<>());
    }

    public Game addMove(String playerId, Move move) {
        List<Move> newFirstPlayerMoves = new ArrayList<>(firstPlayerMoves);
        List<Move> newSecondPlayerMoves = new ArrayList<>(secondPlayerMoves);

        if (playerId.equals(firstPlayerId)) {
            newFirstPlayerMoves.add(move);
        } else if (secondPlayerId.isPresent() && playerId.equals(secondPlayerId.get())) {
            newSecondPlayerMoves.add(move);
        }

        return new Game(firstPlayerId, secondPlayerId, newFirstPlayerMoves, newSecondPlayerMoves);
    }

    public List<Move> getFirstPlayerMoves() {
        return Collections.unmodifiableList(firstPlayerMoves);
    }

    public List<Move> getSecondPlayerMoves() {
        return Collections.unmodifiableList(secondPlayerMoves);
    }

    public int completedRounds() {
        return Math.min(firstPlayerMoves.size(), secondPlayerMoves.size());
    }

    private record Scores(int firstPlayerScore, int secondPlayerScore) {}

    private Scores calculateScores() {
        if (secondPlayerId.isEmpty()) {
            return new Scores(0, 0);
        }

        int firstPlayerScore = 0;
        int secondPlayerScore = 0;
        int rounds = completedRounds();

        for (int i = 0; i < rounds; i++) {
            Move firstPlayerMove = firstPlayerMoves.get(i);
            Move secondPlayerMove = secondPlayerMoves.get(i);

            if (firstPlayerMove == secondPlayerMove) {
                continue; // It's a tie for this round
            }

            if (firstPlayerMove.beats(secondPlayerMove)) {
                firstPlayerScore++;
            } else {
                secondPlayerScore++;
            }
        }

        return new Scores(firstPlayerScore, secondPlayerScore);
    }

    public int getFirstPlayerScore() {
        return calculateScores().firstPlayerScore();
    }

    public int getSecondPlayerScore() {
        return calculateScores().secondPlayerScore();
    }

    public enum Move {
        ROCK, PAPER, SCISSORS;
    
        public boolean beats(Move other) {
            return switch (this) {
                case ROCK -> other == SCISSORS;
                case PAPER -> other == ROCK;
                case SCISSORS -> other == PAPER;
            };
        }
    }

    public enum Result {
        IN_PROGRESS, PLAYER_ONE_WINS, PLAYER_TWO_WINS
    }

    public Result evaluateWinner() {
        if (secondPlayerId.isEmpty()) {
            return Result.IN_PROGRESS;
        }

        int firstPlayerScore = 0;
        int secondPlayerScore = 0;
        int rounds = completedRounds();

        for (int i = 0; i < rounds; i++) {
            Move firstPlayerMove = firstPlayerMoves.get(i);
            Move secondPlayerMove = secondPlayerMoves.get(i);

            if (firstPlayerMove.beats(secondPlayerMove)) {
                firstPlayerScore++;
            } else if (secondPlayerMove.beats(firstPlayerMove)) {
                secondPlayerScore++;
            }

            if (firstPlayerScore == WINNING_SCORE) {
                return Result.PLAYER_ONE_WINS;
            } else if (secondPlayerScore == WINNING_SCORE) {
                return Result.PLAYER_TWO_WINS;
            }
        }

        return Result.IN_PROGRESS;
    }
}
