package com.codingame.agent;

import java.util.*;
import java.io.*;
import java.math.*;

import com.codingame.game.Simulator;


class GameNode {
    private int teamId;
    private Simulator simulator;
    private PipedInputStream input;
    private PipedOutputStream commandInput;
    private StringWriter sw;
    private PrintWriter simulOutput;

    public GameNode(int teamId) throws IOException {
        this.teamId = teamId;
        // Two pairs of streams for communicating with game simulation.
        // input, commandInput --> For passing into the simulator commands.
        this.input = new PipedInputStream();
        this.commandInput = new PipedOutputStream(this.input);
        // simulOutput --> For reading simulator outputs.
        this.sw = new StringWriter();
        this.simulOutput = new PrintWriter(sw);
        this.simulator = new Simulator(this.input, this.simulOutput, System.err);
    }

    public void initializeSimulator() throws IOException {
        // Initialize simulation
        this.commandInput.write("[[INIT] 1]\n".getBytes());
        this.commandInput.write("2\n".getBytes());
        this.simulator.processInput();
        this.commandInput.write("[[GET_GAME_INFO] 0]\n".getBytes());
        this.simulator.processInput();
        int numLinesRead = 0;
        while ((numLinesRead = readSimulatorOutput()) <= 0) {
            System.err.println("[Simulator] waiting to receive lines...");
        }
    }

    // This function blocks until a non-zero number of lines are read.
    private int readSimulatorOutput() throws IOException {
        Scanner s = new Scanner(this.sw.toString());
        int numLinesRead = 0;
        while (s.hasNextLine()) {
            String line = s.nextLine();
            System.err.println("[Simulator] read line: " + line);
            numLinesRead++;
        }
        this.sw.getBuffer().setLength(0);
        return numLinesRead;
    }
}


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {


    public static void main(String args[]) throws IOException {

        Scanner in = new Scanner(System.in);
        int myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        int turnNumber = 0;

        // game loop
        while (true) {
            turnNumber++;
            int myScore = in.nextInt();
            int myMagic = in.nextInt();
            int opponentScore = in.nextInt();
            int opponentMagic = in.nextInt();
            int entities = in.nextInt(); // number of entities still in game
            for (int i = 0; i < entities; i++) {
                int entityId = in.nextInt(); // entity identifier
                String entityType = in.next(); // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
                int x = in.nextInt(); // position
                int y = in.nextInt(); // position
                int vx = in.nextInt(); // velocity
                int vy = in.nextInt(); // velocity
                int state = in.nextInt(); // 1 if the wizard is holding a Snaffle, 0 otherwise
            }

            System.err.println(String.format("Finished processing turn %d", turnNumber));

            GameNode init = new GameNode(myTeamId);
            init.initializeSimulator();

            for (int i = 0; i < 2; i++) {

                // Write an action using System.out.println()
                // To debug: System.err.println("Debug messages...");
                System.err.println(String.format("moving wizard %d", i));

                // Edit this line to indicate the action for each wizard (0 ≤ thrust ≤ 150, 0 ≤ power ≤ 500)
                // i.e.: "MOVE x y thrust" or "THROW x y power"
                System.out.println("MOVE 8000 3750 100");
            }
        }
    }
}
