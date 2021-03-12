package dk.mmj.eevhe.integrationTest;

import dk.eSoftware.commandLineParser.NoSuchBuilderException;
import dk.eSoftware.commandLineParser.SingletonCommandLineParser;
import dk.eSoftware.commandLineParser.WrongFormatException;
import dk.mmj.eevhe.client.ResultFetcher;
import dk.mmj.eevhe.client.Voter;
import dk.mmj.eevhe.client.results.ElectionResult;
import dk.mmj.eevhe.server.bulletinboard.BulletinBoard;
import dk.mmj.eevhe.server.decryptionauthority.DecryptionAuthority;
import joptsimple.internal.Strings;
import org.junit.After;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class IntegrationUnitTest {

    @After
    public void tearDown() throws Exception {
        Path conf = Paths.get("conf");

        List<Path> paths = Arrays.asList(
                conf.resolve("DA1.zip"),
                conf.resolve("DA2.zip"),
                conf.resolve("DA3.zip")
        );

        for (Path path : paths) {
            if (Files.exists(path)) {
                Files.delete(path);
            }
        }
    }

    @Test
    public void runTestElection() {
        IntegrationTestConfigBuilder builder = new IntegrationTestConfigBuilder();
        IntegrationTest test;
        try {
            String args = "--vote -start --duration=3";

            IntegrationTest.IntegrationTestConfiguration config =
                    new SingletonCommandLineParser<>(builder).parse(args.split(" "));

            assertEquals("All DAs should be active", Arrays.asList(1, 2, 3), config.getDecryptionAuthorities());
            assertEquals("Should have one scheduled votes", 1, config.getVoteDelays().size());
            assertEquals("Did not respect duration param", 3, config.getDuration());

            test = config.produceInstance();
            assertNotNull("Failed to produce instance", test);
        } catch (NoSuchBuilderException | WrongFormatException e) {
            fail("failed to build config: " + e);
            e.printStackTrace();
            return;
        }

        final TestObserver observer = new TestObserver();

        test.setObserver(observer);

        test.run();

        assertTrue("Did not finalize election",observer.finalized);

        assertNotNull("ErrorList was not initialized - finalization was not executed", observer.errors);
        if (!observer.errors.isEmpty()) {
            fail(Strings.join(observer.errors, "\n"));
        }
    }


    private static class TestObserver implements IntegrationTest.Observer {
        private final List<DecryptionAuthority> authorities = new ArrayList<>();
        private final List<Voter> multiVoters = new ArrayList<>();
        private BulletinBoard bulletinBoard;
        private ResultFetcher resultFetcher;
        private List<String> errors;
        private boolean finalized = false;

        public TestObserver() {
        }

        @Override
        public void registerDecryptionAuthority(DecryptionAuthority authority) {
            authorities.add(authority);
        }

        @Override
        public void registerMultiVoter(Voter multiVoter) {
            multiVoters.add(multiVoter);
        }

        @Override
        public void registerBulletinBoard(BulletinBoard bulletinBoard) {
            this.bulletinBoard = bulletinBoard;
        }

        @Override
        public void registerResultFetcher(ResultFetcher resultFetcher) {
            this.resultFetcher = resultFetcher;
        }


        @Override
        public void finalizationHook() {
            try {
                errors = new ArrayList<>();
                int[] votes = new int[3];
                Arrays.fill(votes, 0);

                for (Voter multiVoter : multiVoters) {
                    int[] cast = multiVoter.getCastVotes();
                    votes[0] += cast[0];
                    votes[1] += cast[1];
                    votes[2] += cast[2];
                }


                ElectionResult electionResult = resultFetcher.getElectionResult();
                int retries = 0;

                while (electionResult == null && retries < 5) {
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException e) {
                        fail("InterruptedException on thread.sleep in extracting election results");
                    }
                    retries++;
                    electionResult = resultFetcher.getElectionResult();
                }

                if (electionResult == null) {
                    errors.add("Failed to extract electionResult");
                    finalized = true;
                    return;
                }
                int[] extracted = electionResult.getCandidateVotes().stream().mapToInt(Integer::valueOf).toArray();
                if (!Arrays.equals(votes, extracted)) {
                    errors.add("Extracted votes did not match those cast. Expected: " + Arrays.toString(votes) +
                            " Actual: " + Arrays.toString(extracted));
                }
                finalized = true;
            } finally {
                terminate();
            }
        }

        private void terminate() {
            for (DecryptionAuthority authority : authorities) {
                authority.terminate();
            }
            if (bulletinBoard != null) {
                bulletinBoard.terminate();
            }

        }
    }

}
