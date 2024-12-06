package com.codingame.agent;

import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.math.*;
import java.time.Duration;

import com.codingame.game.Action;
import com.codingame.game.GameNode;
import com.codingame.game.WizardAction;


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {

    // Instrumentation function
    public static void markTime(long startTime) {
        long endTime = System.nanoTime();

        long durationInNano = endTime - startTime;
        long durationInMillis = Duration.ofNanos(durationInNano).toMillis();

        System.err.println("Time taken: " + durationInMillis + " milliseconds");
    }

    private static List<Double> geneticParams;

    private static double rollout(GameNode game, int depth) throws IOException {
        System.err.println(String.format("========== ROLLOUT DEPTH: %d | Player: %b ==========", depth, game.isOptimizingPlayerTurn()));
        if (depth < 0) {
            return -1;
        }
        if (depth == 0) {
            // Score state instead.
            return game.getScore(geneticParams);
        }
        List<WizardAction> actions = game.getActions();
        List<String> actionStrings = actions.stream()
                                            .map(a -> a.action1 + "|" + a.action2)
                                            .collect(Collectors.toList());
        // System.err.println(String.format("[Player] rollout actions: %s", String.join("\n", actionStrings)));
        if (actions.size() > 0) {
            try {
                game.takeAction(actions.get(0));
            }
            catch (RuntimeException e) {
                // This means the game is over
                return game.getScore(geneticParams);
            }
            System.err.println(String.format(" [Player] taken action: %s,%s", actions.get(0).action1, actions.get(0).action2));
        }
        GameNode gameCopy;
        try {
            gameCopy = game.copy();
        }
        catch (RuntimeException e) {
            return game.getScore(geneticParams);
        }
        return rollout(gameCopy, depth-1);
    }

    public static void main(String args[]) throws IOException {

        Scanner in = new Scanner(System.in);
        int myTeamId = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        int turnNumber = 0;

        geneticParams = List.of(1.0, 10.0, 0.5);

        for (String arg : args) {
            if (arg.startsWith("-params=")) {
                geneticParams = new ArrayList<>();
                String[] params = arg.substring(8).split(",");
                for (String param : params) {
                    geneticParams.add(Double.parseDouble(param));
                }
            }
        }


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
            double score = rollout(init, 3);
            System.err.println(String.format("Rollout Score: %.3f", score));

            markTime(startTime);

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
