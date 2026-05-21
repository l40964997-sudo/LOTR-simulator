package nl.rug.oop.rts.controller.action;

import nl.rug.oop.rts.controller.EditorContext;
import nl.rug.oop.rts.model.observer.ModelListener;

import javax.swing.*;

/**
 * Base class for actions whose enabled state depends on the model.
 * <p>
 * Subscribing actions to the model means we never need to scatter
 * "enable/disable button" calls throughout the controller: the menu bar
 * simply pushes its actions, the actions push themselves whenever the
 * model state changes. This is the canonical view of the
 * <em>Observer</em> pattern within the controller layer.
 */
public abstract class ModelBoundAction extends AbstractAction {

    /** Serialisation id. */
    private static final long serialVersionUID = 1L;

    /** The shared context. */
    protected final EditorContext context;

    /**
     * Constructs the action and subscribes it to the relevant model
     * components.
     *
     * @param name the display name
     * @param context the context
     */
    protected ModelBoundAction(String name, EditorContext context) {
        super(name);
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        this.context = context;
        ModelListener listener = e -> setEnabled(computeEnabled());
        context.getGraph().addListener(listener);
        context.getCommandHistory().addListener(listener);
        context.addListener(listener);
        setEnabled(computeEnabled());
    }

    /**
     * Subclasses override this to declare when the action should be enabled.
     *
     * @return whether the action should be enabled
     */
    protected abstract boolean computeEnabled();
}
