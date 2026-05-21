package nl.rug.oop.rts.model.command;

/**
 * A reversible user action.
 * <p>
 * The Command pattern lets the application record actions on a stack and
 * undo/redo them without the controller layer having to know what
 * operation it is reverting. Each implementation captures the data it
 * needs to restore the previous state in its own fields, leaving the
 * history bookkeeping (push/pop, stack size limits) to
 * {@link CommandHistory}.
 */
public interface Command {

    /**
     * Performs the action.
     */
    void execute();

    /**
     * Reverts the action, restoring the previous state.
     */
    void undo();

    /**
     * Returns a short label suitable for display in tool tips or in the
     * edit menu.
     *
     * @return a human readable label
     */
    String label();
}
