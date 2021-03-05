package dk.mmj.eevhe.server.decryptionauthority;

import dk.mmj.eevhe.crypto.ElGamal;
import dk.mmj.eevhe.crypto.SecurityUtils;
import dk.mmj.eevhe.crypto.exceptions.UnableToDecryptException;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import dk.mmj.eevhe.crypto.zeroknowledge.VoteProofUtils;
import dk.mmj.eevhe.entities.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.*;

import static dk.mmj.eevhe.TestHelper.runDKG;
import static org.junit.Assert.*;

public class TestDecrypterImpl {

    private HashMap<Integer, PartialKeyPair> dkgRes;


    @Before
    public void setup() {
        dkgRes = runDKG();
    }

    @Test
    public void generatePartialResult() throws InterruptedException {
        PublicKey pk = dkgRes.get(1).getPublicKey();
        List<PersistedBallot> ballots = Arrays.asList(
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "1", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(1, 3, "2", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(0, 3, "3", pk)),
                new PersistedBallot(SecurityUtils.generateBallot(2, 3, "4", pk))
        );
        Thread.sleep(5);//

        List<Candidate> candidates = Arrays.asList(
                new Candidate(0, "Mette", "A"),
                new Candidate(1, "Jakob", "V"),
                new Candidate(2, "Morten", "B")
        );

        DecrypterImpl decrypterImpl1 = new DecrypterImpl(1, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);
        DecrypterImpl decrypterImpl2 = new DecrypterImpl(2, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);
        DecrypterImpl decrypterImpl3 = new DecrypterImpl(3, () -> ballots, b -> VoteProofUtils.verifyBallot(b, pk), candidates);

        PartialResultList partialResultList1 = decrypterImpl1.generatePartialResult(new Date().getTime() - 1, dkgRes.get(1));
        PartialResultList partialResultList2 = decrypterImpl2.generatePartialResult(new Date().getTime() - 1, dkgRes.get(2));
        PartialResultList partialResultList3 = decrypterImpl3.generatePartialResult(new Date().getTime() - 1, dkgRes.get(3));

        partialResultList1.getResults().forEach(this::assertResultPasses);
        partialResultList2.getResults().forEach(this::assertResultPasses);
        partialResultList3.getResults().forEach(this::assertResultPasses);

        //Decryption successful
        HashMap<Integer, BigInteger> partials = new HashMap<>();
        partials.put(1, partialResultList1.getResults().get(0).getResult());
        partials.put(2, partialResultList2.getResults().get(0).getResult());
        partials.put(3, partialResultList3.getResults().get(0).getResult());
        BigInteger cs = SecurityUtils.lagrangeInterpolate(partials, pk.getP());
        try {
            CipherText sum = partialResultList1.getResults().get(0).getCipherText();
            int result = ElGamal.homomorphicDecryptionFromPartials(sum.getD(), cs, pk.getG(), pk.getP(), 15);
            assertEquals("Results was incorrect", 1, result);
        } catch (UnableToDecryptException e) {
            fail(e.getMessage());
        }

    }

    private void assertResultPasses(PartialResult res) {
        PartialKeyPair partialKeyPair = dkgRes.get(res.getId());
        PublicKey pk = partialKeyPair.getPublicKey();

        CipherText sum = res.getCipherText();
        PublicKey partialPublicKey = new PublicKey(partialKeyPair.getPartialPublicKey(), pk.getG(), pk.getQ());

        assertTrue("Failed to validate proof of correct decryption for partial decryption!", DLogProofUtils.verifyProof(
                sum,
                new CipherText(res.getResult(), sum.getD()),
                partialPublicKey, res.getProof(), res.getId()));
    }

    @Test
    public void returnsNullWhenNoBallots() {
        DecrypterImpl decrypterImpl = new DecrypterImpl(1, ArrayList::new, (b) -> false, new ArrayList<>());

        PartialResultList res = decrypterImpl.generatePartialResult(-1, new PartialKeyPair(null, null, null));

        assertNull("Should be null", res);
    }
}
