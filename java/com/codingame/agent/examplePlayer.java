package com.codingame.agent;

import java.util.*;
import java.io.*;
import java.math.*;

import com.codingame.game.Action;
import com.codingame.game.GameNode;
import com.codingame.game.WizardAction;


/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {


    private static void rollout(GameNode game, int depth) throws IOException, RuntimeException {
        if (depth <= 0) {
            return;
        }
        List<WizardAction> actions = game.getActions();
        if (actions.size() > 0) {
            game.takeAction(actions.get(0));
        }
        rollout(game, depth-1);
    }

    public static void main(String args[]) throws IOException {

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

            GameNode init = new GameNode(myTeamId);
            init.initializeSimulator(List.of(gameState.toString().split("\n")));
            rollout(init, 3);

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
