package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.*;
import dk.mmj.eevhe.protocols.connectors.interfaces.Broadcaster;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.lang.annotation.Annotation;
import java.math.BigInteger;
import java.net.URI;
import java.util.*;

import static java.math.BigInteger.valueOf;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBulletinBoardBroadcaster {
    private final HashMap<String, WebTarget> targets = new HashMap<>();
    private WebTarget target;
    private Broadcaster broadcaster;

    private static void assertException(Runnable runnable) {
        try {
            runnable.run();
            fail("Expected exception was not thrown");
        } catch (Exception expected) {

        }
    }

    @Before
    public void setUp() {
        target = mock(WebTarget.class);

        //Build map with all webTargets with paths
        Arrays.asList("commitments", "pedersenComplain", "pedersenComplaints", "feldmanComplain", "feldmanComplaints", "resolveComplaint", "complaintResolves")
                .forEach(s -> targets.put(s, mock(WebTarget.class)));

        //noinspection SuspiciousMethodCalls
        when(target.path(anyString())).then(i -> targets.get(i.getArgument(0)));


        broadcaster = new BulletinBoardBroadcaster(target);
    }

    @Test
    public void commit() {
        final WebTarget commitTarget = targets.get("commitments");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<CommitmentDTO> commitments = new ArrayList<>();
        when(target.path("commitments").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<CommitmentDTO> argument = invocationOnMock.getArgument(0);
                    commitments.add(argument.getEntity());
                    return new SimpleResponse(204);
                });

        final CommitmentDTO c1 = new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65, "FOO");
        final CommitmentDTO c2 = new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66, "BAR");

        broadcaster.commit(c1);
        broadcaster.commit(c2);
        assertTrue("First commitment not properly handled", commitments.contains(c1));
        assertTrue("Second commitment not properly handled", commitments.contains(c2));
    }

    @Test
    public void commitError() {
        final WebTarget commitTarget = targets.get("commitments");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("commitments").request().post(any()))
                .thenReturn(new SimpleResponse(500));

        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65, "FOO"));
        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66, "FOO"));
    }

    @Test
    public void getCommitments() throws JsonProcessingException {
        final List<CommitmentDTO> expected = Arrays.asList(
                new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65, "BAR"),
                new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66, "BAR"));

        final String commitsString = new ObjectMapper().writeValueAsString(expected);

        final WebTarget webTarget = targets.get("commitments");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("commitments").request().get(String.class)).thenReturn(commitsString);

        final List<CommitmentDTO> commitments = broadcaster.getCommitments();
        assertArrayEquals("Commitments were not equal to those sent through webTarget",
                expected.toArray(),
                commitments.toArray());
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

        final List<PedersenComplaintDTO> complaints = new ArrayList<>();
        when(target.path("pedersenComplain").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<PedersenComplaintDTO> argument = invocationOnMock.getArgument(0);
                    complaints.add(argument.getEntity());
                    return new SimpleResponse(204);
                });

        final PedersenComplaintDTO c1 = new PedersenComplaintDTO(2, 65);
        final PedersenComplaintDTO c2 = new PedersenComplaintDTO(2, 66);

        broadcaster.pedersenComplain(c1);
        broadcaster.pedersenComplain(c2);
        assertTrue("First complaint not properly handled", complaints.contains(c1));
        assertTrue("Second complaint not properly handled", complaints.contains(c2));
    }

    @Test
    public void feldmanComplain() {
        final WebTarget commitTarget = targets.get("feldmanComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<FeldmanComplaintDTO> complaints = new ArrayList<>();
        when(target.path("feldmanComplain").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<FeldmanComplaintDTO> argument = invocationOnMock.getArgument(0);
                    complaints.add(argument.getEntity());
                    return new SimpleResponse(204);
                });

        final FeldmanComplaintDTO c1 = new FeldmanComplaintDTO(2, 65, valueOf(123), valueOf(123));
        final FeldmanComplaintDTO c2 = new FeldmanComplaintDTO(2, 66, valueOf(123), valueOf(123));

        broadcaster.feldmanComplain(c1);
        broadcaster.feldmanComplain(c2);
        assertTrue("First complaint not properly handled", complaints.contains(c1));
        assertTrue("Second complaint not properly handled", complaints.contains(c2));
    }

    @Test
    public void pedersenComplainError() {
        final WebTarget commitTarget = targets.get("pedersenComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("pedersenComplain").request().post(any()))
                .thenReturn(new SimpleResponse(500));


        broadcaster.pedersenComplain(new PedersenComplaintDTO(2, 65));
        broadcaster.pedersenComplain(new PedersenComplaintDTO(2, 66));
    }

    @Test
    public void feldmanComplainError() {
        final WebTarget commitTarget = targets.get("feldmanComplain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("feldmanComplain").request().post(any()))
                .thenReturn(new SimpleResponse(500));


        broadcaster.feldmanComplain(new FeldmanComplaintDTO(2, 65, valueOf(123), valueOf(123)));
        broadcaster.feldmanComplain(new FeldmanComplaintDTO(2, 66, valueOf(123), valueOf(123)));
    }

    @Test
    public void getPedersenComplaints() throws JsonProcessingException {
        final List<PedersenComplaintDTO> expected = Arrays.asList(
                new PedersenComplaintDTO(2, 65),
                new PedersenComplaintDTO(2, 66));

        final String commitsString = new ObjectMapper().writeValueAsString(expected);

        final WebTarget webTarget = targets.get("pedersenComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("pedersenComplaints").request().get(String.class)).thenReturn(commitsString);

        final List<PedersenComplaintDTO> commitments = broadcaster.getPedersenComplaints();
        assertArrayEquals("Commitments were not equal to those sent through webTarget",
                expected.toArray(),
                commitments.toArray());
    }

    @Test
    public void getFeldmanComplaints() throws JsonProcessingException {
        final List<FeldmanComplaintDTO> expected = Arrays.asList(
                new FeldmanComplaintDTO(2, 65, valueOf(123), valueOf(123)),
                new FeldmanComplaintDTO(2, 66, valueOf(123), valueOf(123)));

        final String commitsString = new ObjectMapper().writeValueAsString(expected);

        final WebTarget webTarget = targets.get("feldmanComplaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("feldmanComplaints").request().get(String.class)).thenReturn(commitsString);

        final List<FeldmanComplaintDTO> commitments = broadcaster.getFeldmanComplaints();
        assertArrayEquals("Commitments were not equal to those sent through webTarget",
                expected.toArray(),
                commitments.toArray());
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

        final List<ComplaintResolveDTO> commitments = new ArrayList<>();
        when(target.path("resolveComplaint").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<ComplaintResolveDTO> argument = invocationOnMock.getArgument(0);
                    commitments.add(argument.getEntity());
                    return new SimpleResponse(204);
                });

        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);
        final ComplaintResolveDTO c1 = new ComplaintResolveDTO(2, 65, res1);
        final ComplaintResolveDTO c2 = new ComplaintResolveDTO(2, 66, res2);

        broadcaster.resolveComplaint(c1);
        broadcaster.resolveComplaint(c2);
        assertTrue("First commitment not properly handled", commitments.contains(c1));
        assertTrue("Second commitment not properly handled", commitments.contains(c2));
    }

    @Test
    public void resolveComplaintError() {
        final WebTarget commitTarget = targets.get("resolveComplaint");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("resolveComplaint").request().post(any()))
                .thenReturn(new SimpleResponse(500));

        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);
        broadcaster.resolveComplaint(new ComplaintResolveDTO(2, 65, res1));
        broadcaster.resolveComplaint(new ComplaintResolveDTO(2, 66, res2));
    }

    @Test
    public void getResolves() throws JsonProcessingException {
        PartialSecretMessageDTO res1 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 65, 2);
        PartialSecretMessageDTO res2 = new PartialSecretMessageDTO(new BigInteger("56138131"), new BigInteger("2342429"), 66, 2);
        final List<ComplaintResolveDTO> expected = Arrays.asList(
                new ComplaintResolveDTO(2, 65, res1),
                new ComplaintResolveDTO(2, 66, res2));

        final String commitsString = new ObjectMapper().writeValueAsString(expected);

        final WebTarget webTarget = targets.get("complaintResolves");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("complaintResolves").request().get(String.class)).thenReturn(commitsString);

        final List<ComplaintResolveDTO> commitments = broadcaster.getResolves();
        assertArrayEquals("Commitments were not equal to those sent through webTarget",
                expected.toArray(),
                commitments.toArray());
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

    private static class SimpleResponse extends Response {
        private final int status;

        public SimpleResponse(int status) {
            this.status = status;
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
        public Object getEntity() {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> aClass) {
            return null;
        }

        @Override
        public <T> T readEntity(GenericType<T> genericType) {
            return null;
        }

        @Override
        public <T> T readEntity(Class<T> aClass, Annotation[] annotations) {
            return null;
        }

        @Override
        public <T> T readEntity(GenericType<T> genericType, Annotation[] annotations) {
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
