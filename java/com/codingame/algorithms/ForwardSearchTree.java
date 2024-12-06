package com.codingame.algorithms;

import com.codingame.game.GameNode;

import java.util.ArrayList;

class ForwardSearchTree extends GameTree {
    public int depth;
    public float reward;
    public Action bestAction;
    public Function<GameState, float> valueFunction;

    public ForwardSearchTree(GameNode game, int depth, float reward, Function<GameState, float> valueFunction) {
        this.depth = depth;
        this.reward = reward;
        this.valueFunction = valueFunction;
        
        this.bestAction = null;

        super(game);
    }
    
    public ForwardSearchTree(GameNode game, int depth, float reward) {
        this.depth = depth;
        this.reward = reward;
        this.valueFunction = state -> 0;
        
        this.bestAction = null;

        super(game);        
    }

    public ForwardSearchTree(GameNode game, int depth) {
        this.depth = depth;
        this.reward = 0;
        this.valueFunction = state -> 0;
        
        this.bestAction = null;
        
        super(game);
    }
    
    public float forward() {
        children = new ArrayList();
        bestAction = null;

        if (!depth)
            return valueFunction(game.getState());

        float maxUtility = Float.NEGATIVE_INFINITY; 
        for (Action action : game.getPlayerActions()) {
            ForwardSearchTree simulatedGame = game.copy();
            float simualtedReward = simulatedGame.takeAction(action);
            float utility = simualtedReward + simulatedGame.forward();
            children.add(GameTree(simulatedGame, depth - 1, simualtedReward));

            if (utility > maxUtility) {
                maxUtility = utility;
                bestAction = action;
            }
        }
        
        return maxUtility;
    }
}