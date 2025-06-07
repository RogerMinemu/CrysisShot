package com.crysisshot.ranking;

public enum Rank {
    NOVATO("Novato", 0, "§7"),
    LETAL("Letal", 10, "§e"),
    VETERANO("Veterano", 50, "§6"),
    DEADEYE("Deadeye", 100, "§c");

    private final String displayName;
    private final int requiredKills;
    private final String colorCode;

    Rank(String displayName, int requiredKills, String colorCode) {
        this.displayName = displayName;
        this.requiredKills = requiredKills;
        this.colorCode = colorCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getRequiredKills() {
        return requiredKills;
    }

    public String getColorCode() {
        return colorCode;
    }

    public String getFormattedName() {
        return colorCode + displayName + "§r";
    }

    /**
     * Get the appropriate rank based on bow kills
     */
    public static Rank getRankByKills(int bowKills) {
        if (bowKills >= DEADEYE.requiredKills) {
            return DEADEYE;
        } else if (bowKills >= VETERANO.requiredKills) {
            return VETERANO;
        } else if (bowKills >= LETAL.requiredKills) {
            return LETAL;
        } else {
            return NOVATO;
        }
    }

    /**
     * Get the next rank in progression
     */
    public Rank getNextRank() {
        switch (this) {
            case NOVATO:
                return LETAL;
            case LETAL:
                return VETERANO;
            case VETERANO:
                return DEADEYE;
            case DEADEYE:
                return null; // Already at max rank
            default:
                return null;
        }
    }
}
