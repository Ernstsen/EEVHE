package dk.mmj.eevhe.server.bulletinboard;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.client.JerseyWebTarget;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;


public class BulletinBoardEdge extends AbstractServer {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger;
    private final int port;
    private final Integer id;
    private final BulletinBoardEdgeConfiguration configuration;
    private final ServerState state = ServerState.getInstance();

    // Server state keys
    static final String CERTIFICATE_LIST = "certificateList";
    static final String PEER_ADDRESSES = "peerAddresses";

    public BulletinBoardEdge(BulletinBoardEdgeConfiguration configuration) {
        this.configuration = configuration;
        logger = LogManager.getLogger(BulletinBoardPeer.class + " " + configuration.id + ":");
        port = configuration.port;
        id = configuration.id;

        Path conf = Paths.get(configuration.confPath);

        List<SignedEntity<List<String>>> listOfSignedPeerCertificates = new ArrayList<>();

        try {
            BBInput bbInput = mapper.readValue(conf.resolve("BB_input.json").toFile(), BBInput.class);
            List<String> peerAddresses = bbInput.getPeers().stream().map(BBPeerInfo::getAddress).collect(Collectors.toList());
            state.put(PEER_ADDRESSES, peerAddresses);

            for (String peerAddress: peerAddresses) {
                JerseyWebTarget target = configureWebTarget(logger, peerAddress);
                String signedPeerCertificatesString = target.path("getPeerCertificates").request().get(String.class);
                SignedEntity<List<String>> signedPeerCertificates = mapper.readValue(signedPeerCertificatesString, new TypeReference<SignedEntity<List<String>>>() {});
                listOfSignedPeerCertificates.add(signedPeerCertificates);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to query signed certificate list from all bulletin board peers", e);
        }

        state.put(CERTIFICATE_LIST, listOfSignedPeerCertificates);
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
