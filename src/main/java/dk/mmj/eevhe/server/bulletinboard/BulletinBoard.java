package dk.mmj.eevhe.server.bulletinboard;


import dk.eSoftware.commandLineParser.Configuration;
import dk.mmj.eevhe.entities.PersistedBallot;
import dk.mmj.eevhe.server.AbstractServer;
import dk.mmj.eevhe.server.ServerState;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.ArrayList;
import java.util.HashSet;


public class BulletinBoard extends AbstractServer {
    static final String PUBLIC_KEY = "publicKey";
    static final String HAS_VOTED = "hasVoted";
    static final String RESULT = "result";
    static final String BALLOTS = "ballots";

    private final BulletinBoardConfiguration configuration;

    public BulletinBoard(BulletinBoardConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void configure(ServletHolder servletHolder) {
        servletHolder.setInitParameter(
                "jersey.config.server.provider.classnames",
                BulletinBoardResource.class.getCanonicalName() + ";" + "org.glassfish.jersey.jackson.JacksonFeature");

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

    public static class BulletinBoardConfiguration implements Configuration {
        private final Integer port;

        BulletinBoardConfiguration(Integer port) {
            this.port = port;
        }
    }
}
