package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPackage;
import dk.mmj.eevhe.entities.BulletinBoardUpdatable;
import dk.mmj.eevhe.entities.SignedEntity;
import dk.mmj.eevhe.protocols.agreement.AgreementHelper;
import dk.mmj.eevhe.server.AbstractServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BulletinBoardPeer extends AbstractServer {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger;
    private final int port;
    private final Integer id;
    private final BBInput bbInput;
    private AgreementHelper agreementHelper;

    public BulletinBoardPeer(BulletinBoardPeerConfiguration configuration) {
        logger = LogManager.getLogger(BulletinBoardPeer.class + " " + configuration.id + ":");
        port = configuration.port;
        id = configuration.id;

        Path conf = Paths.get(configuration.confPath);
        if (!Files.exists(conf) || !Files.exists(conf)) {
            logger.error("Configuration folder either did not exists or were not a folder. Path: " + conf + "\n");
            terminate();
        }

        try {
            bbInput = mapper.readValue(conf.resolve("BB_input.json").toFile(), BBInput.class);
        } catch (IOException e) {
            logger.error("Failed to read BB input file", e);
            throw new RuntimeException("Failed to read BB from file", e);
        }

//        agreementHelper = new AgreementHelper(
//                null,
//                null,
//                this::updateState
//        );//TODO: Introduce!
    }

    /**
     * Called when receiving network data from edge
     * Passes data to MVBA protocol
     * Receives responds back from MVBA protocol
     * Updates BB state
     */
    @Deprecated
    public static void executeConsensusProtocol(BBPackage<?> bbPackage, Runnable methodExecutor) {
        //        TODO: Receive data from Edge
        //        TODO: call MVBA protocol
        //        TODO: receive result from MVBA protocol -> If ok:
        boolean consensusObtained = true;

        if (consensusObtained) {
            //        TODO: update BB state
            methodExecutor.run();
        }
    }

    private void updateState(String str) {
        try {
            BulletinBoardUpdatable updatable = mapper.readValue(str, BulletinBoardUpdatable.class);

            updatable.update(null);//TODO: GET STATE
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize BulletinBoardUpdatable", e);
        }
    }

    public void executeConsensusProtocol(SignedEntity<BulletinBoardUpdatable> entity) {
        //TODO: VERIFY!
        try {
            String s = mapper.writeValueAsString(entity);
            agreementHelper.agree(s);
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize entity", e);
        }
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                BulletinBoardPeerResource.class.getCanonicalName());
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
