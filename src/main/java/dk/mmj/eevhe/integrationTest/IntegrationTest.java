package dk.mmj.eevhe.integrationTest;

import dk.eSoftware.commandLineParser.*;
import dk.mmj.eevhe.Application;
import dk.mmj.eevhe.client.Client;
import dk.mmj.eevhe.client.ClientConfigBuilder;
import dk.mmj.eevhe.client.ResultFetcher;
import dk.mmj.eevhe.client.Voter;
import dk.mmj.eevhe.initialization.SystemConfigurer;
import dk.mmj.eevhe.initialization.SystemConfigurerConfigBuilder;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoard;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoardConfigBuilder;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthorityConfigBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * IntegrationTest runs a {@link BulletinBoard}, {@link SystemConfigurer} and a number of
 * {@link dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority}s, as honest participants of the system.
 * <p>
 * Using the configuration it is possible to determine which ones should be executed and whether votes should
 * be case at the beginning, and whether they should be cast 30 secs. before planned termination
 */
public class IntegrationTest implements Application {
    private static final Logger logger = LogManager.getLogger(IntegrationTest.class);
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
    private final List<Integer> decryptionAuthorities;
    private final List<Integer> voteDelays;
    private final int duration;
    private Observer observer;
    private Thread bbThread;

    public IntegrationTest(IntegrationTest.IntegrationTestConfiguration configuration) {
        this.decryptionAuthorities = configuration.decryptionAuthorities;
        this.duration = configuration.duration;
        this.voteDelays = configuration.voteDelays;
    }

    public void setObserver(Observer observer) {
        this.observer = observer;
    }

    @Override
    public void run() {
        logger.warn("############### WARNING ###############");
        logger.warn("This is an integration test.");
        logger.warn("By using this configuration you are running the system with a single point of failure, namely this machine");
        logger.warn("Under no circumstance should this happen in a production environment");
        logger.warn("############### WARNING ###############");

        logger.info("Launching BulletinBoard");
        startBulletinBoard();

        logger.info("Configuring system");
        runSystemConfiguration(duration);

        for (Integer id : decryptionAuthorities) {
            logger.info("Launching authority decryption with id=" + id);
            launchDecryptionAuthority(id);
        }

        for (Integer voteDelay : voteDelays) {
            doMultiVote(voteDelay);
        }

        retrieveVotes(duration + 1);

        scheduler.schedule(this::twoMinHook, 2, TimeUnit.MINUTES);

        if (observer != null) {
            scheduler.schedule(observer::finalizationHook, duration + 1, TimeUnit.MINUTES);
        }

        try {
            bbThread.join();
        } catch (InterruptedException e) {
            logger.error("Interrupted when waiting for bb to terminate");
        }
    }

    private void twoMinHook() {
        logger.debug("Two Minute Hook");
    }

    /**
     * Retrieves votes from bulletin board at later time
     *
     * @param timeOffset when to retrieve the votes in minutes
     */
    private void retrieveVotes(int timeOffset) {
        CommandLineParser<? extends InstanceCreatingConfiguration<? extends Client>> parser =
                new SingletonCommandLineParser<>(new ClientConfigBuilder());
        InstanceCreatingConfiguration<? extends Client> parse;
        try {
            parse = parser.parse("--client --read=true".split(" "));
        } catch (NoSuchBuilderException | WrongFormatException e) {
            throw new RuntimeException("Failed parsing resultFetcher conf", e);
        }

        Client voter = parse.produceInstance();

        if(observer != null && voter instanceof ResultFetcher){
            observer.registerResultFetcher((ResultFetcher) voter);
        }

        scheduler.schedule(voter, timeOffset, TimeUnit.MINUTES);
    }

    /**
     * Asynchronously casts 50 random votes at specified relative time
     *
     * @param timeOffset relative delay for votes in ms
     */
    private void doMultiVote(int timeOffset) {
        CommandLineParser<? extends InstanceCreatingConfiguration<? extends Client>> parser
                = new SingletonCommandLineParser<>(new ClientConfigBuilder());
        InstanceCreatingConfiguration<? extends Client> parse;
        try {
            parse = parser.parse("--client --multi=50".split(" "));
        } catch (NoSuchBuilderException | WrongFormatException e) {
            throw new RuntimeException("Failed parsing multi-vote conf", e);
        }

        Client voter = parse.produceInstance();
        if (observer != null && voter instanceof Voter) {
            observer.registerMultiVoter((Voter) voter);
        }

        scheduler.schedule(voter, timeOffset, TimeUnit.MILLISECONDS);
    }

    private void startBulletinBoard() {
        CommandLineParser<BulletinBoard.BulletinBoardConfiguration> parser = new SingletonCommandLineParser<>(new BulletinBoardConfigBuilder());
        BulletinBoard.BulletinBoardConfiguration conf;
        try {
            conf = parser.parse(new String[0]);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            throw new RuntimeException("Failed parsing bulletin board conf", e);
        }

        BulletinBoard bb = conf.produceInstance();
        if (observer != null) {
            observer.registerBulletinBoard(bb);
        }

        bbThread = new Thread(bb);
        bbThread.start();
    }

    /**
     * Executes the trusted dealer in thread, as to wait for finished execution
     *
     * @param duration duration of vote
     */
    private void runSystemConfiguration(int duration) {
        String params = "--addresses -1_https://localhost:8081 -2_https://localhost:8082 -3_https://localhost:8083 --outputFolder=conf --time -min=" + duration;
        CommandLineParser<SystemConfigurer.SystemConfiguration> parser = new SingletonCommandLineParser<>(new SystemConfigurerConfigBuilder());

        SystemConfigurer.SystemConfiguration conf;
        try {
            conf = parser.parse(params.split(" "));
        } catch (NoSuchBuilderException | WrongFormatException e) {
            throw new RuntimeException("Failed parsing trusted dealer conf.", e);
        }

        conf.produceInstance().run();
    }

    private void launchDecryptionAuthority(Integer id) {
        String params = "--authority --conf=conf/ --port=808" + id + " --id=" + id;
        SingletonCommandLineParser<DecryptionAuthority.DecryptionAuthorityConfiguration> parser =
                new SingletonCommandLineParser<>(new DecryptionAuthorityConfigBuilder());

        DecryptionAuthority.DecryptionAuthorityConfiguration conf;
        try {
            conf = parser.parse(params.split(" "));
        } catch (NoSuchBuilderException | WrongFormatException e) {
            throw new RuntimeException("Failed parsing decryption authority conf.", e);
        }
        DecryptionAuthority da = conf.produceInstance();

        if (observer != null) {
            observer.registerDecryptionAuthority(da);
        }
        scheduler.execute(da);
    }

    /**
     * Observer-pattern for being able to extract and assert on state in tests
     */
    interface Observer {

        /**
         * Registers a Decryption Authority
         *
         * @param authority authority
         */
        void registerDecryptionAuthority(DecryptionAuthority authority);

        /**
         * Registers a voter-instance with multi-vote set to true
         *
         * @param multiVoter voter instance
         */
        void registerMultiVoter(Voter multiVoter);

        /**
         * Registers the bulletinBoard
         *
         * @param bulletinBoard the instance
         */
        void registerBulletinBoard(BulletinBoard bulletinBoard);

        /**
         * Register resultFetcher
         *
         * @param resultFetcher resultsFetcher
         */
        void registerResultFetcher(ResultFetcher resultFetcher);

        /**
         * Called when voting has been finished - signalizes the end of the test-run
         */
        void finalizationHook();
    }

    public static class IntegrationTestConfiguration extends AbstractInstanceCreatingConfiguration<IntegrationTest> {
        private final List<Integer> decryptionAuthorities;
        private final List<Integer> voteDelays;
        private final int duration;

        /**
         * @param decryptionAuthorities list of ids for all enabled decryption authorities
         * @param duration              how long the vote should be active(minutes)
         * @param voteDelays            list of times where votes should be dispatched
         */
        IntegrationTestConfiguration(List<Integer> decryptionAuthorities, int duration, List<Integer> voteDelays) {
            super(IntegrationTest.class);
            this.decryptionAuthorities = decryptionAuthorities;
            this.duration = duration;
            this.voteDelays = voteDelays;
        }

        List<Integer> getDecryptionAuthorities() {
            return decryptionAuthorities;
        }

        List<Integer> getVoteDelays() {
            return voteDelays;
        }

        int getDuration() {
            return duration;
        }
    }
}
