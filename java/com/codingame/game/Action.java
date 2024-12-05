package com.codingame.game;

public class Action {
    public static enum Type {
        MOVE, THROW, OBLIVIATE, PETRIFICUS, ACCIO, FLIPENDO
    }
    protected Type actionType;
    protected int[] args;

    public Action(Type actionType, int... args) {
        this.actionType = actionType;
        this.args = args;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(actionType.name());
        for (int arg : args) {
            sb.append(" ").append(arg);
        }
        return sb.toString();
    }
}

