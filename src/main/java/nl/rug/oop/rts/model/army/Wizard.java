package nl.rug.oop.rts.model.army;

/**
 * A spellcasting hero unit such as Saruman of Many Colours.
 * <p>
 * The wizard is fragile but devastating; every other round it unleashes a
 * spell that more than triples its base damage. In return its hit points
 * are typically lower than those of a comparable line unit, so a focused
 * counter-charge can still bring it down.
 */
public class Wizard extends Unit {

    /** Damage multiplier applied when the wizard casts a spell. */
    private static final double SPELL_MULTIPLIER = 3.0;

    /** Counts the rounds since the last cast; the spell goes off when even. */
    private int roundsSinceCast;

    /**
     * Constructs a wizard ready to cast.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction
     * @param evasion dodge chance in [0, 1]
     */
    public Wizard(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
        this.roundsSinceCast = 0;
    }

    @Override
    public int computeDamage() {
        roundsSinceCast++;
        if (roundsSinceCast % 2 == 0) {
            return (int) Math.round(super.computeDamage() * SPELL_MULTIPLIER);
        }
        return super.computeDamage();
    }

    @Override
    public void resetForBattle() {
        roundsSinceCast = 0;
    }

    @Override
    public String getAbilityDescription() {
        return "Sorcery: triples damage every second round of combat.";
    }
}
