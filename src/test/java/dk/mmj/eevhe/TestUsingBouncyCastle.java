package dk.mmj.eevhe;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

/**
 * Does static provider add of BouncyCastle
 */
public class TestUsingBouncyCastle {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
}
