package dk.mmj.eevhe.protocols.connectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dk.mmj.eevhe.entities.CommitmentDTO;
import dk.mmj.eevhe.entities.ComplaintDTO;
import dk.mmj.eevhe.entities.ComplaintResolveDTO;
import dk.mmj.eevhe.entities.PartialSecretMessageDTO;
import dk.mmj.eevhe.protocols.interfaces.Broadcaster;
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
        Arrays.asList("commitments", "complain", "complaints", "resolveComplaint", "complaintResolves")
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

        final CommitmentDTO c1 = new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65);
        final CommitmentDTO c2 = new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66);

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

        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65));
        broadcaster.commit(new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66));
    }

    @Test
    public void getCommitments() throws JsonProcessingException {
        final List<CommitmentDTO> expected = Arrays.asList(
                new CommitmentDTO(new BigInteger[]{valueOf(5), valueOf(654)}, 65),
                new CommitmentDTO(new BigInteger[]{valueOf(85), valueOf(65)}, 66));

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
    public void complain() {
        final WebTarget commitTarget = targets.get("complain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        final List<ComplaintDTO> complaints = new ArrayList<>();
        when(target.path("complain").request().post(any()))
                .then(invocationOnMock -> {
                    final Entity<ComplaintDTO> argument = invocationOnMock.getArgument(0);
                    complaints.add(argument.getEntity());
                    return new SimpleResponse(204);
                });

        final ComplaintDTO c1 = new ComplaintDTO(2, 65);
        final ComplaintDTO c2 = new ComplaintDTO(2, 66);

        broadcaster.complain(c1);
        broadcaster.complain(c2);
        assertTrue("First complaint not properly handled", complaints.contains(c1));
        assertTrue("Second complaint not properly handled", complaints.contains(c2));
    }

    @Test
    public void complainError() {
        final WebTarget commitTarget = targets.get("complain");
        final Invocation.Builder commitBuilder = mock(Invocation.Builder.class);
        when(commitTarget.request()).thenReturn(commitBuilder);

        when(target.path("complain").request().post(any()))
                .thenReturn(new SimpleResponse(500));


        broadcaster.complain(new ComplaintDTO(2, 65));
        broadcaster.complain(new ComplaintDTO(2, 66));
    }

    @Test
    public void getComplaints() throws JsonProcessingException {
        final List<ComplaintDTO> expected = Arrays.asList(
                new ComplaintDTO(2, 65),
                new ComplaintDTO(2, 66));

        final String commitsString = new ObjectMapper().writeValueAsString(expected);

        final WebTarget webTarget = targets.get("complaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("complaints").request().get(String.class)).thenReturn(commitsString);

        final List<ComplaintDTO> commitments = broadcaster.getComplaints();
        assertArrayEquals("Commitments were not equal to those sent through webTarget",
                expected.toArray(),
                commitments.toArray());
    }

    @Test
    public void getComplaintsError() {
        final String commitsString = "stringWithError";

        final WebTarget webTarget = targets.get("complaints");
        final Invocation.Builder builder = mock(Invocation.Builder.class);
        when(webTarget.request()).thenReturn(builder);
        when(target.path("complaints").request().get(String.class)).thenReturn(commitsString);

        assertException(broadcaster::getComplaints);
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
