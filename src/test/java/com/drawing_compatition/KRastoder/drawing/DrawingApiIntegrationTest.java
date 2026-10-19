package com.drawing_compatition.KRastoder.drawing;

import com.drawing_compatition.KRastoder.TestcontainersConfiguration;
import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class DrawingApiIntegrationTest {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02, 0x03
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUserPrincipal owner;
    private AppUserPrincipal other;

    @BeforeEach
    void setUp() {
        owner = AppUserPrincipal.from(users.save(new User("owner", "Owner", "{noop}password")));
        other = AppUserPrincipal.from(users.save(new User("other", "Other", "{noop}password")));
    }

    @Test
    void anonymousRequestIsRejected() throws Exception {
        mockMvc.perform(get("/api/drawings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsAndListsADrawingWithVoteInformation() throws Exception {
        createDrawing(owner, "A friendly robot");

        mockMvc.perform(get("/api/drawings").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].description").value("A friendly robot"))
                .andExpect(jsonPath("$[0].owner.username").value("owner"))
                .andExpect(jsonPath("$[0].voteCount").value(0))
                .andExpect(jsonPath("$[0].votedByMe").value(false));
    }

    @Test
    void listsOnlyTheCurrentUsersDrawingsNewestFirst() throws Exception {
        long first = createDrawing(owner, "First");
        long second = createDrawing(owner, "Second");
        createDrawing(other, "Someone else's");

        mockMvc.perform(get("/api/drawings/mine").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first));
    }

    @Test
    void myDrawingsAreEmptyForAUserWithoutDrawings() throws Exception {
        mockMvc.perform(get("/api/drawings/mine").with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void filtersByDayAndRangeAndReturns404WhenEmpty() throws Exception {
        createDrawing(owner, "Today's drawing");

        String today = LocalDate.now(ZoneOffset.UTC).toString();

        mockMvc.perform(get("/api/drawings").param("date", today).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/drawings")
                        .param("from", today)
                        .param("to", today)
                        .with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/drawings").param("date", "2000-01-01").with(user(owner)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail", containsString("No drawings")));

        mockMvc.perform(get("/api/drawings")
                        .param("from", "2000-01-01")
                        .param("to", "2000-01-02")
                        .with(user(owner)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/drawings")
                        .param("date", today)
                        .param("from", today)
                        .with(user(owner)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/drawings").param("from", today).with(user(owner)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void votesOnceAndCanRemoveTheVote() throws Exception {
        long drawingId = createDrawing(owner, "Vote for me");

        mockMvc.perform(post("/api/drawings/{id}/votes", drawingId).with(user(other)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(1))
                .andExpect(jsonPath("$.votedByMe").value(true));

        mockMvc.perform(post("/api/drawings/{id}/votes", drawingId).with(user(other)).with(csrf()))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/drawings/{id}/votes", drawingId).with(user(other)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/drawings/{id}/votes", drawingId).with(user(other)).with(csrf()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/drawings/{id}", drawingId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.voteCount").value(0));
    }

    @Test
    void onlyTheOwnerCanUpdateAndDeleteADrawing() throws Exception {
        long drawingId = createDrawing(owner, "Original");

        mockMvc.perform(patch("/api/drawings/{id}", drawingId)
                        .with(user(other))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateDrawingRequest("Hijacked"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/drawings/{id}", drawingId).with(user(other)).with(csrf()))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/drawings/{id}", drawingId)
                        .with(user(owner))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateDrawingRequest("Updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Updated"));

        mockMvc.perform(delete("/api/drawings/{id}", drawingId).with(user(owner)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/drawings/{id}", drawingId).with(user(owner)))
                .andExpect(status().isNotFound());
    }

    @Test
    void servesTheImageBytes() throws Exception {
        long drawingId = createDrawing(owner, "Image test");

        mockMvc.perform(get("/api/drawings/{id}/image", drawingId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES));
    }

    @Test
    void rejectsAnUnsupportedUploadType() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "note.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/drawings")
                        .file(file)
                        .with(user(owner))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void commentsCanBeCreatedUpdatedAndDeletedByTheirAuthor() throws Exception {
        long drawingId = createDrawing(owner, "Comment test");

        MvcResult created = mockMvc.perform(post("/api/drawings/{id}/comments", drawingId)
                        .with(user(other))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentPayload("Nice work"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author.username").value("other"))
                .andReturn();

        long commentId = objectMapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/drawings/{id}/comments", drawingId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(patch("/api/comments/{id}", commentId)
                        .with(user(owner))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentPayload("Edited by owner"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/comments/{id}", commentId)
                        .with(user(other))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentPayload("Edited"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body").value("Edited"));

        mockMvc.perform(delete("/api/comments/{id}", commentId).with(user(other)).with(csrf()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/drawings/{id}/comments", drawingId).with(user(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private long createDrawing(AppUserPrincipal principal, String description) throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "drawing.png", "image/png", PNG_BYTES);

        MvcResult result = mockMvc.perform(multipart("/api/drawings")
                        .file(file)
                        .param("description", description)
                        .with(user(principal))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();
    }

    private record CommentPayload(String body) {
    }
}
