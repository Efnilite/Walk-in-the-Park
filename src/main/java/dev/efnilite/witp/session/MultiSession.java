package dev.efnilite.witp.session;

import com.google.common.annotations.Beta;

/**
 * A Session for multiple players.
 *
 * @author Efnilite
 */
@Beta
public class MultiSession extends SingleSession {

    @Override
    public boolean isAcceptingPlayers() {
        return true;
    }
}
