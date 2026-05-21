package nl.rug.oop.rts.model.command;

import nl.rug.oop.rts.model.observer.ModelEvent;
import nl.rug.oop.rts.model.observer.Observable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Two stacks of {@link Command} instances supporting Undo and Redo.
 * <p>
 * The class is itself {@link Observable} so that the Edit menu can grey
 * out the Undo/Redo entries whenever there is nothing to revert. The
 * history has a configurable cap to prevent unbounded memory growth.
 */
public class CommandHistory extends Observable {

    /** Default maximum number of recorded actions. */
    public static final int DEFAULT_CAPACITY = 100;

    /** Maximum number of commands to keep. */
    private final int capacity;

    /** Stack of commands that have been executed. */
    private final Deque<Command> undoStack = new ArrayDeque<>();

    /** Stack of commands available to redo. */
    private final Deque<Command> redoStack = new ArrayDeque<>();

    /** Default constructor with the default capacity. */
    public CommandHistory() {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Constructor with a custom capacity.
     *
     * @param capacity the maximum number of commands to remember; clamped to {@code >= 1}
     */
    public CommandHistory(int capacity) {
        this.capacity = Math.max(1, capacity);
    }

    /**
     * Executes the command and records it on the undo stack. Clears the
     * redo stack because the timeline has now diverged from any previously
     * undone path.
     *
     * @param command the command; ignored when {@code null}
     */
    public void execute(Command command) {
        if (command == null) {
            return;
        }
        command.execute();
        undoStack.push(command);
        if (undoStack.size() > capacity) {
            undoStack.pollLast();
        }
        redoStack.clear();
        notifyListeners(ModelEvent.Type.GENERIC);
    }

    /**
     * Reverts the most recently executed command.
     *
     * @return {@code true} if an undo happened
     */
    public boolean undo() {
        if (undoStack.isEmpty()) {
            return false;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
        notifyListeners(ModelEvent.Type.GENERIC);
        return true;
    }

    /**
     * Re-applies the most recently undone command.
     *
     * @return {@code true} if a redo happened
     */
    public boolean redo() {
        if (redoStack.isEmpty()) {
            return false;
        }
        Command command = redoStack.pop();
        command.execute();
        undoStack.push(command);
        notifyListeners(ModelEvent.Type.GENERIC);
        return true;
    }

    /**
     * Whether an undo is currently possible.
     *
     * @return {@code true} when the undo stack is non-empty
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * Whether a redo is currently possible.
     *
     * @return {@code true} when the redo stack is non-empty
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * Clears both stacks. Useful after a destructive operation that cannot
     * be safely undone.
     */
    public void clear() {
        undoStack.clear();
        redoStack.clear();
        notifyListeners(ModelEvent.Type.GENERIC);
    }
}
