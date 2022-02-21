package dev.efnilite.witp.session;

public enum SessionVisibility {

    /**
     * Public means any player can join as player.
     */
    PUBLIC,

    /**
     * Id only means players will need the Session ID to join.
     */
    ID_ONLY,

    /**
     * Private means no-one can join. Spectators and Players won't be able to join.
     */
    PRIVATE

}
