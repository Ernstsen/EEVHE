package dk.mmj.eevhe;

import dk.eSoftware.commandLineParser.*;
import dk.mmj.eevhe.client.ClientConfigBuilder;
import dk.mmj.eevhe.initialization.SystemConfigurerConfigBuilder;
import dk.mmj.eevhe.integrationTest.IntegrationTestConfigBuilder;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardConfigBuilder;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityConfigBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import java.util.HashMap;

public class Main {

    static {
        //Disable JMX as a way to bypass errors known to OpenJDK8
        System.setProperty("log4j2.disable.jmx", Boolean.TRUE.toString());
        //Register BouncyCastle as SecurityProvider
        Security.addProvider(new BouncyCastleProvider());
    }

    public static void main(String[] args) {
        CommandLineParser<? extends InstanceCreatingConfiguration<? extends Application>> parser = getParser();
        InstanceCreatingConfiguration<? extends Application> configuration;

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

        Application app = configuration.produceInstance();
        app.run();
    }

    private static CommandLineParser<? extends InstanceCreatingConfiguration<? extends Application>> getParser() {
        HashMap<String, CommandLineParser.ConfigBuilder<? extends InstanceCreatingConfiguration<? extends Application>>> mapping = new HashMap<>();
        mapping.put("--client", new ClientConfigBuilder());
        mapping.put("--authority", new DecryptionAuthorityConfigBuilder());
        mapping.put("--bulletinBoard", new BulletinBoardConfigBuilder());
        mapping.put("--configuration", new SystemConfigurerConfigBuilder());
        mapping.put("--integrationTest", new IntegrationTestConfigBuilder());
        return new MultiParser<>(mapping);
    }
}
