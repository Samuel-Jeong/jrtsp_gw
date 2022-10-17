package org.kkukie.jrtsp_gw.media.stun.model;

import lombok.Getter;
import lombok.Setter;
import org.kkukie.jrtsp_gw.media.stun.handler.IceAuthenticator;

import java.security.SecureRandom;

@Getter
@Setter
public class IceAuthenticatorImpl implements IceAuthenticator {

    private final SecureRandom random = new SecureRandom();
    private final StringBuilder builder = new StringBuilder();
    protected String ufrag = "";
    protected String password = "";
    protected String remoteUfrag = "";
    protected String remotePassword = "";

    public IceAuthenticatorImpl() {
    }

    public byte[] getLocalKey(String ufrag) {
        return (this.isUserRegistered(ufrag) && (this.password != null)) ? this.password.getBytes() : null;
    }

    public byte[] getRemoteKey(String ufrag, String media) {
        int colon = ufrag.indexOf(":");
        if (colon < 0) {
            if (ufrag.equals(this.remoteUfrag)) {
                return this.remotePassword.getBytes();
            }
        } else if (ufrag.equals(this.ufrag) && this.remotePassword != null) {
            return this.remotePassword.getBytes();
        }
        return null;
    }

    public boolean isUserRegistered(String ufrag) {
        int colon = ufrag.indexOf(":");
        String result = (colon < 0) ? ufrag : ufrag.substring(0, colon);
        return result.equals(this.ufrag);
    }

    public boolean validateUsername(String username) {
        int colon = username.indexOf(":");
        return (colon != -1) && username.substring(0, colon).equals(this.ufrag);
    }

    public void reset() {
        this.ufrag = "";
        this.password = "";
        this.remoteUfrag = "";
        this.remotePassword = "";
    }
}
