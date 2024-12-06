package com.codingame.algorithms;

import com.codingame.game.GameNode;

import java.util.ArrayList;

class AlphaBetaSearchTree extends GameTree {
    public int depth;
    public float reward;
    public boolean opponent;

    public Action bestAction;
    public float alpha;
    public float beta;
    
    public Function<GameState, float> valueFunction;

    public AlphaBetaSearchTree(GameNode game, int depth, float reward, Function<GameState, float> valueFunction, boolean opponent) {
        this.depth = depth;
        this.reward = reward;
        this.valueFunction = valueFunction;
        this.opponent = opponent;
        
        this.bestAction = null;
        this.alpha = Float.NEGATIVE_INFINITY;
        this.beta = Float.POSITIVE_INFINITY;

        super(game);        
    }

    public AlphaBetaSearchTree(GameNode game, int depth, float reward, Function<GameState, float> valueFunction) {
        this.depth = depth;
        this.reward = reward;
        this.valueFunction = valueFunction;
        this.opponent = false;
        
        this.bestAction = null;
        this.alpha = Float.NEGATIVE_INFINITY;
        this.beta = Float.POSITIVE_INFINITY;

        super(game);        
    }
    
    public AlphaBetaSearchTree(GameNode game, int depth, float reward) {
        this.depth = depth;
        this.reward = reward;
        this.valueFunction = state -> 0;
        this.opponent = false;
        
        this.bestAction = null;
        this.alpha = Float.NEGATIVE_INFINITY;
        this.beta = Float.POSITIVE_INFINITY;

        super(game);        
    }

    public AlphaBetaSearchTree(GameNode game, int depth) {
        this.depth = depth;
        this.reward = 0;
        this.valueFunction = state -> 0;
        this.opponent = false;
        
        this.bestAction = null;
        this.alpha = Float.NEGATIVE_INFINITY;
        this.beta = Float.POSITIVE_INFINITY; 

        super(game);
    }
    
    public float alphaBeta() {
        children = new ArrayList();
        bestAction = null;
        alpha = Float.NEGATIVE_INFINITY;
        beta = Float.POSITIVE_INFINITY; 

        if (!depth)
            return valueFunction(game.getState());

        float maxUtility = opponent ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY; 
        for (Action action : game.getPlayerActions()) {;
            ForwardSearchTree simulatedGame = game.copy();
            float simualtedReward = simulatedGame.takeAction(action);
            float utility = simualtedReward + simulatedGame.alphaBeta();
            children.add(GameTree(simulatedGame, depth - 1, simualtedReward));

            if (!opponent) {
                if (utility > optimalUtility) {
                    optimalUtility = utility;
                    bestAction = action;
                
                    if (optimalUtility > beta)
                        break;

                    alpha = max(alpha, optimalUtility);
                }
            } else {
                if (utility < optimalUtility) {
                    optimalUtility = utility
                    bestAction = action;

                    if (optimalUtility < alpha) 
                        break;
                    
                    beta = min(beta, optimalUtility);
                }
            }
        }
        
        return optimalUtility;
    }
}