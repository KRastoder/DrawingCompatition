package com.drawing_compatition.KRastoder.user;

import com.drawing_compatition.KRastoder.TestcontainersConfiguration;
import com.drawing_compatition.KRastoder.auth.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProfileApiIntegrationTest {

    private static final byte[] PNG_BYTES = new byte[]{
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x01, 0x02, 0x03
    };

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private ObjectMapper objectMapper;

    private User artist;
    private AppUserPrincipal artistPrincipal;

    @BeforeEach
    void setUp() {
        artist = users.save(new User("profileuser", "Profile User", "{noop}password"));
        artistPrincipal = AppUserPrincipal.from(artist);
    }

    @Test
    void returnsTheSignedInUsersProfile() throws Exception {
        mockMvc.perform(get("/api/profile").with(user(artistPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(artist.getId()))
                .andExpect(jsonPath("$.username").value("profileuser"))
                .andExpect(jsonPath("$.displayName").value("Profile User"));
    }

    @Test
    void updatesTheProfileDescription() throws Exception {
        mockMvc.perform(patch("/api/profile")
                        .with(user(artistPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest("  I draw robots and strange plants.  ")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("I draw robots and strange plants."));

        mockMvc.perform(get("/api/users/{id}", artist.getId()).with(user(artistPrincipal)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("I draw robots and strange plants."));
    }

    @Test
    void aBlankDescriptionIsStoredAsEmpty() throws Exception {
        mockMvc.perform(patch("/api/profile")
                        .with(user(artistPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("   "))))
                .andExpect(status().isOk());

        User saved = users.findById(artist.getId()).orElseThrow();
        assertThat(saved.getBio()).isNull();
    }

    @Test
    void rejectsADescriptionThatIsTooLong() throws Exception {
        mockMvc.perform(patch("/api/profile")
                        .with(user(artistPrincipal))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateProfileRequest("a".repeat(ProfileService.MAX_BIO_LENGTH + 1))
                        )))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadsServesAndRemovesAProfilePicture() throws Exception {
        MockMultipartFile avatar = new MockMultipartFile("image", "avatar.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/profile/avatar")
                        .file(avatar)
                        .with(user(artistPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value("/api/users/" + artist.getId() + "/avatar"));

        mockMvc.perform(get("/api/users/{id}/avatar", artist.getId()).with(user(artistPrincipal)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(PNG_BYTES))
                .andExpect(header().string("Cache-Control", containsString("no-cache")));

        mockMvc.perform(delete("/api/profile/avatar")
                        .with(user(artistPrincipal))
                        .with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/{id}/avatar", artist.getId()).with(user(artistPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnUnsupportedProfilePictureType() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "note.txt",
                "text/plain",
                "hello".getBytes()
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/profile/avatar")
                        .file(file)
                        .with(user(artistPrincipal))
                        .with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returns404ForAnUnknownProfile() throws Exception {
        mockMvc.perform(get("/api/users/{id}", Long.MAX_VALUE).with(user(artistPrincipal)))
                .andExpect(status().isNotFound());
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/profile"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/{id}", artist.getId()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/users/{id}/avatar", artist.getId()))
                .andExpect(status().isUnauthorized());
    }
}
