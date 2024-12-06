package com.codingame.game;

import java.io.*;
import java.lang.RuntimeException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GameNode {
    public static boolean DEPLOY = true;
    private int teamId;
    private int currentPlayerIdx;
    private Simulator simulator;
    private PipedInputStream input;
    private PipedOutputStream commandInput;
    private StringWriter sw;
    private PrintWriter simulOutput;
    private boolean isReadyForAction;
    private List<WizardAction> permissibleActions;
    private String gameState;
    private List<String> initialState;
    private List<WizardAction> actionSequence;

    private static final Pattern HEADER_PATTERN = Pattern.compile("\\[\\[(?<cmd>.+)\\] ?(?<lineCount>[0-9]+)\\]");

    /*
     * Define Heuristic Scoring Functions here.
     */
    private double nearestSnaffleHeuristic(Simulator simulator) {
        return simulator.nearestSnaffleHeuristic(teamId);
    }

    private double snaffleToGoalHeuristic(Simulator simulator) {
        return simulator.snaffleToGoalHeuristic(teamId);
    }

    private double bludgerToWizardHeuristic(Simulator simulator) {
        return simulator.bludgerToWizardHeuristic(teamId);
    }

    private List<Function<Simulator, Double>> heuristics;

    private static void logIfNotDeploy(String msg) {
        if (!DEPLOY) System.err.println(msg);
    }

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
        this.initialState = new ArrayList<>();
        this.actionSequence = new ArrayList<>();
        this.heuristics = new ArrayList<>();
        this.heuristics.add(s -> nearestSnaffleHeuristic(s));
        this.heuristics.add(s -> snaffleToGoalHeuristic(s));
        this.heuristics.add(s -> bludgerToWizardHeuristic(s));
    }

    public GameNode copy() throws IOException, RuntimeException {
        GameNode newNode = new GameNode(teamId);
        newNode.initializeSimulator(this.initialState);
        logIfNotDeploy(String.format("[GameNode] Copying Actions: %s", String.join(",", this.actionSequence.stream().map(w -> w.toString()).collect(Collectors.toList()))));
        for (WizardAction wa : this.actionSequence) {
            newNode.takeAction(wa);
        }
        return newNode;
    }

    public double getScore(List<Double> weights) {
        double totScore = 0.0;
        int heuIdx = 0;
        for (Function<Simulator, Double> heu : heuristics) {
            double weight = heuIdx >= weights.size() ? 1.0 : weights.get(heuIdx++);
            totScore += heu.apply(this.simulator) * weight;
        }
        return totScore;
    }

    // Formats and sends a command into the simulator.
    private void executeCommand(String command, List<String> data) throws IOException, RuntimeException {
        logIfNotDeploy(String.format("[GameNode] Sending command: %s", command));
        commandInput.write(String.format("[[%s] %d]\n", command, data.size()).getBytes());
        for (String line : data) {
            commandInput.write(String.format("%s\n", line).getBytes());
            // logIfNotDeploy(line);
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
            // logIfNotDeploy("[Simulator] read line: " + line);
            numLinesRead++;
        }
        this.sw.getBuffer().setLength(0);
        s.close();
        return numLinesRead;
    }

    // This function blocks until a non-zero number of lines are read.
    private String waitForSimulatorOutput() throws IOException {
        StringBuilder sb = new StringBuilder();
        int numLinesRead = 0;
        numLinesRead = readOutputBuffer(sb);
        // while ((numLinesRead = readOutputBuffer(sb)) <= 0) {
        //    logIfNotDeploy("[Simulator] waiting to receive lines...");
        //}
        return sb.toString();
    }

    public void initializeSimulator() throws IOException, RuntimeException {
        // Initialize simulation
        executeCommand("INIT", List.of("2"));
        // Make the game ready to be played
        step();
        this.currentPlayerIdx = 0;
    }

    public void initializeSimulator(List<String> gameState) throws IOException, RuntimeException {
        // We need to first initialize the structure
        executeCommand("INIT", List.of("2"));
        // Update simulator to another internal state
        executeCommand("UPDATE_INTERNAL_STATE", gameState);
        // Make the game ready to be played
        step();
        // If optimizing agent is playerIdx 1, we need to first simulate
        // opponent move.
        this.currentPlayerIdx = 0;
        this.initialState = gameState;
    }

    // Whether simulator is set to the optimizing player's turn.
    // Optimizing player means the player we're searching actions for.
    public boolean isOptimizingPlayerTurn() {
        return currentPlayerIdx == teamId;
    }

    public void step() throws IOException, RuntimeException {
        if (this.isReadyForAction) return;
        executeCommand("GET_GAME_INFO", Collections.emptyList());
        gameState = waitForSimulatorOutput();
        // Increment and modulo player index
        currentPlayerIdx = (currentPlayerIdx + 1) % 2;
        this.isReadyForAction = true;
    }

    // At least 1 action will be available since we always generate
    // MOVE in 16 directions * 2 thrust levels.
    private void generateActions() throws IOException, RuntimeException {
        if (permissibleActions.size() > 0) {
            return;
        }

        logIfNotDeploy("Attempting to generate next round actions");
        System.err.flush();

        // Step the game info
        String playerStatus = simulator.getPlayerStatus().toString();
        logIfNotDeploy(String.format("[GameNode] Player Status:\n%s", playerStatus));

        // Expect 2 lines from simulator.
        executeCommand("GET_CURRENT_PLAYER_WIZARD_ACTIONS", Collections.emptyList());
        String wizardLocations = waitForSimulatorOutput();

        Scanner s = new Scanner(wizardLocations);
        while (s.hasNextLine()) {
            String line = s.nextLine();
            Matcher m = HEADER_PATTERN.matcher(line);
            if (!m.matches()) throw new RuntimeException(String.format("Response from simulator for getAction does not match. %s", line));
            String cmd = m.group("cmd");
            int lineCount = Integer.parseInt(m.group("lineCount"));
            List<String> payload = new ArrayList<>();
            for (int i = 0; i < lineCount; ++i) payload.add(s.nextLine());
            if (cmd.equals("WIZARD_LOCATIONS")) {
                for (String action : payload) {
                    String[] actions = action.split(",");
                    WizardAction wa = new WizardAction(actions[0], actions[1]);
                    permissibleActions.add(wa);
                }
            }
        }
        s.close();
    }

    // Get available actions for the current player.
    public List<WizardAction> getActions() throws IOException, RuntimeException {
        generateActions();
        return new ArrayList<>(permissibleActions);
    }

    // Simulate one set of actions (both wizards).
    // This will mutate the internals of Simulator and GameNode.
    public double takeAction(WizardAction wa) throws IOException, RuntimeException {
        // To step the internals of the simulator correctly, we need to first ensure actions have been generated.
        if (!isReadyForAction) {
            throw new RuntimeException(
                "Current state of GameNode has not yet generated actions. " +
                "Internal simulator state may be inconsistent. " +
                "Did you forget to call getActions() first?");
        }
        // Rewards encode score + collisions.
        double reward = 0.0;
        executeCommand("SET_PLAYER_OUTPUT", List.of(wa.action1, wa.action2));
        /*
         * executeCommand("GET_RECENT_COLLISIONS", Collections.emptyList());
         * String collisions = waitForSimulatorOutput();
         * executeCommand("GET_SCORES", Collections.emptyList());
         * String scores = waitForSimulatorOutput();
         */
        this.actionSequence.add(wa);
        // We clear the list of actions after taking one as this means we're on to the next player
        permissibleActions = new ArrayList<>();
        isReadyForAction = false;
        step();
        return reward;
    }
}

