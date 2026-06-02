package nl.rug.oop.rts.controller.action;

import nl.rug.oop.rts.controller.EditorContext;

import java.awt.event.ActionEvent;
import java.util.Random;

/**
 * Concrete {@link ModelBoundAction}s wired to the menu bar buttons.
 * <p>
 * Each inner class encapsulates the precondition logic for one toolbar
 * action. Keeping them together in one file makes it easy to scan the
 * full button vocabulary of the editor in a single place; each class
 * is still its own bounded unit, so the cohesion principle is upheld.
 */
public final class EditorActions {

    /**
     * Prevents instantiation of this action holder.
     */
    private EditorActions() {
        throw new AssertionError("Utility holder");
    }

    /** "Add Node" action. Always enabled. */
    public static class AddNodeAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /** Random source. */
        private final Random random = new Random();

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public AddNodeAction(EditorContext ctx) {
            super("Add Location", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            int x = 200 + random.nextInt(400);
            int y = 150 + random.nextInt(300);
            context.addNode(x, y);
        }
    }

    /** "Remove Node" action: only enabled when a node is selected. */
    public static class RemoveNodeAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public RemoveNodeAction(EditorContext ctx) {
            super("Remove Selected Location", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return context.getGraph().getSelectedNode() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.removeNode(context.getGraph().getSelectedNode());
        }
    }

    /** "Draw Route" toggles add-edge mode; enabled when a node is selected. */
    public static class StartAddEdgeAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public StartAddEdgeAction(EditorContext ctx) {
            super("Draw Route", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return context.getGraph().getSelectedNode() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.setAddEdgeMode(!context.isAddEdgeMode());
        }
    }

    /** "Remove Edge" action; enabled when an edge is selected. */
    public static class RemoveEdgeAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public RemoveEdgeAction(EditorContext ctx) {
            super("Remove Selected Route", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return context.getGraph().getSelectedEdge() != null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.removeEdge(context.getGraph().getSelectedEdge());
        }
    }

    /** Simulate one step. Always enabled. */
    public static class SimulateStepAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public SimulateStepAction(EditorContext ctx) {
            super("Simulate Time Step", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return true;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.getSimulator().simulateStep();
        }
    }

    /** Undo. Enabled when the command stack has anything to revert. */
    public static class UndoAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public UndoAction(EditorContext ctx) {
            super("Undo", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return context.canUndo();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.undo();
        }
    }

    /** Redo. Enabled when the redo stack has anything to reapply. */
    public static class RedoAction extends ModelBoundAction {
        /** Serialisation id. */
        private static final long serialVersionUID = 1L;

        /**
         * Constructs the action.
         * @param ctx the editor context
         */
        public RedoAction(EditorContext ctx) {
            super("Redo", ctx);
        }

        @Override
        protected boolean computeEnabled() {
            return context.canRedo();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            context.redo();
        }
    }
}
