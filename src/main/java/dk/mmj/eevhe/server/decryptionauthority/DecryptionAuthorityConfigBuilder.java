package dk.mmj.eevhe.server.decryptionauthority;

import dk.eSoftware.commandLineParser.CommandLineParser;
import dk.mmj.eevhe.TestableConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DecryptionAuthorityConfigBuilder implements CommandLineParser.ConfigBuilder<DecryptionAuthority.DecryptionAuthorityConfiguration>, TestableConfigurationBuilder {
    private static final Logger logger = LogManager.getLogger(DecryptionAuthorityConfigBuilder.class);
    private static final String SELF = "authority";

    //Configuration options
    private static final String ID = "id=";
    private static final String PORT = "port=";
    private static final String BULLETIN_BOARD_1 = "bb=";
    private static final String BULLETIN_BOARD_2 = "bulletinBoard=";
    private static final String CONF = "conf=";
    private static final String CORRUPT = "timeCorrupt=";

    //State
    private Integer port = 8080;
    private Integer id = null;
    private String bulletinBoard = "https://localhost:8080";
    private String confPath = "";
    private Integer timeCorrupt = 0;

    @Override
    public void applyCommand(CommandLineParser.Command command) {
        String cmd = command.getCommand();

        if (cmd.startsWith(PORT)) {
            String intString = cmd.substring(PORT.length());
            port = Integer.parseInt(intString);
        } else if (cmd.startsWith(BULLETIN_BOARD_1)) {
            bulletinBoard = cmd.substring(BULLETIN_BOARD_1.length());
        } else if (cmd.startsWith(BULLETIN_BOARD_2)) {
            bulletinBoard = cmd.substring(BULLETIN_BOARD_2.length());
        } else if (cmd.startsWith(CONF)) {
            confPath = cmd.substring(CONF.length());
        } else if (cmd.startsWith(CORRUPT)) {
            timeCorrupt = Integer.parseInt(cmd.substring(CORRUPT.length()));
        } else if (cmd.startsWith(ID)) {
            id = Integer.parseInt(cmd.substring(ID.length()));
        } else if (!cmd.equals(SELF)) {
            logger.warn("Did not recognize command " + command.getCommand());
        }
    }

    @Override
    public DecryptionAuthority.DecryptionAuthorityConfiguration build() {
        if (id == null) {
            logger.error("id must be supplied. Use -h for help");
            return null;
        }

        return new DecryptionAuthority.DecryptionAuthorityConfiguration(port, bulletinBoard, confPath, id, timeCorrupt);
    }

    @Override
    public String help() {
        return "" +
                "\tMODE: " + SELF + "\n" +
                "\t  --" + ID + "int\t\tSpecifies the authority's ID in the system. Used in key-generation protocol\n" +
                "\t  --" + PORT + "int\t\tSpecifies port to be used. Standard=8081\n" +
                "\t  --" + BULLETIN_BOARD_2 + "/" + BULLETIN_BOARD_1 + "ip:port location bulletin board to be used\n" +
                "\t  --" + CORRUPT + "int\t\tInteger specifying with what offset a timeCorrupt DA tries to decrypt with." +
                "\t  --" + CONF + "Path\t\tRelative path to config folder containgin; zip file with certificate and private key named DA{id}.zip," +
                " a file denoted 'common_input.json' containing common input to all DAs and list of candidates 'candidates.json'.\n";

    }

    @Override
    public List<String> getParameters() {
        return new ArrayList<>(Arrays.asList(
                ID,
                PORT,
                BULLETIN_BOARD_1,
                BULLETIN_BOARD_2,
                CONF,
                CORRUPT
        ));
    }
}
