package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.TestUsingBouncyCastle;
import dk.mmj.eevhe.crypto.signature.CertificateHelper;
import dk.mmj.eevhe.crypto.signature.KeyHelper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.connectors.interfaces.DKGBroadcaster;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.net.URI;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBulletinBoardDKGBroadcaster extends TestUsingBouncyCastle {
    private final HashMap<String, WebTarget> targets = new HashMap<>();
    private WebTarget target;
    private DKGBroadcaster broadcaster;
    private X509CertificateHolder daOneCert;
    private AsymmetricKeyParameter daOneSk;

    private static void assertException(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception was not thrown");
        } catch (Exception expected) {

        }
    }

    @Before
    public void setUp() throws NoSuchProviderException, NoSuchAlgorithmException, IOException, OperatorCreationException {
        AsymmetricKeyParameter sk = KeyHelper.readKey(Paths.get("certs/test_glob_key.pem"));
        AlgorithmIdentifier sha256WithRSASignature = new DefaultSignatureAlgorithmIdentifierFinder().find("SHA256WITHRSA");
        AlgorithmIdentifier digestSha = new DefaultDigestAlgorithmIdentifierFinder().find("SHA-256");

        java.security.KeyPair keyPair = KeyHelper.generateRSAKeyPair();
        X509v3CertificateBuilder cb = new X509v3CertificateBuilder(
                new X500Name("CN=EEVHE_TESTSUITE"),
                BigInteger.valueOf(1),
                new Date(), new Date(System.currentTimeMillis() + (60 * 1000)),
                new X500Name("CN=DA" + 1),
                new SubjectPublicKeyInfo(sha256WithRSASignature, keyPair.getPublic().getEncoded())
        );

        ContentSigner signer = new BcRSAContentSignerBuilder(
                sha256WithRSASignature,
                digestSha
        ).build(sk);

        daOneCert = cb.build(signer);
        daOneSk = PrivateKeyFactory.createKey(keyPair.getPrivate().getEncoded());

        target = mock(WebTarget.class);

        //Build map with all webTargets with paths
        Arrays.asList("commitments", "pedersenComplain", "pedersenComplaints", "feldmanComplain", "feldmanComplaints", "resolveComplaint", "complaintResolves")
                .forEach(s -> targets.put(s, mock(WebTarget.class)));

        //noinspection SuspiciousMethodCalls
        when(target.path(anyString())).then(i -> targets.get(i.getArgument(0)));

        WebTarget publicInfTarget = mock(WebTarget.class);
        when(target.path("publicInfo")).thenReturn(publicInfTarget);

        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(publicInfTarget.request()).thenReturn(commitBuilder);
        ObjectMapper mapper = new ObjectMapper();

        SignedEntity<PartialPublicInfo> ppi = new SignedEntity<>(new PartialPublicInfo(
                1, null, null, null, -1,
                CertificateHelper.certificateToPem(daOneCert)
        ), daOneSk);
        when(commitBuilder.get()).thenReturn(new SimpleResponse<>(200, mapper.writeValueAsString(Collections.singletonList(ppi))));

        HashMap<Integer, String> certMap = new HashMap<>();
        certMap.put(1, CertificateHelper.certificateToPem(daOneCert));
        broadcaster = new BulletinBoardDKGBroadcaster(target, daOneSk, () -> certMap);

    }

    @Test
    public void commit() {
        final WebTarget commitTarget = targets.get("commitments");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<SignedEntity<CommitmentDTO>> commitments = new ArrayList<>();
        when(target.path("commitments").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<SignedEntity<CommitmentDTO>> argument = invocationOnMock.getArgument(0);
                    commitments.add(argument.getEntity());
                    return new SimpleResponse<>(204);
                });

        final CommitmentDTO c1 = new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65, "FOO");
        final CommitmentDTO c2 = new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66, "BAR");

        broadcaster.commit(c1);
        broadcaster.commit(c2);
        assertTrue("One or more entities had unverifiable a signature", commitments.stream().allMatch(this::verify));
        assertEquals("First commitment not properly handled", c1, commitments.get(0).getEntity());
        assertEquals("Second commitment not properly handled", c2, commitments.get(1).getEntity());
    }

    @Test
    public void commitError() {
        final WebTarget commitTarget = targets.get("commitments");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("commitments").request().post(any()))
                .thenReturn(new SimpleResponse<>(500));

        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65, "FOO"));
        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66, "FOO"));
    }

    @Test
    public void getCommitments() throws JsonProcessingException {
        SignedEntity<CommitmentDTO> validCommit = new SignedEntity<>(new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 1, "BAR"), daOneSk);
        SignedEntity<CommitmentDTO> invalidCommit = new SignedEntity<>(new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 1, "BAR"), daOneSk);
        final List<SignedEntity<CommitmentDTO>> expected = Arrays.asList(validCommit, invalidCommit);

        invalidCommit.getEntity().setProtocol("FOO");

        final String commitsString = new ObjectMapper().writeValueAsString(expected.toArray());

        final WebTarget webTarget = targets.get("commitments");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("commitments").request().get(String.class)).thenReturn(commitsString);

        final List<CommitmentDTO> commitments = broadcaster.getCommitments();
        assertEquals("Only expected one commit", 1, commitments.size());
        assertEquals("Wrong commit returned", validCommit.getEntity(), commitments.get(0));
    }

    @Test
    public void getCommitmentsError() {

        final String commitsString = "stringWithError";

        final WebTarget webTarget = targets.get("commitments");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("commitments").request().get(String.class)).thenReturn(commitsString);

        assertException(broadcaster::getCommitments);
    }

    @Test
    public void pedersenComplain() {
        final WebTarget commitTarget = targets.get("pedersenComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<SignedEntity<PedersenComplaintDTO>> complaints = new ArrayList<>();
        when(target.path("pedersenComplain").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<SignedEntity<PedersenComplaintDTO>> argument = invocationOnMock.getArgument(0);
                    complaints.add(argument.getEntity());
                    return new SimpleResponse<>(204);
                });

        final PedersenComplaintDTO c1 = new PedersenComplaintDTO(2, 65);
        final PedersenComplaintDTO c2 = new PedersenComplaintDTO(2, 66);

        broadcaster.pedersenComplain(c1);
        broadcaster.pedersenComplain(c2);

        assertTrue("One or more entities had unverifiable a signature", complaints.stream().allMatch(this::verify));
        assertEquals("First complaint not properly handled", c1, complaints.get(0).getEntity());
        assertEquals("Second complaint not properly handled", c2, complaints.get(1).getEntity());
    }

    @Test
    public void feldmanComplain() {
        final WebTarget commitTarget = targets.get("feldmanComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<SignedEntity<FeldmanComplaintDTO>> complaints = new ArrayList<>();
        when(target.path("feldmanComplain").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<SignedEntity<FeldmanComplaintDTO>> argument = invocationOnMock.getArgument(0);
                    complaints.add(argument.getEntity());
                    return new SimpleResponse<>(204);
                });

        final FeldmanComplaintDTO c1 = new FeldmanComplaintDTO(1, 65, valueOf(123), valueOf(123));
        final FeldmanComplaintDTO c2 = new FeldmanComplaintDTO(1, 66, valueOf(123), valueOf(123));

        broadcaster.feldmanComplain(c1);
        broadcaster.feldmanComplain(c2);

        assertTrue("One or more entities had unverifiable a signature", complaints.stream().allMatch(this::verify));
        assertEquals("First complaint not properly handled", complaints.get(0).getEntity(), c1);
        assertEquals("Second complaint not properly handled", complaints.get(1).getEntity(), c2);
    }

    @Test
    public void pedersenComplainError() {
        final WebTarget commitTarget = targets.get("pedersenComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("pedersenComplain").request().post(any()))
                .thenReturn(new SimpleResponse<>(500));


        broadcaster.pedersenComplain(new PedersenComplaintDTO(1, 65));
        broadcaster.pedersenComplain(new PedersenComplaintDTO(1, 66));
    }

    @Test
    public void feldmanComplainError() {
        final WebTarget commitTarget = targets.get("feldmanComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("feldmanComplain").request().post(any()))
                .thenReturn(new SimpleResponse<>(500));


        broadcaster.feldmanComplain(new FeldmanComplaintDTO(1, 65, valueOf(123), valueOf(123)));
        broadcaster.feldmanComplain(new FeldmanComplaintDTO(1, 66, valueOf(123), valueOf(123)));
    }

    @Test
    public void getPedersenComplaints() throws JsonProcessingException {
        SignedEntity<PedersenComplaintDTO> complaintWrongProof = new SignedEntity<>(new PedersenComplaintDTO(1, 66), daOneSk);
        complaintWrongProof.getEntity().setSenderId(3);

        SignedEntity<PedersenComplaintDTO> includedComplaint = new SignedEntity<>(new PedersenComplaintDTO(1, 65), daOneSk);
        final List<SignedEntity<PedersenComplaintDTO>> expected = Arrays.asList(
                includedComplaint,
                complaintWrongProof);

        final String commitsString = new ObjectMapper().writeValueAsString(expected.toArray());

        final WebTarget webTarget = targets.get("pedersenComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("pedersenComplaints").request().get(String.class)).thenReturn(commitsString);

        final List<PedersenComplaintDTO> commitments = broadcaster.getPedersenComplaints();

        assertEquals("Should only have one commitment", 1, commitments.size());
        assertEquals("Wrong commitment included", includedComplaint.getEntity(), commitments.get(0));
    }

    @Test
    public void getFeldmanComplaints() throws JsonProcessingException {
        SignedEntity<FeldmanComplaintDTO> valid = new SignedEntity<>(new FeldmanComplaintDTO(1, 65, valueOf(123), valueOf(123)), daOneSk);
        SignedEntity<FeldmanComplaintDTO> invalid = new SignedEntity<>(new FeldmanComplaintDTO(1, 66, valueOf(123), valueOf(123)), daOneSk);
        final List<SignedEntity<FeldmanComplaintDTO>> expected = Arrays.asList(valid, invalid);
        invalid.getEntity().setTargetId(67);

        final String commitsString = new ObjectMapper().writeValueAsString(expected.toArray());

        final WebTarget webTarget = targets.get("feldmanComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("feldmanComplaints").request().get(String.class)).thenReturn(commitsString);

        final List<FeldmanComplaintDTO> commitments = broadcaster.getFeldmanComplaints();

        assertEquals("Should only have one complaint", 1, commitments.size());
        assertEquals("Wrong one included", valid.getEntity(), commitments.get(0));
    }

    @Test
    public void getPedersenComplaintsError() {
        final String commitsString = "stringWithError";

        final WebTarget webTarget = targets.get("pedersenComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("pedersenComplaints").request().get(String.class)).thenReturn(commitsString);

        assertException(broadcaster::getPedersenComplaints);
    }

    @Test
    public void getFeldmanComplaintsError() {
        final String commitsString = "stringWithError";

        final WebTarget webTarget = targets.get("feldmanComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("feldmanComplaints").request().get(String.class)).thenReturn(commitsString);

        assertException(broadcaster::getFeldmanComplaints);
    }

    @Test
    public void resolveComplaint() {
        final WebTarget commitTarget = targets.get("resolveComplaint");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<SignedEntity<ComplaintResolveDTO>> commitments = new ArrayList<>();
        when(target.path("resolveComplaint").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<SignedEntity<ComplaintResolveDTO>> argument = invocationOnMock.getArgument(0);
                    commitments.add(argument.getEntity());
                    return new SimpleResponse<>(204);
                });

        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);
        final ComplaintResolveDTO c1 = new ComplaintResolveDTO(2, 65, res1);
        final ComplaintResolveDTO c2 = new ComplaintResolveDTO(2, 66, res2);

        broadcaster.resolveComplaint(c1);
        broadcaster.resolveComplaint(c2);

        assertTrue("One or more entities had unverifiable a signature", commitments.stream().allMatch(this::verify));
        assertEquals("First commitment not properly handled", commitments.get(0).getEntity(), c1);
        assertEquals("Second commitment not properly handled", commitments.get(1).getEntity(), c2);
    }

    @Test
    public void resolveComplaintError() {
        final WebTarget commitTarget = targets.get("resolveComplaint");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("resolveComplaint").request().post(any()))
                .thenReturn(new SimpleResponse<>(500));

        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);
        broadcaster.resolveComplaint(new ComplaintResolveDTO(2, 65, res1));
        broadcaster.resolveComplaint(new ComplaintResolveDTO(2, 66, res2));
    }

    @Test
    public void getResolves() throws JsonProcessingException {
        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);

        SignedEntity<ComplaintResolveDTO> valid = new SignedEntity<>(new ComplaintResolveDTO(2, 1, res1), daOneSk);
        SignedEntity<ComplaintResolveDTO> invalid = new SignedEntity<>(new ComplaintResolveDTO(3, 1, res2), daOneSk);
        final List<SignedEntity<ComplaintResolveDTO>> expected = Arrays.asList(valid, invalid);
        invalid.getEntity().getValue().setSender(-2);

        final String commitsString = new ObjectMapper().writeValueAsString(expected.toArray());

        final WebTarget webTarget = targets.get("complaintResolves");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("complaintResolves").request().get(String.class)).thenReturn(commitsString);

        final List<ComplaintResolveDTO> commitments = broadcaster.getResolves();

        assertEquals("Should only have one complaint", 1, commitments.size());
        assertEquals("Wrong one included", valid.getEntity(), commitments.get(0));
    }

    @Test
    public void getResolvesError() {
        final String commitsString = "stringWithError";

        final WebTarget webTarget = targets.get("complaintResolves");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("complaintResolves").request().get(String.class)).thenReturn(commitsString);

        assertException(broadcaster::getResolves);

    }

    private boolean verify(SignedEntity<?> e) {
        try {
            return e.verifySignature(CertificateHelper.getPublicKeyFromCertificate(daOneCert));
        } catch (IOException exception) {
            fail("Failed to verify signature");
            return false;
        }
    }

    private static class SimpleResponse<T> extends Response {

        private final int status;
        private T entity;

        public SimpleResponse(int status) {
            this.status = status;
        }

        public SimpleResponse(int status, T entity) {
            this.status = status;
            this.entity = entity;
        }

        @Override
        public int getStatus() {
            return status;
        }

        @Override
        public StatusType getStatusInfo() {
            return null;
        }

        @Override
        public T getEntity() {
            return entity;
        }

        @Override
        public <R> R readEntity(Class<R> aClass) {
            //noinspection unchecked
            return (R) entity;
        }

        @Override
        public <R> R readEntity(GenericType<R> genericType) {
            return null;
        }

        @Override
        public <R> R readEntity(Class<R> aClass, Annotation[] annotations) {
            return null;
        }

        @Override
        public <R> R readEntity(GenericType<R> genericType, Annotation[] annotations) {
            return null;
        }

        @Override
        public boolean hasEntity() {
            return false;
        }

        @Override
        public boolean bufferEntity() {
            return false;
        }

        @Override
        public void close() {

        }

        @Override
        public MediaType getMediaType() {
            return null;
        }

        @Override
        public Locale getLanguage() {
            return null;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public Set<String> getAllowedMethods() {
            return null;
        }

        @Override
        public Map<String, NewCookie> getCookies() {
            return null;
        }

        @Override
        public EntityTag getEntityTag() {
            return null;
        }

        @Override
        public Date getDate() {
            return null;
        }

        @Override
        public Date getLastModified() {
            return null;
        }

        @Override
        public URI getLocation() {
            return null;
        }

        @Override
        public Set<Link> getLinks() {
            return null;
        }

        @Override
        public boolean hasLink(String s) {
            return false;
        }

        @Override
        public Link getLink(String s) {
            return null;
        }

        @Override
        public Link.Builder getLinkBuilder(String s) {
            return null;
        }

        @Override
        public MultivaluedMap<String, Object> getMetadata() {
            return null;
        }

        @Override
        public MultivaluedMap<String, String> getStringHeaders() {
            return null;
        }

        @Override
        public String getHeaderString(String s) {
            return null;
        }
    }
}
