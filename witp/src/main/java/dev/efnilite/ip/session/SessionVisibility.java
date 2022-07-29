package dev.efnilite.ip.session;

public enum SessionVisibility {


    /**
     * Private means no-one can join. Spectators and Players won't be able to join.
     */
    PRIVATE,
    /**
     * ID only means players will need the Session ID to join.
     */
    ID_ONLY,
    /**
     * Public means any player can join as player.
     */
    PUBLIC,

}
