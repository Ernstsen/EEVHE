package dk.mmj.eevhe.initialization;

import dk.eSoftware.commandLineParser.CommandLineParser;
import dk.mmj.eevhe.TestableConfigurationBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class SystemConfigurerConfigBuilder implements CommandLineParser.ConfigBuilder<SystemConfigurer.SystemConfiguration>, TestableConfigurationBuilder {
    private static final Logger logger = LogManager.getLogger(SystemConfigurerConfigBuilder.class);

    //Configuration options
    private static final String SELF = "--configuration";
    private static final String OUTPUT_FOLDER_PATH = "outputFolder=";
    private static final String CERT_KEY_PATH = "certKey=";
    private static final String CERT_PATH = "cert=";
    private static final String DA_ADDRESSES = "addresses"; // TODO: da_addresses??
    private static final String BB_PEER_ADDRESSES = "bb_peer_addresses";
    private static final String TIME = "time";
    private static final String TIME_DAY = "day=";
    private static final String TIME_HR = "hour=";
    private static final String TIME_MIN = "min=";
    private final Map<Integer, String> daAddresses = new HashMap<>();
    private final Map<Integer, String> bbPeerAddresses = new HashMap<>();
    //state
    private Path outputFolderPath = Paths.get("./conf/");
    private Path skFilePath = Paths.get("./certs/test_glob_key.pem");
    private Path certFilePath = Paths.get("./certs/test_glob.pem");
    private long time = 10_000 * 60;

    @Override
    public void applyCommand(CommandLineParser.Command command) {
        String cmd = command.getCommand();
        if (cmd.equalsIgnoreCase(TIME)) {
            time = 0;
            for (String param : command.getParams()) {
                int minute = 60 * 1_000;
                int hour = minute * 60;
                int day = hour * 24;

                if (param.startsWith(TIME_DAY)) {
                    time += (long) Integer.parseInt(param.substring(TIME_DAY.length())) * day;
                } else if (param.startsWith(TIME_HR)) {
                    time += (long) Integer.parseInt(param.substring(TIME_HR.length())) * hour;
                } else if (param.startsWith(TIME_MIN)) {
                    time += (long) Integer.parseInt(param.substring(TIME_MIN.length())) * minute;
                }
            }

        } else if (cmd.equalsIgnoreCase(DA_ADDRESSES)) {
            for (String param : command.getParams()) {
                String[] split = param.split("_", 2);
                daAddresses.put(Integer.parseInt(split[0]), split[1]);
            }
        } else if (cmd.equalsIgnoreCase(BB_PEER_ADDRESSES)) {
            for (String param : command.getParams()) {
                String[] split = param.split("_", 2);
                bbPeerAddresses.put(Integer.parseInt(split[0]), split[1]);
            }
        } else if (cmd.startsWith(OUTPUT_FOLDER_PATH)) {
            outputFolderPath = Paths.get(cmd.substring(OUTPUT_FOLDER_PATH.length()));
        } else if (cmd.startsWith(CERT_PATH)) {
            certFilePath = Paths.get(cmd.substring(CERT_PATH.length()));
        } else if (cmd.startsWith(CERT_KEY_PATH)) {
            skFilePath = Paths.get(cmd.substring(CERT_KEY_PATH.length()));
        } else if (!cmd.equals(SELF)) {
            logger.warn("Did not recognize command " + command.getCommand());
        }
    }

    @Override
    public SystemConfigurer.SystemConfiguration build() {
        return new SystemConfigurer.SystemConfiguration(
                outputFolderPath,
                skFilePath,
                certFilePath,
                daAddresses,
                bbPeerAddresses,
                new Date().getTime() + time);
    }

    @Override
    public String help() {
        return "\tMODE: System Configurer (configuration) - System Configurer creates files to configure Decryption Authorities \n" +
                "\t NOTE: System Configurer assumes file 'certs/test_glob_key.pem' to sign generated certificates\n" +
                "\t  --" + TIME + "\t\t Sets time. Vote ends at current time + time parameters. Standard value: 10 min\n" +
                "\t\t -" + TIME_DAY + "days, -" + TIME_HR + "hours, -" + TIME_MIN + "minutes\n" +
                "\t --" + DA_ADDRESSES + "\t\t defines addresses for the DAs. Supplied on the form -id_url\n" +
                "\t --" + OUTPUT_FOLDER_PATH + "\t\t defines output folder using relative path\n" +
                "\t --" + CERT_PATH + "\t\t specifies .pem file with certificate" +
                "\t --" + CERT_KEY_PATH + "\t\t specifies .pem file with certificate private-key\n";
    }

    @Override
    public List<String> getParameters() {
        return Arrays.asList(
                OUTPUT_FOLDER_PATH,
                CERT_KEY_PATH,
                DA_ADDRESSES,
                TIME,
                TIME_DAY,
                TIME_HR,
                TIME_MIN
        );
    }
}
