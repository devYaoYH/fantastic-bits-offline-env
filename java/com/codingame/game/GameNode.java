package com.codingame.game;

import java.io.*;
import java.lang.RuntimeException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GameNode {
    private int teamId;
    private int currentPlayerIdx;
    private Simulator simulator;
    private PipedInputStream input;
    private PipedOutputStream commandInput;
    private StringWriter sw;
    private PrintWriter simulOutput;
    private List<WizardAction> permissibleActions;
    private String gameState;

    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[\\[(?<cmd>.+)\\] ?(?<lineCount>[0-9]+)\\]");

    // Initialize from perspective of the optimizing player.
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
        this.permissibleActions = new ArrayList<>();
    }

    // Formats and sends a command into the simulator.
    private void executeCommand(String command, List<String> data) throws IOException {
        commandInput.write(String.format("[[%s] %d]\n", command, data.size()).getBytes());
        for (String line : data) {
            commandInput.write(String.format("%s\n", line).getBytes());
        }
        simulator.processInput();
    }

    // Consume the output buffer from the simulator.
    private int readOutputBuffer(StringBuilder sb) throws IOException {
        Scanner s = new Scanner(this.sw.toString());
        int numLinesRead = 0;
        while (s.hasNextLine()) {
            String line = s.nextLine();
            sb.append(line + "\n");
            System.err.println("[Simulator] read line: " + line);
            numLinesRead++;
        }
        this.sw.getBuffer().setLength(0);
        return numLinesRead;
    }

    // This function blocks until a non-zero number of lines are read.
    private String waitForSimulatorOutput() throws IOException {
        StringBuilder sb = new StringBuilder();
        int numLinesRead = 0;
        while ((numLinesRead = readOutputBuffer(sb)) <= 0) {
            System.err.println("[Simulator] waiting to receive lines...");
        }
        return sb.toString();
    }

    public void initializeSimulator() throws IOException {
        // Initialize simulation
        executeCommand("INIT", List.of("2"));
        this.currentPlayerIdx = 0;
    }

    public void initializeSimulator(List<String> gameState) throws IOException {
        // We need to first initialize the structure
        executeCommand("INIT", List.of("2"));
        // Update simulator to another internal state
        executeCommand("UPDATE_INTERNAL_STATE", gameState);
        // If optimizing agent is playerIdx 1, we need to first simulate
        // opponent move.
        this.currentPlayerIdx = 0;
    }

    // Whether simulator is set to the optimizing player's turn.
    // Optimizing player means the player we're searching actions for.
    public boolean isOptimizingPlayerTurn() {
        return currentPlayerIdx == teamId;
    }

    // At least 1 action will be available since we always generate
    // MOVE in 16 directions * 2 thrust levels.
    private void generateActions() throws IOException {
        if (permissibleActions.size() > 0) {
            return;
        }

        // Increment and modulo player index
        currentPlayerIdx = (currentPlayerIdx + 1) % 2;

        // Expect 2 lines from simulator.
        executeCommand("GET_CURRENT_PLAYER_WIZARD_ACTIONS", Collections.emptyList());
        String wizardLocations = waitForSimulatorOutput();

        System.err.println(String.format("[GameNode] Wizard actions:\n %s", wizardLocations));
    }

    // Get available actions for the current player.
    public List<WizardAction> getActions() throws IOException {
        generateActions();
        List<WizardAction> actions = new ArrayList<>();
        Collections.copy(actions, permissibleActions);
        return actions;
    }

    // Simulate one set of actions (both wizards).
    // This will mutate the internals of Simulator and GameNode.
    public double takeAction(WizardAction wa) throws IOException, RuntimeException {
        // To step the internals of the simulator correctly, we need to first ensure actions have been generated.
        if (permissibleActions.size() < 1) {
            throw new RuntimeException(
                "Current state of GameNode has not yet generated actions. " +
                "Internal simulator state may be inconsistent. " +
                "Did you forget to call getActions() first?");
        }
        // Rewards encode score + collisions.
        double reward = 0.0;
        executeCommand("SET_PLAYER_OUTPUT", List.of(wa.action1.toString(), wa.action2.toString()));
        if (currentPlayerIdx == 0) {
            gameState = waitForSimulatorOutput();
            System.err.println(String.format("[GameNode] Game state:\n%s", gameState));
        }
        /*
        executeCommand("GET_RECENT_COLLISIONS", Collections.emptyList());
        String collisions = waitForSimulatorOutput();
        executeCommand("GET_SCORES", Collections.emptyList());
        String scores = waitForSimulatorOutput();
        */
        // We clear the list of actions after taking one as this means we're on to the next player
        permissibleActions = new ArrayList<>();
        return reward;
    }
}

