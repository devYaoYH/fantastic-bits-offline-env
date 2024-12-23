package com.codingame.agent;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;
import java.time.Duration;

import com.codingame.game.GameNode;
import com.codingame.game.WizardAction;


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {

    // Random number generater has static seed for testing.
    static Random rand = new Random(0);
    // Ablation coefficient, agent takes a random action with probability 1-RATIONALITY_COEFF.
    static Double RATIONALITY_COEFF = 1.0;
    // If > 0, a rollout is performed with the heuristic state score
    // taken as reward and discounted by GAMMA each turn.
    static int ROLLOUT_DEPTH = 0;
    // The expected value is returned, so an average over the following
    // number of rollouts.
    static int ROLLOUT_WIDTH = 10;
    // Future reward discounting factor. We're really uncertain about our
    // future hence the low gamma value.
    static Double GAMMA = 0.7;

    /* Heuristics:
     *   1. distance of wizards to snaffles
     *   2. distance of snaffles to goal
     *   3. distance of bludgers to wizards
     *   4. magic score
     *   5. score score
     */
    // Best hand-tuned parameters so far:
    // 2, 800, 0.1, 1, 1000 (~78.13% win-rate against heuristic in 32 games)
    private static List<Double> geneticParams = List.of(2.0, 800.0, 0.1, 1.0, 1000.0);

    // Searching strategy, simultaneous or decoupled
    private static Boolean simultaneousSearching = false;

    // Instrumentation function
    public static void markTime(long startTime) {
        long endTime = System.nanoTime();

        long durationInNano = endTime - startTime;
        long durationInMillis = Duration.ofNanos(durationInNano).toMillis();

        System.err.println("Time taken: " + durationInMillis + " milliseconds");
    }


    private static double rollout(GameNode game, int depth) throws IOException {
        // System.err.println(String.format("========== ROLLOUT DEPTH: %d | Player: %b ==========", depth, game.isOptimizingPlayerTurn()));
        if (depth < 0) {
            return -1;
        }
        if (depth == 0) {
            // Score state instead.
            return game.getScore(geneticParams);
        }
        Double curScore = game.getScore(geneticParams);
        WizardAction randomAction = game.getRandomAction();
        // System.err.println(String.format("[Player] rollout actions: %s", String.join("\n", actionStrings)));
        try {
            game.takeAction(randomAction);
        }
        catch (RuntimeException e) {
            // This means the game is over
            return game.getScore(geneticParams);
        }
        // System.err.println(String.format(" [Player] taken action: %s,%s", actions.get(0).action1, actions.get(0).action2));
        return curScore + GAMMA * rollout(game, depth-1);
    }

    static class TreeNode {
        private GameNode game;
        private Double score;
        public TreeNode(GameNode game, Double score) {
            this.game = game;
            this.score = score;
        }
        public Double getScore() {
            return score;
        }
        public WizardAction getActions() {
            return game.getInitialAction();
        }
    }

    static Comparator<TreeNode> gameTreeComparator = (t1, t2) -> {
        double t1Score = t1.getScore();
        double t2Score = t2.getScore();
        if (t1Score > t2Score) return 1;
        else if (t1Score < t2Score) return -1;
        else return 0;
    };

    private static TreeNode forwardSearch(GameNode game, int depth, boolean isForFirstPod, boolean isSimultaneous) throws IOException {
        // System.err.println(String.format("========== FORWARD SEARCH DEPTH: %d | Player: %b | Team: %d ==========", depth, game.isOptimizingPlayerTurn(), game.getTeamId()));
        if (depth == 0) {
            // Return the current node
            if (ROLLOUT_DEPTH == 0) {
                return new TreeNode(game, game.getScore(geneticParams));
            }
            else {
                Double totScore = 0.0;
                for (int i = 0; i < ROLLOUT_WIDTH; ++i) {
                    GameNode newNode = game;
                    try {
                        newNode = game.copy();
                    }
                    catch (RuntimeException e) {
                        totScore += game.getScore(geneticParams);
                        continue;
                    }
                    Double score = rollout(newNode, ROLLOUT_DEPTH);
                    totScore += score;
                    // System.err.println(String.format("[ForwardSearch] leaf node score: %.3f", score));
                }
                return new TreeNode(game, totScore / ROLLOUT_WIDTH);
            }
        }
        List<WizardAction> actions = new ArrayList<>();
        // If we are the enemy, select a random action || We're rational up to the coeff degree
        if (!game.isOptimizingPlayerTurn() || rand.nextDouble() > RATIONALITY_COEFF) {
            actions = List.of(game.getRandomAction());
        }
        else {
            actions = isSimultaneous ? game.getActions() : game.getActions(isForFirstPod);
        }
        // System.err.println(String.format("  Number of actions generated: %d", actions.size()));
        // Branch out into further actions
        List<TreeNode> children = new ArrayList<>();
        for (WizardAction wa : actions) {
            GameNode newNode;
            try {
                newNode = game.copy();
            }
            catch (RuntimeException e) {
                return new TreeNode(game, game.getScore(geneticParams));
            }
            try {
                newNode.takeAction(wa);
            }
            catch (RuntimeException e) {
                // This means the game is over
                return new TreeNode(newNode, newNode.getScore(geneticParams));
            }
            // Only decrease depth if we get through a complete turn
            if ((newNode.isOptimizingPlayerTurn() && newNode.getTeamId() == 0) ||
                (!newNode.isOptimizingPlayerTurn() && newNode.getTeamId() == 1)) {
                // System.err.println("  New round reached.");
                children.add(forwardSearch(newNode, depth-1, isForFirstPod, isSimultaneous));
            }
            else {
                children.add(forwardSearch(newNode, depth, isForFirstPod, isSimultaneous));
            }
        }
        return children.stream().max(gameTreeComparator).get();
    }

    public static void main(String args[]) throws IOException {

        for (String arg : args) {
            if (arg.startsWith("-params=")) {
                geneticParams = new ArrayList<>();
                String[] params = arg.substring(8).split(",");
                for (String param : params) {
                    geneticParams.add(Double.parseDouble(param));
                }
                System.err.println("Genome: " + String.join(", ", geneticParams.stream().map(d -> d.toString()).toList()));
            }
            if (arg.startsWith("-r=")) {
                String value = arg.substring(3);
                RATIONALITY_COEFF = Double.parseDouble(value);
                System.err.println("Rationality: " + String.format("%.2f", RATIONALITY_COEFF));
            }
            if (arg.startsWith("-simul=")) {
                String value = arg.substring(7);
                simultaneousSearching = value.equals("true");
                System.err.println(String.format("Rationality: %b", simultaneousSearching));
            }
        }

        Scanner in = new Scanner(System.in);
        int myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        int turnNumber = 0;

        // game loop
        while (true) {
            turnNumber++;
            StringBuilder gameState = new StringBuilder();
            List<String> myWizards = new ArrayList<>();
            List<String> opponentWizards = new ArrayList<>();
            List<String> turnEntities = new ArrayList<>();
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
                // Append into current gameState string to update simulator.
                if (entityType.equals("WIZARD")) {
                    myWizards.add(String.format("%d %s %d %d %d %d %d\n",
                        entityId,
                        entityType,
                        x, y, vx, vy, state));
                }
                else if (entityType.equals("OPPONENT_WIZARD")) {
                    opponentWizards.add(String.format("%d %s %d %d %d %d %d\n",
                        entityId,
                        entityType,
                        x, y, vx, vy, state));
                }
                else {
                    turnEntities.add(String.format("%d %s %d %d %d %d %d\n",
                        entityId,
                        entityType,
                        x, y, vx, vy, state));
                }
            }

            // Always format playerIdx 0 stats first.
            if (myTeamId == 0) {
                gameState.append(String.format("%d %d %d %d\n", myScore, myMagic, opponentScore, opponentMagic));
                for (String wizard: myWizards) {
                    gameState.append(wizard);
                }
                for (String wizard : opponentWizards) {
                    gameState.append(wizard);
                }
            }
            else {
                gameState.append(String.format("%d %d %d %d\n", opponentScore, opponentMagic, myScore, myMagic));
                for (String wizard : opponentWizards) {
                    gameState.append(wizard);
                }
                for (String wizard: myWizards) {
                    gameState.append(wizard);
                }
            }
            for (String entity : turnEntities) {
                gameState.append(entity);
            }

            System.err.println(String.format("Finished processing turn %d", turnNumber));

            long startTime = System.nanoTime();

            GameNode init = new GameNode(myTeamId);
            init.initializeSimulator(List.of(gameState.toString().split("\n")));
            // double score = rollout(init, 3);
            // System.err.println(String.format("Rollout Score: %.3f", score));
            WizardAction optimalAction;
            if (simultaneousSearching) {
                optimalAction = forwardSearch(init, 1, false, true).getActions();
            }
            else {
                WizardAction optimal1Action = forwardSearch(init, 1, true, false).getActions();
                WizardAction optimal2Action = forwardSearch(init, 1, false, false).getActions();
                optimalAction = new WizardAction(optimal1Action.action1, optimal2Action.action2);
            }
            markTime(startTime);

            System.out.println(optimalAction.action1);
            System.out.println(optimalAction.action2);
        }
    }
}
