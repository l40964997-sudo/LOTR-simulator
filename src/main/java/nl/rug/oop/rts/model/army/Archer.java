package nl.rug.oop.rts.model.army;

/**
 * Specialised {@link Unit} that deals bonus damage on the opening round of
 * combat to model a volley of arrows before melee is joined.
 * <p>
 * The class is purely an illustration of polymorphism for the bonus points
 * mentioned in the assignment. It is intentionally small - the only
 * behavioural override is in {@link #computeDamage()} - so that the
 * battle resolver can stay completely oblivious to whether it is operating
 * on a plain {@code Unit} or one of its subclasses.
 */
public class Archer extends Unit {

    /** Multiplier applied while the volley bonus is still available. */
    private static final double VOLLEY_MULTIPLIER = 1.5;

    /** Whether the unit has discharged its volley this battle. */
    private boolean volleyAvailable;

    /**
     * Constructs an archer with its volley charge ready.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     */
    public Archer(String name, int strength, int health) {
        super(name, strength, health);
        this.volleyAvailable = true;
    }

    @Override
    public int computeDamage() {
        if (volleyAvailable) {
            volleyAvailable = false;
            return (int) Math.round(super.computeDamage() * VOLLEY_MULTIPLIER);
        }
        return super.computeDamage();
    }

    /**
     * Restores the volley charge. Called by the battle resolver at the
     * start of a fresh engagement.
     */
    public void resetVolley() {
        this.volleyAvailable = true;
    }
}
