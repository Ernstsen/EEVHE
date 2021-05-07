package dk.mmj.eevhe.server.bulletinboard;


import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPeerInfo;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;


public class BulletinBoardEdge extends AbstractServer {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger;
    private final int port;
    private final Integer id;
    private final BulletinBoardEdgeConfiguration configuration;
    private final ServerState state = ServerState.getInstance();

    // Server state keys
    static final String PEER_ADDRESSES = "peerAddresses";

    public BulletinBoardEdge(BulletinBoardEdgeConfiguration configuration) {
        this.configuration = configuration;
        logger = LogManager.getLogger(BulletinBoardPeer.class + " " + configuration.id + ":");
        port = configuration.port;
        id = configuration.id;

        Path conf = Paths.get(configuration.confPath);

        try {
            BBInput bbInput = mapper.readValue(conf.resolve("BB_input.json").toFile(), BBInput.class);
            List<String> peerAddresses = bbInput.getPeers().stream().map(BBPeerInfo::getAddress).collect(Collectors.toList());
            state.put(PEER_ADDRESSES, peerAddresses);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read BB input file", e);
        }
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                BulletinBoardEdgeResource.class.getCanonicalName() + ";" + "org.glassfish.jersey.jackson.JacksonFeature");
    }

    @Override
    protected int getPort() {
        return configuration.port;
    }

    public static class BulletinBoardEdgeConfiguration extends AbstractInstanceCreatingConfiguration<BulletinBoardEdge> {
        private final int port;
        private final String confPath;
        private final int id;

        BulletinBoardEdgeConfiguration(int port, String confPath, int id) {
            super(BulletinBoardEdge.class);
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
