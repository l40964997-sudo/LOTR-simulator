package nl.rug.oop.rts.model.army;

/**
 * A heavily armoured {@link Unit} that absorbs hits but lashes back at
 * full strength.
 * <p>
 * The base {@code Unit} already supports a flat {@code defense} field; this
 * subclass simply documents the role for code clarity and lets specialised
 * sub-subclasses (such as trolls) layer extra behaviour on top without
 * needing to revisit the base class.
 */
public class Heavy extends Unit {

    /**
     * Constructs a heavy unit with its full statline.
     *
     * @param name unit name
     * @param strength base damage per round
     * @param health hit points
     * @param defense flat damage reduction per hit (typically 3-6)
     * @param evasion dodge chance in [0, 1]
     */
    public Heavy(String name, int strength, int health, int defense, double evasion) {
        super(name, strength, health, defense, evasion);
    }

    @Override
    public String getAbilityDescription() {
        return "Heavy Armour: reduces every incoming hit by a flat amount.";
    }
}
