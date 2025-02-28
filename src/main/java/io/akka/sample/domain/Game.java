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

    public enum Move {
        ROCK, PAPER, SCISSORS
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
        int rounds = Math.min(firstPlayerMoves.size(), secondPlayerMoves.size());

        for (int i = 0; i < rounds; i++) {
            Move firstPlayerMove = firstPlayerMoves.get(i);
            Move secondPlayerMove = secondPlayerMoves.get(i);

            if (firstPlayerMove == secondPlayerMove) {
                continue; // It's a tie for this round
            }

            switch (firstPlayerMove) {
                case ROCK:
                    if (secondPlayerMove == Move.SCISSORS) {
                        firstPlayerScore++;
                    } else {
                        secondPlayerScore++;
                    }
                    break;
                case PAPER:
                    if (secondPlayerMove == Move.ROCK) {
                        firstPlayerScore++;
                    } else {
                        secondPlayerScore++;
                    }
                    break;
                case SCISSORS:
                    if (secondPlayerMove == Move.PAPER) {
                        firstPlayerScore++;
                    } else {
                        secondPlayerScore++;
                    }
                    break;
            }

            if (firstPlayerScore == 2) {
                return Result.PLAYER_ONE_WINS;
            } else if (secondPlayerScore == 2) {
                return Result.PLAYER_TWO_WINS;
            }
        }

        return Result.IN_PROGRESS;
    }
}
