package dk.mmj.eevhe;

import dk.eSoftware.commandLineParser.*;
import dk.mmj.eevhe.client.ClientConfigBuilder;
import dk.mmj.eevhe.client.ResultFetcher;
import dk.mmj.eevhe.client.Voter;
import dk.mmj.eevhe.initialization.TrustedDealer;
import dk.mmj.eevhe.initialization.TrustedDealerConfigBuilder;
import dk.mmj.eevhe.integrationTest.IntegrationTest;
import dk.mmj.eevhe.integrationTest.IntegrationTestConfigBuilder;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoard;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardConfigBuilder;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityConfigBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.HashMap;

public class Main {

    static {
        //Disable JMX as a way to bypass errors known to OpenJDK8
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());
        //Register BouncyCastle as Securityprovider
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        CommandLineParser parser = getParser();
        Configuration configuration;

        if (args.length == 0 || "-h".equals(args[0]) || "-help".equals(args[0])) {
            System.out.println(parser.help());
            return;
        }

        try {
            configuration = parser.parse(args);
        } catch (NoSuchBuilderException e) {
            System.out.println("Failed to match first parameter \"" + args[0] + "\" to a mode of configuration");
            return;
        } catch (WrongFormatException e) {
            System.out.println("One or more parameters were incorrectly formatted: \"" + e.getMessage() + "\"");
            return;
        }


        Application app = getApplication(configuration);
        app.run();
    }

    private static Application getApplication(Configuration parse) {
        if (parse instanceof Voter.VoterConfiguration) {
            return new Voter((Voter.VoterConfiguration) parse);
        } else if (parse instanceof ResultFetcher.ResultFetcherConfiguration) {
            return new ResultFetcher((ResultFetcher.ResultFetcherConfiguration) parse);
        } else if (parse instanceof DecryptionAuthority.DecryptionAuthorityConfiguration) {
            return new DecryptionAuthority((DecryptionAuthority.DecryptionAuthorityConfiguration) parse);
        } else if (parse instanceof BulletinBoard.BulletinBoardConfiguration) {
            return new BulletinBoard((BulletinBoard.BulletinBoardConfiguration) parse);
        } else if (parse instanceof TrustedDealer.TrustedDealerConfiguration) {
            return new TrustedDealer((TrustedDealer.TrustedDealerConfiguration) parse);
        } else if (parse instanceof IntegrationTest.IntegrationTestConfiguration) {
            return new IntegrationTest((IntegrationTest.IntegrationTestConfiguration) parse);
        } else {
            System.out.println("" +
                    "====================\n" +
                    "Mapped first parameter to configuration but configuration was not registered. \n" +
                    "This should NEVER happen\n" +
                    "Terminating.\n" +
                    "====================");
            System.exit(-1);
            return null;
        }
    }

    private static CommandLineParser getParser() {
        HashMap<String, CommandLineParser.ConfigBuilder> mapping = new HashMap<>();
        mapping.put("--client", new ClientConfigBuilder());
        mapping.put("--authority", new DecryptionAuthorityConfigBuilder());
        mapping.put("--bulletinBoard", new BulletinBoardConfigBuilder());
        mapping.put("--dealer", new TrustedDealerConfigBuilder());
        mapping.put("--integrationTest", new IntegrationTestConfigBuilder());
        return new MultiParser(mapping);
    }
}
