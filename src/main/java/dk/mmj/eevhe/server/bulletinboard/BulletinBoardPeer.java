package dk.mmj.eevhe.server.bulletinboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.BBInput;
import dk.mmj.eevhe.entities.BBPeerInfo;
import dk.mmj.eevhe.entities.BulletinBoardUpdatable;
import dk.mmj.eevhe.entities.PeerInfo;
import dk.mmj.eevhe.protocols.agreement.AgreementHelper;
import dk.mmj.eevhe.protocols.agreement.broadcast.BrachaBroadcastManager;
import dk.mmj.eevhe.protocols.agreement.mvba.CompositeCommunicator;
import dk.mmj.eevhe.protocols.agreement.mvba.MultiValuedByzantineAgreementProtocolImpl;
import dk.mmj.eevhe.protocols.connectors.RestBBPeerCommunicator;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.eclipse.jetty.servlet.ServletHolder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static dk.mmj.eevhe.client.SSLHelper.configureWebTarget;

public class BulletinBoardPeer extends AbstractServer {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final Logger logger;
    private final int port;
    private final Integer id;
    private final BBInput bbInput;
    private final AsymmetricKeyParameter sk;
    private final String certString;
    private final Map<Integer, RestBBPeerCommunicator> communicators;
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
            Map<String, String> peerCertificates = bbInput.getPeers().stream()
                    .collect(Collectors.toMap(i -> Integer.toString(i.getId()), BBPeerInfo::getCertificate));
            ServerState.getInstance().put("peerCertificates." + id, peerCertificates);
        } catch (IOException e) {
            logger.error("Failed to read BB input file", e);
            throw new RuntimeException("Failed to read BB from file", e);
        }

        Path privateInput = conf.resolve("BB_peer" + id + ".zip");
        logger.info("Reading private input from file: " + privateInput);
        try (ZipFile zipFile = new ZipFile(privateInput.toFile())) {
            ZipEntry skEntry = zipFile.getEntry("sk.pem");
            ZipEntry certEntry = zipFile.getEntry("cert.pem");

            try (InputStream is = zipFile.getInputStream(skEntry)) {
                sk = KeyHelper.readKey(IOUtils.toByteArray(is));
            }

            try (InputStream is = zipFile.getInputStream(certEntry)) {
                certString = new String(IOUtils.toByteArray(is));
            }

        } catch (IOException e) {
            logger.error("Error occurred while reading private input file from " + privateInput, e);
            throw new RuntimeException("Failed to read private input from file");
        }

        communicators = bbInput.getPeers().stream()
                .collect(Collectors.toMap(
                        PeerInfo::getId,
                        p -> new RestBBPeerCommunicator(configureWebTarget(logger, p.getAddress()), sk)));

        CompositeCommunicator compositeCommunicator = new CompositeCommunicator(this::sendString, this::sendBoolean);

        Map<Integer, Consumer<String>> peerMap = communicators.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue()::sendMessageBroadcast));

        int t = peerMap.size() / 3;
        agreementHelper = new AgreementHelper(
                new BrachaBroadcastManager(peerMap, id, t),
                new MultiValuedByzantineAgreementProtocolImpl(compositeCommunicator, peerMap.size(), t, "BB_PEER" + id),//TODO
                this::updateState
        );

        Consumer<BulletinBoardUpdatable> consensus = this::executeConsensusProtocol;
        ServerState.getInstance().put("executeConsensusProtocol." + id, consensus);
    }

    private void sendString(String baId, String message) {
        communicators.values().forEach(c -> c.sendMessageBA(baId, message));
    }

    private void sendBoolean(String baId, Boolean message) {
        communicators.values().forEach(c -> c.sendMessageBA(baId, message));
    }

    private void updateState(String str) {
        try {
            BulletinBoardUpdatable updatable = mapper.readValue(str, BulletinBoardUpdatable.class);

            updatable.update(getState());
        } catch (JsonProcessingException e) {
            logger.error("Failed to deserialize BulletinBoardUpdatable", e);
        }
    }

    private BulletinBoardState getState() {
        ServerState serverState = ServerState.getInstance();
        return serverState.computeIfAbsent("bbState." + id, s -> new BulletinBoardState());
    }

    public void executeConsensusProtocol(BulletinBoardUpdatable entity) {
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
        servletHolder.setInitParameter("id", id.toString());
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
