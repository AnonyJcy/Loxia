package com.cy.loxia.ui;

/**
 * Java-compatible callback interfaces for DialogManager
 */
public class DialogCallbacks {
    public interface OnConfirm {
        void onConfirm();
    }

    public interface OnSelect<T> {
        void onSelect(T value);
    }

    public interface OnAction {
        void onAction(String action);
    }
}
