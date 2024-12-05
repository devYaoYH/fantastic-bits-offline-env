package com.codingame.algorithms;

import com.codingame.game.GameNode;

import java.util.ArrayList;

class GameTree {
    public GameNode game;
    public ArrayList<GameTree> children;
    
    public GameTree(GameNode game) {
        this.game = game;
        children = new ArrayList<GameTree>();
    }
}