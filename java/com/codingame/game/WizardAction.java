package com.codingame.game;

public class WizardAction {
    public String action1, action2;
    public WizardAction(String action1, String action2) {
        this.action1 = action1;
        this.action2 = action2;
    }

    @Override
    public int hashCode() {
        String combinedAction = action1 + action2;
        return combinedAction.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return this.hashCode() == obj.hashCode();
    }
}

