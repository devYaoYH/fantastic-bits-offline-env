package com.codingame.game;

import java.io.*;
import java.util.*;


public class GameNode {
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

