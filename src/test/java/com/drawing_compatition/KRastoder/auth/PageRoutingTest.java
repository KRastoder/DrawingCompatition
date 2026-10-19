package com.drawing_compatition.KRastoder.auth;

import com.drawing_compatition.KRastoder.TestcontainersConfiguration;
import com.drawing_compatition.KRastoder.user.User;
import com.drawing_compatition.KRastoder.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class PageRoutingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    private AppUserPrincipal signedIn;

    @BeforeEach
    void setUp() {
        signedIn = AppUserPrincipal.from(users.save(new User("pageuser", "Page User", "{noop}password")));
    }

    @Test
    void anonymousVisitorsAreSentToTheLoginPage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void signedInUsersReachEachApplicationPage() throws Exception {
        mockMvc.perform(get("/").with(user(signedIn)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/home.html"));

        mockMvc.perform(get("/newdrawing").with(user(signedIn)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/newdrawing.html"));

        mockMvc.perform(get("/my-drawings").with(user(signedIn)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/my-drawings.html"));

        mockMvc.perform(get("/profile").with(user(signedIn)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/profile.html"));

        mockMvc.perform(get("/profile/123").with(user(signedIn)))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/profile.html"));
    }
}
