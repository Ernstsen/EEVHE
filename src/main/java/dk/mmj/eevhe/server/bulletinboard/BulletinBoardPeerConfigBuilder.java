package dk.mmj.eevhe.server.bulletinboard;

import dk.eSoftware.commandLineParser.CommandLineParser;
import dk.mmj.eevhe.TestableConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class BulletinBoardPeerConfigBuilder implements CommandLineParser.ConfigBuilder<BulletinBoardPeer.BulletinBoardPeerConfiguration>, TestableConfigurationBuilder {
    private static final Logger logger = LogManager.getLogger(BulletinBoardPeerConfigBuilder.class);
    private static final String SELF = "bulletinBoardPeer";

    //Configuration options
    private static final String ID = "id=";
    private static final String PORT = "port=";
    private static final String CONF = "conf=";

    //State
    private Integer port = 8080;
    private Integer id = null;
    private String confPath = "";


    @Override
    public void applyCommand(CommandLineParser.Command command) {
        String cmd = command.getCommand();

        if (cmd.startsWith(PORT)) {
            String intString = cmd.substring(PORT.length());
            port = Integer.parseInt(intString);
        } else if (cmd.startsWith(CONF)) {
            confPath = cmd.substring(CONF.length());
        } else if (cmd.startsWith(ID)) {
            id = Integer.parseInt(cmd.substring(ID.length()));
        } else if (!cmd.equals(SELF)) {
            logger.warn("Did not recognize command " + command.getCommand());
        }
    }

    @Override
    public BulletinBoardPeer.BulletinBoardPeerConfiguration build() {
        if (id == null) {
            logger.error("id must be supplied. Use -h for help");
            return null;
        }

        return new BulletinBoardPeer.BulletinBoardPeerConfiguration(port, confPath, id);
    }

    @Override
    public String help() {
        return "" +
                "\tMODE: " + SELF + "\n" +
                "\t  --" + ID + "int\t\tSpecifies the BBPeer's ID in the system\n" +
                "\t  --" + PORT + "int\t\tSpecifies port to be used. Standard=8081\n" +
                "\t  --" + CONF + "Path\t\tRelative path to config folder containing; zip file with certificate named BBPeer{id}.zip," +
                " a file denoted 'BB_input.json' containing common input to all BBPeers'.\n";
    }

    @Override
    public List<String> getParameters() {
        return new ArrayList<>(Arrays.asList(
                ID,
                PORT,
                CONF
        ));
    }
}
