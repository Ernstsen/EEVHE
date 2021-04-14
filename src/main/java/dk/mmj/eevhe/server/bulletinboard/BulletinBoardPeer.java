package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPackage;
import dk.mmj.eevhe.entities.BBState;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.server.AbstractServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class BulletinBoardPeer extends AbstractServer {
    private static MethodHandles.Lookup lookup = MethodHandles.lookup();
    private final Logger logger;
    private final int port;
    private final Integer id;
    private final BBInput bbInput;

    public BulletinBoardPeer(BulletinBoardPeerConfiguration configuration) {
        logger = LogManager.getLogger(BulletinBoardPeer.class + " " + configuration.id + ":");
        port = configuration.port;
        id = configuration.id;

        Path conf = Paths.get(configuration.confPath);
        if (!Files.exists(conf) || !Files.exists(conf)) {
            logger.error("Configuration folder either did not exists or were not a folder. Path: " + conf + "\n");
            terminate();
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            bbInput = mapper.readValue(conf.resolve("BB_input.json").toFile(), BBInput.class);
        } catch (IOException e) {
            logger.error("Failed to read BB input file", e);
            throw new RuntimeException("Failed to read BB from file", e);
        }
    }

    /**
     * Called when receiving network data from edge
     * Passes data to MVBA protocol
     * Receives responds back from MVBA protocol
     * Updates BB state
     */
    public static void executeConsensusProtocol(BBPackage bbPackage, MethodExecutor methodExecutor) {
        //        TODO: Receive data from Edge
        //        TODO: call MVBA protocol
        //        TODO: receive result from MVBA protocol -> If ok:
        boolean consensusObtained = true;

        if (consensusObtained) {
            //        TODO: update BB state
            methodExecutor.execute();
        }
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                BulletinBoardPeer.class.getCanonicalName());
    }

    @Override
    protected int getPort() {
        return port;
    }

    /**
     * Configuration for a BulletinBoardPeer
     */
    public static class BulletinBoardPeerConfiguration extends AbstractInstanceCreatingConfiguration<BulletinBoardPeer> {
        private final int port;
        private final String confPath;
        private final int id;

        BulletinBoardPeerConfiguration(int port, String confPath, int id) {
            super(BulletinBoardPeer.class);
            this.port = port;
            this.confPath = confPath;
            this.id = id;
        }

        public int getPort() {
            return port;
        }

        public String getConfPath() {
            return confPath;
        }

        public int getId() {
            return id;
        }

    }
}
