package dk.mmj.eevhe.client;

import dk.eSoftware.commandLineParser.CommandLineParser;
import dk.mmj.eevhe.TestableConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClientConfigBuilder implements CommandLineParser.ConfigBuilder<Client.ClientConfiguration<? extends Client>>, TestableConfigurationBuilder {
    private static final Logger logger = LogManager.getLogger(ClientConfigBuilder.class);
    private static final String SELF = "client";

    //Configuration options
    private static final String TARGET_URL = "server=";
    private static final String ID = "id=";
    private static final String VOTE = "vote=";
    private static final String MULTI = "multi=";
    private static final String READ = "read=";
    private static final String FORCE_CALCULATIONS = "forceCalculations=";
    private static final String ELECTION_CERT_PATH = "electionCertificate=";

    //State
    private String targetUrl = "https://localhost:8080";
    private String id = "TEST_ID" + UUID.randomUUID().toString();
    private Integer vote = null;
    private Integer multi = null;
    private boolean read = false;
    private boolean forceCalculations = false;
    private Path electionCertPath = Paths.get("certs/test_glob.pem");

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
            vote = Integer.parseInt(cmd.substring(VOTE.length()));
        } else if (cmd.startsWith(MULTI)) {
            multi = Integer.parseInt(cmd.substring(MULTI.length()));
        } else if (cmd.startsWith(READ)) {
            read = Boolean.parseBoolean(cmd.substring(READ.length()));
        } else if (cmd.startsWith(FORCE_CALCULATIONS)) {
            forceCalculations = Boolean.parseBoolean(cmd.substring(FORCE_CALCULATIONS.length()));
        } else if (cmd.startsWith(ELECTION_CERT_PATH)) {
            electionCertPath = Paths.get(cmd.substring(ELECTION_CERT_PATH.length()));
        } else if (!cmd.equals(SELF)) {
            logger.warn("Did not recognize command " + command.getCommand());
        }
    }

    /**
     * Returns the {@link Voter.VoterConfiguration} with the loaded variables.
     *
     * @return Configuration for client execution
     */
    @Override
    public Client.ClientConfiguration<? extends Client> build() {
        if (read) {
            return new ResultFetcher.ResultFetcherConfiguration(targetUrl, forceCalculations, electionCertPath);
        } else {
            return new Voter.VoterConfiguration(targetUrl, id, vote, multi, electionCertPath);
        }
    }

    /**
     * @return String describing configuration options
     */
    @Override
    public String help() {
        return "\tMODE: client\n" +
                "\t  --" + TARGET_URL + "url\t\t Specifies url for public server to connect to. Standard is: "
                + targetUrl + "\n" +
                "\t  --" + ID + "idString\t\t id identifying this instance as a unique voter\n" +
                "\t  --" + VOTE + "{true,false}\t the vote to be cast. If not supplied program will prompt for it\n" +
                "\t  --" + MULTI + "int\t\t How many random votes should be cast. If set, id and vote is ignored as it is test.\n" +
                "\t  --" + READ + "boolean\t Default=false. If true, all params except " + TARGET_URL.substring(0, TARGET_URL.length() - 1) +
                "are ignored. Fetches poll results from bulletin board.\n" +
                "\t  --" + FORCE_CALCULATIONS + "boolean\t\t Forces client to calculate sum of votes.\n" +
                "\t  --" + ELECTION_CERT_PATH + "Path\t\t Points to global election certificate to verify DA and BB certs.\n";
    }

    @Override
    public List<String> getParameters() {
        return Arrays.asList(
                TARGET_URL,
                ID,
                VOTE,
                MULTI,
                READ,
                FORCE_CALCULATIONS,
                ELECTION_CERT_PATH
        );
    }
}
