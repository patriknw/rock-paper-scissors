package io.akka.sample.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static io.akka.sample.domain.Game.Move.*;
import static io.akka.sample.domain.Game.Result.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GameTest {

    private Game game;

    @BeforeEach
    public void setup() {
        game = new Game("player1", Optional.of("player2"));
    }

    @Test
    public void testRockBeatsScissors() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", SCISSORS);
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", SCISSORS);
        assertEquals(PLAYER_ONE_WINS, game.evaluateWinner());
    }

    @Test
    public void testScissorsBeatsPaper() {
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", PAPER);
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", PAPER);
        assertEquals(PLAYER_ONE_WINS, game.evaluateWinner());
    }

    @Test
    public void testPaperBeatsRock() {
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", ROCK);
        assertEquals(PLAYER_ONE_WINS, game.evaluateWinner());
    }

    @Test
    public void testTie() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", PAPER);
        assertEquals(IN_PROGRESS, game.evaluateWinner());
    }

    @Test
    public void testPlayer1WinsAfterTwoRounds() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", SCISSORS);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", ROCK);
        assertEquals(PLAYER_ONE_WINS, game.evaluateWinner());
    }

    @Test
    public void testPlayer2WinsAfterTwoRounds() {
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", SCISSORS);
        assertEquals(PLAYER_TWO_WINS, game.evaluateWinner());
    }

    @Test
    public void testPlayer1WinsAfterThreeRounds() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", SCISSORS);
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", ROCK);
        assertEquals(PLAYER_ONE_WINS, game.evaluateWinner());
    }

    @Test
    public void testPlayer2WinsAfterThreeRounds() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", PAPER);
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", SCISSORS);
        assertEquals(PLAYER_TWO_WINS, game.evaluateWinner());
    }

    @Test
    public void testTieAfterThreeRounds() {
        game = game.addMove("player1", ROCK);
        game = game.addMove("player2", SCISSORS);
        game = game.addMove("player1", SCISSORS);
        game = game.addMove("player2", ROCK);
        game = game.addMove("player1", PAPER);
        game = game.addMove("player2", PAPER);
        assertEquals(IN_PROGRESS, game.evaluateWinner());
    }
}
