package dk.mmj.eevhe.entities;

import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.crypto.zeroknowledge.DLogProofUtils;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestBBPackage {
    private AsymmetricKeyParameter secretKey;

    public TestBBPackage() throws IOException {
        secretKey = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
    }

    private CipherText getCipherText(int c, int d) {
        return new CipherText(BigInteger.valueOf(c), BigInteger.valueOf(d));
    }

    private Proof getProof(int e0, int e1, int z0, int z1) {
        return new Proof(BigInteger.valueOf(e0), BigInteger.valueOf(e1), BigInteger.valueOf(z0), BigInteger.valueOf(z1));
    }

    private DLogProofUtils.Proof getDLogProof(int e, int z) {
        return new DLogProofUtils.Proof(BigInteger.valueOf(e), BigInteger.valueOf(z));
    }

    @Test
    public void shouldRepresentBallotAsString() {
        List<CandidateVoteDTO> candidateVotes = new ArrayList<CandidateVoteDTO>() {{
            add(new CandidateVoteDTO(getCipherText(1, 2), "Some random ID", getProof(1, 2, 3, 4)));
            add(new CandidateVoteDTO(getCipherText(10, 20), "Some random ID", getProof(10, 20, 30, 40)));
        }};

        BBPackage<BallotDTO> bbPackage = new BBPackage<>(new BallotDTO(candidateVotes, "Some random ID", getProof(0, 1, 2, 3)));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"candidateVotes\":[{\"cipherText\":{\"c\":1,\"d\":2},\"id\":\"Some random ID\",\"proof\":{\"e0\":1,\"e1\":2,\"z0\":3,\"z1\":4}},{\"cipherText\":{\"c\":10,\"d\":20},\"id\":\"Some random ID\",\"proof\":{\"e0\":10,\"e1\":20,\"z0\":30,\"z1\":40}}],\"id\":\"Some random ID\",\"sumIsOneProof\":{\"e0\":0,\"e1\":1,\"z0\":2,\"z1\":3}}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentPartialResultsAsString() {
        List<PartialResult> partialResults = new ArrayList<PartialResult>() {{
            add(new PartialResult(1, BigInteger.valueOf(1), getDLogProof(1, 2), getCipherText(1, 2)));
            add(new PartialResult(2, BigInteger.valueOf(2), getDLogProof(1, 2), getCipherText(1, 2)));
        }};
        PartialResultList partialResultList = new PartialResultList(partialResults, 2, 1);

        BBPackage<SignedEntity<PartialResultList>> bbPackage = new BBPackage<>(new SignedEntity<PartialResultList>(partialResultList, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"results\":[{\"id\":1,\"result\":1,\"proof\":{\"e\":1,\"z\":2},\"cipherText\":{\"c\":1,\"d\":2}},{\"id\":2,\"result\":2,\"proof\":{\"e\":1,\"z\":2},\"cipherText\":{\"c\":1,\"d\":2}}],\"voteCount\":2,\"daId\":1},\"signature\":\"IELngkXB1JgA/yT0X6hNaJFG8DuIZAqAmNwxbGVYYJ3UF3qMrqgnBftovApzdzCM4k3x3xTBw1aRhKCMi0vas3zUtnZeLoVqKLlz+1V19FJvRIs9cz30p0wAqC0dnBjpNEDkM9mrE+3XqCbrnHuim9ecSObHtXjMvvNthvD21kidaMmHYpR6l5obwL2xj8bpV5/404hygaPeSdFgJreEbef+414JDc/Uc1Vs2auhNk+NBo2n3Wi2vq5ApR8299URC0rXWTruImooxepX8lISSlG2MTsO+Se5MvFUUeKCYvQQn5766lUNQ4UjsKK9luIXPULTWYhjI8xhwVKVVyYZByC46aa5mwW+/uossqRxyf08mZMoYDAFMz1czLMLth5bCipAZxXhrJF4fW5cvTKLGbChtfBcKiFd+UobcA8QQuj1joC51FuGrdfvA5GeptznjGMnpzE7LCRNiGrViIB6PWCJdmEMjBDU4BI/gmC2Aeo33Fl136aVZUtVk64/h74TCdXk6tXeVLWaRRQN/D0R8kaJw0S+2dcLbnfNXR4Ra3yNh7VukOBUtShJSo2IxnW/W8gErH1/QCPr+HMAMVVzAdRoQ0gb3tECY+PnA6wkBqLH2eNaJIZ19K0CdWsySTdm4bqLQNwpR0XPIhQqAShNYaLFr5aUqOzCUBsDv6mvs0Y=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentCommitmentAsString() {
        CommitmentDTO commitment = new CommitmentDTO(new BigInteger[]{BigInteger.valueOf(1), BigInteger.valueOf(2), BigInteger.valueOf(3)}, 1, "Some protocol");

        BBPackage<SignedEntity<CommitmentDTO>> bbPackage = new BBPackage<>(new SignedEntity<>(commitment, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"commitment\":[1,2,3],\"id\":1,\"protocol\":\"Some protocol\"},\"signature\":\"CNmSTl3uQxs+oyOAyJItfyeF+Fb6/uG7xLQd8bW/A8ckbBLTDtjNU69eaKWFPj23uUyDftqyvtW7GNiEgIZeBVgwg6io4izxygeD3vxBvNJ2cV8ocHgjdxjzVFxI3+LlNKd9puiYWkcXdeH5+LRRpPT+6Yjdrw0jsuFHKBDXnpETsr2/27EO917LzA47Rj2QLWiPl1DEj9xVZyywNwqfBYNfQ5vkXO/Yn6icWU01SXj5MbktZNYTxuDeLCZnGB/dguFeJ36aW66nQ6hxjHaj61u5dfXgI+q7Y72c9BZd1kTSF2mt2vJvgp059gRz4L5nwRWGOcdIKaraNvBzsn4AdEwF/FlJrskFK8h3COh1yYHQrp0H3EVo2MyPM0xe8rTI5Yf4Vmvo9P0qcynMeyTXemiUn09Zikkg8XNgbMW3RPYklK6qxmVfWG5e9o5ylgz8dubKL10BH66S85zQJLQyvB+Moy2jCAZBfbtOFSbiALJiB2lwdKAIhvlRCESYYvAl8CnpaZwA6dRL0/KII9DKHM/rOqq69S/oxgq1RFkxviscqUA0+/85NLQ1l081ws4ZVtOqqulbK2iIhkqGN/tyo3+TtuMlBA9ak3F0WWrBZ2/AlVb3h2uuIWF4onWVZeHgu8kv9Y7lrlPT20Du36rmGEEd6MZUpCAD0WXcEja6yJE=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentPedersenComplaintAsString() {
        PedersenComplaintDTO complaint = new PedersenComplaintDTO(1,2);

        BBPackage<SignedEntity<PedersenComplaintDTO>> bbPackage = new BBPackage<>(new SignedEntity<>(complaint, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"senderId\":1,\"targetId\":2},\"signature\":\"UujdM0CEn5R1YD2Xu0s4fn3ai8b29WfGHh/evQLQxCZT5bmPD7hmf+JuX2Dwd5fgdnQzCgkNt6J0UsSw+ULTF9SQnAESnNz/VEnhxbprjwY+j9DnuEjiLmj32qSE8AKXUaLzO18URUHUhKJ6/1xWa25XZywbgttvRVBoQTy5oFjWM3OThts3GaRL+/0IB2dDZ/tUmuB/u0Ql7S/b8IYEEdXXR0AKpweOSPibtIQpTt6AlheuHFxmGNFtmzZpeoVZDeWR4w4iOqUBgbHOHPx5J9oC7re/IRJyYFwVsEjHNUmmka3pGa7xJ1LnxhbsIkB7uYAMATqItYQ6BvzjLv7wPFuReuEBsdgo9LEIOT7hZRxnwJGjcTLVXOZ6yOLoV3nVErviyMHYlgfZafwz5CevuwtlrdJBrP4qnpUtRrfpVT0rtnHrrxzHvZfCEZ9QWLgXHybypsvWEA1iXABD6MK60I/6kDpU/sPalJ8+hW5ZLIq11ikKFanlpOVjzSus8iKMrlrTzGD6gqHiI2uZqmIEJfXpb7uj2zHhED/WhfTHupWz1vIBaQyQ09PFw5uME7S2lTPbqm5fv3W9dM5c/7c9JblEgs6tf0dtbgjrobTCXyEpxZpxNsWOaSHCbuxgBQO7Nyl72uwILK7MO2yR65ZncWPSljcHGOSSMmB9COITrhE=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentFeldmanComplaintAsString() {
        FeldmanComplaintDTO complaint = new FeldmanComplaintDTO(1, 2, BigInteger.valueOf(1), BigInteger.valueOf(2));

        BBPackage<SignedEntity<FeldmanComplaintDTO>> bbPackage = new BBPackage<>(new SignedEntity<>(complaint, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"senderId\":1,\"targetId\":2,\"val1\":1,\"val2\":2},\"signature\":\"WqMf8a7UghsQ726Kfn6T8Zk4SbJsDRxA77JlF0qiMeBvLZhaQGkVfeh5gzSTEWrAelcpMEek8sup0TVNCMaeWcbyfNEHZM21uIXgNKF9ecapFKyD7OAob7HR7GZs3bJoYlwI+nzn+4ulBRuJBLJ7I1WN6tMs+8aQGmByFIDTtINK5+cPRxs0ypNAZIijlbw60PFiAwwGMMC/K5PihVDQl2QgIDzJtGYlSCdOjWdtMUAEEUgY3b78sMnZlCfPuaL5EzMeaariVsuKLGi0lSaDJfRqHaSh5vc6NPKEM9d78/KZidkojM4MxObjiqbnWMv72JW34ypzqZqrgNaHbbZKtgE8Sn0gTQ9zF/7v+CbfylF4FVrcseTnWOvwNTPfX2WtzKVqkOD5mYY6Urv9gjG4u3iL1SzBUC1te2m3++MCcpWr8+7A1PBa+5r+sua/f28RloFw6lgcQIKGaZ4Djd69zT+KXo4vr4Wob0efkzND5e7Gszn5M8aRqBXysZWUqxK2V9EZnQwTV27x5bO+hF6jTU5I57DWmJjWLEN9poKjaEUIAKpQNSnqIqEg47mW0o5TxGwbuPs68S3PR31pbJFBeWFdYtimIWI8DGdF7EWoVDPs88MQUQAPaaDWHVVs6z4TR8/riLpIB131KVW8glnjHh/kuIYHIrqOfjpbbF3Ev78=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentComplaintResolveAsString() {
        PartialSecretMessageDTO partialSecretMessage = new PartialSecretMessageDTO(BigInteger.valueOf(1), BigInteger.valueOf(2), 1, 2);
        ComplaintResolveDTO resolve = new ComplaintResolveDTO(1, 2, partialSecretMessage);

        BBPackage<SignedEntity<ComplaintResolveDTO>> bbPackage = new BBPackage<>(new SignedEntity<>(resolve, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"complaintSenderId\":1,\"complaintResolverId\":2,\"value\":{\"partialSecret1\":1,\"partialSecret2\":2,\"target\":1,\"sender\":2}},\"signature\":\"o3ApZ+Sn160bwk9lsvFfg4aEZucjQEemDtdE+q2FRj42ekv/VQOQyyqnNXPoA0huon+y945v6aJvMjX8SzBVjw6DrZkhTx2gPueOCRmqTfjstBiRa8Qh6FICJ0Kdg6aWKxcLfBusJFMZiTGwEptuROkHlXEMjS/4cI0DfUw4oerKfrUAUqlPaMbiLclU8cSZvHRTM0VJIJ2cpplufIKYGRgLRju+6vjU3RcfGqmQQtyooD8dX2PtVNE/Z+yEossvnGh0Y2ZeLp35umNLagx+3yGue6FtzlPICzm6swikXcjPv0CBhKl3NmOKWF4DXDWzyTS8bgIXQzgYkIdopNfw2pwQEzESXD3PhqeyX3T8251NbGJimLbal3xyUFk7kRVfJsCWhk0Xp+0Bbqr/pfb++XVlMnvhEykrIgSqMzC5FRqN87QywyLYhHmYtgVPER3DdsnEGvpAT6g/sndOxD/wvgHAJjIZqVQguiR3hUKojD29lSrW+MlSURkRFQt/QmeXA+NtVqd7/5PH+VE40I5GmW6Q7rPmyLLA8IQ4n280jpuRGumbb2nd4aBMBRT0Nqp9Y2/iLukittPUeP4JQDgXnoYglBbLvNU2HRcs+QukLzqsXIeEKrrkUkaoMZ9wStirFQtflixvq0EpHLuQRs0xz7rrPRFUy5in4ixmDDHP4NA=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentPartialPublicInfoAsString() {
        PublicKey publicKey = new PublicKey(BigInteger.valueOf(4), BigInteger.valueOf(2), BigInteger.valueOf(11));
        List<Candidate> candidates = new ArrayList<Candidate>(){{
            add(new Candidate(1, "John", "Vote for John"));
            add(new Candidate(2, "Dorthe", "Dorthe as queen"));
        }};

        PartialPublicInfo partialPublicInfo = new PartialPublicInfo(1, publicKey, BigInteger.valueOf(3), candidates, 123456789, "Some certificate");

        BBPackage<SignedEntity<PartialPublicInfo>> bbPackage = new BBPackage<>(new SignedEntity<>(partialPublicInfo, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"senderId\":1,\"publicKey\":{\"h\":4,\"g\":2,\"q\":11},\"partialPublicKey\":3,\"candidates\":[{\"idx\":1,\"name\":\"John\",\"description\":\"Vote for John\"},{\"idx\":2,\"name\":\"Dorthe\",\"description\":\"Dorthe as queen\"}],\"endTime\":123456789,\"certificate\":\"Some certificate\"},\"signature\":\"k4pICuCFC6h/RSA25eoU7iTAWdaUHV4TfVNwUlu3GT14gFu8hPGvkRT8xzaY9XVaseb6PCax4WUcpeZPox2tmGuILa/BsIPySrpKgud9glPMalvKLpH8mXBtYx/sUILZDfWHUOXx5CSuz2+HHtEzgbbj1PLe/5F0o/OjhAyHjVgaYdyQg+rej9jFZF7E8bnAOztBAJhRsxv4WIekoI8kFl4ng1LFX0ohe9V+QnhJzYFn6p0LqkqmEANY6wqeNCs5SM1KzoTLgwTZz6sTBJMVFW5ImC3XPJQBblLdtriCRwk1o3uN24Btt6FTpfum5M9kiPBFHpoWTXRCoSrK1GO3olw6oL70kzOjeZBXtHn367V8M6vq2DEu0EnRqmMbuUpjquTZL9n3jtaNll+bgbowLQahHCdvnRyApcxW1N0zR03OovJ/0pe9ptgjy6XPr4VeSD7X82/03trkCVy8I2zFFGmVXHwBqiFTc7DL8/fcYLn+WBfGpDu1yhXxuUU+PfaRZHRzNCLvzDFq1o5v0ShwzdXSOPmeYsVi/st+0Y1Qsial9lb7knCs+eecEHsF09bofZYWrvXO+HDhMOWj0VgDz1afTEjx1V479HCTKLfuv1eIuudBK9buOdecjyGsZbqfSneF7/UM8qu+qCN3yWWt9DsGXDUp/CjTkjMf+9UIzi8=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }

    @Test
    public void shouldRepresentCertificateAsString() {
        CertificateDTO certificate = new CertificateDTO("Some random certificate", 1);

        BBPackage<SignedEntity<CertificateDTO>> bbPackage = new BBPackage<>(new SignedEntity<>(certificate, secretKey));

        String actual = bbPackage.getContentAsString();

        String expected = "{\"entity\":{\"cert\":\"Some random certificate\",\"id\":1},\"signature\":\"bXAGEcMuz6ehTHP6C1ts38+uf8N7i97oAaRo1os2puwKIYx9m5qV8xm1/aX1jS2Osjc4D1e9VOZuqoz/Mf1EheU0HvkPSvTRdqHGDgQ4YywskJvMsl79WQz2+f8vRNyEz4AjxGPsolEHdKm32VpuN938gi1z9dZ/EMXQwVQ6F2vFppEss6pHVFVGrQwAX/BjDCmfECrr0oOHAB7PTgYcslOiWOmrNWFi/v8C+oXSJqLK0gJjj+9KPpctra0rLpqAetfdmfi+cFB0A2hhgBer8uoJ8P9LM3wDY+QIM6hqp84D3C8q/FZVpCee0/T2m2t3TPeijG4nL/0jQY+iN/EncUzqB0bvvyIf2EakStl0DFQ1CKFih9MFqs5Ur9TWNUj6IIr5mplovR9lHKP4vXHQWSp0lczANz0Ap7cCqX1qnbzwivOFfR4brs/xuD7AdZHGdHB/AzynB0+ozrf+7ztqsrXcElGEHZFdYaAMsTNdOCh40MSXIilZ0evZ46y0+vDTjiqRYpmO5DdBE70efjeHnEiDMaT9UKmbJ5odYrbp7fNpwD0aRiCbrXA4iHadv70UgNN1qmp3EAkyXSQJZHS2zyMDuW1DjX6uEd/R11pkuU15rucEXto3nwSwX5qcgmmeI+pNfK9E93bgU2YMyW7Af8SEE+fF7UFmk0pY0S92GNQ=\"}";
        Assert.assertEquals("BBPackage content not be converted to string correctly", expected, actual);
    }
}
