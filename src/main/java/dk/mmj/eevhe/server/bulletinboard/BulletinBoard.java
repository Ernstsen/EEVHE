package dk.mmj.eevhe.server.bulletinboard;


import dk.eSoftware.commandLineParser.AbstractInstanceCreatingConfiguration;
import dk.mmj.eevhe.entities.PersistedBallot;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.ArrayList;
import java.util.HashSet;


public class BulletinBoard extends AbstractServer {
    static final String HAS_VOTED = "hasVoted";
    static final String PUBLIC_INFO = "publicInfo";
    static final String CERTIFICATE = "certificate";
    static final String RESULT = "result";
    static final String BALLOTS = "ballots";
    static final String COEFFICIENT_COMMITMENT = "coefficient commitment";
    static final String PEDERSEN_COMPLAINTS = "pedersenComplaints";
    static final String FELDMAN_COMPLAINTS = "feldmanComplaints";
    static final String RESOLVED_COMPLAINTS = "resolved complaints";

    private final BulletinBoardConfiguration configuration;

    public BulletinBoard(BulletinBoardConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                BulletinBoardEdgeResource.class.getCanonicalName() + ";" + "org.glassfish.jersey.jackson.JacksonFeature");

        initializeVoting();
    }

    private void initializeVoting() {
        ServerState.getInstance().put(BALLOTS, new ArrayList<PersistedBallot>());
        ServerState.getInstance().put(HAS_VOTED, new HashSet<String>());
    }

    @Override
    protected int getPort() {
        return configuration.port;
    }

    public static class BulletinBoardConfiguration extends AbstractInstanceCreatingConfiguration<BulletinBoard> {
        private final int port;

        BulletinBoardConfiguration(int port) {
            super(BulletinBoard.class);
            this.port = port;
        }

        public int getPort() {
            return port;
        }
    }
}
