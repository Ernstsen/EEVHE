package dk.mmj.evhe.client;

import dk.eSoftware.commandLineParser.CommandLineParser;
import dk.eSoftware.commandLineParser.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class ClientConfigBuilder implements CommandLineParser.ConfigBuilder {
    private static final Logger logger = LogManager.getLogger(ClientConfigBuilder.class);
    private static final String SELF = "--client";

    //Configuration options
    private static final String TARGET_URL = "server=";
    private static final String ID = "id=";
    private static final String VOTE = "vote=";
    private static final String MULTI = "multi=";

    //State
    private String targetUrl = "https://localhost:8080";
    private String id = "TEST_ID" + UUID.randomUUID().toString();
    private Boolean vote = null;
    private Integer multi = null;

    /**
     * Sets the local variables by translating the input from the command line.
     * The available commands are "server=", "id=", "vote=" or "multi=".
     *
     * @param command the command given as parameter.
     */
    @Override
    public void applyCommand(CommandLineParser.Command command) {
        String cmd = command.getCommand();
        if (cmd.startsWith(TARGET_URL)) {
            targetUrl = cmd.substring(TARGET_URL.length());
        } else if (cmd.startsWith(ID)) {
            id = cmd.substring(ID.length());
        } else if (cmd.startsWith(VOTE)) {
            vote = Boolean.parseBoolean(cmd.substring(VOTE.length()));
        } else if (cmd.startsWith(MULTI)) {
            multi = Integer.parseInt(cmd.substring(MULTI.length()));
        } else if (!cmd.equals(SELF)) {
            logger.warn("Did not recognize command " + command.getCommand());
        }
    }

    /**
     * Returns the {@link Client.ClientConfiguration} with the loaded variables.
     *
     * @return Configuration for client execution
     */
    @Override
    public Configuration build() {
        return new Client.ClientConfiguration(targetUrl, id, vote, multi);
    }

    /**
     * @return String describing configuration options
     */
    @Override
    public String help() {
        return "\tMODE: keyServer\n" +
                "\t  --" + TARGET_URL + "publicServerUrl\t Specifies url for public server to connect to. Standard is: "
                + targetUrl + "\n" +
                "\t  --" + ID + "idString\t id identifying this instance as a unique voter\n" +
                "\t  --" + VOTE + "{true,false}\t the vote to be cast. If not supplied program will prompt for it\n" +
                "\t  --" + MULTI + "int\t How many random votes should be cast. If set, id and vote is ignored as it is test.";
    }
}