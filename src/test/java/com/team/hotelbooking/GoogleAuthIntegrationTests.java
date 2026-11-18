package com.team.hotelbooking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team.hotelbooking.identity.api.AuthCookieManager;
import com.team.hotelbooking.identity.application.usecase.impl.GoogleAccountService.VerifiedAccount;
import com.team.hotelbooking.identity.infrastructure.integration.GoogleIdTokens;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class GoogleAuthIntegrationTests {
  private static final String PATH = "/api/v1/auth/google";
  private static final String CLIENT_ID = "test.apps.googleusercontent.com";
  private static final String EMAIL = "google-auth-it@gmail.com";
  private static final String SUBJECT = "google-auth-it";

  @Autowired private MockMvc mvc;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private ObjectMapper mapper;
  @MockitoBean private GoogleIdTokens google;

  @Test
  void createsAndReusesGoogleAccountAndIssuesTheNormalSessionCookies() throws Exception {
    when(google.clientId()).thenReturn(CLIENT_ID);
    Cookie enrollmentNonce = challenge();
    when(google.verify("first-credential", enrollmentNonce.getValue()))
        .thenReturn(new VerifiedAccount(SUBJECT, EMAIL, "Google IT", true));

    mvc.perform(
            post(PATH)
                .cookie(enrollmentNonce)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"first-credential\"}"))
        .andExpect(status().is(422))
        .andExpect(jsonPath("$.code").value("GOOGLE_PHONE_REQUIRED"))
        .andExpect(header().doesNotExist("Set-Cookie"));

    mvc.perform(
            post(PATH)
                .cookie(enrollmentNonce)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"first-credential\",\"phone\":\"0981116543\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.email").value(EMAIL))
        .andExpect(jsonPath("$.data.user.emailVerified").value(true))
        .andExpect(
            header()
                .stringValues(
                    "Set-Cookie",
                    hasItems(
                        containsString("Max-Age=0"),
                        containsString("refresh_token="),
                        containsString("XSRF-TOKEN="))));

    UUID userId =
        jdbc.queryForObject(
            "SELECT user_id FROM google_identities WHERE subject = ?", UUID.class, SUBJECT);
    assertThat(userId).isNotNull();

    Cookie returningNonce = challenge();
    when(google.verify("returning-credential", returningNonce.getValue()))
        .thenReturn(new VerifiedAccount(SUBJECT, EMAIL, "Google IT", true));
    mvc.perform(
            post(PATH)
                .cookie(returningNonce)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"returning-credential\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.user.id").value(userId.toString()));

    Cookie collisionNonce = challenge();
    when(google.verify("unlinked-credential", collisionNonce.getValue()))
        .thenReturn(new VerifiedAccount("other-subject", EMAIL, "Other", true));
    mvc.perform(
            post(PATH)
                .cookie(collisionNonce)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"credential\":\"unlinked-credential\"}"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
  }

  private Cookie challenge() throws Exception {
    var response =
        mvc.perform(get(PATH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.clientId").value(CLIENT_ID))
            .andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Set-Cookie", containsString("SameSite=Strict")))
            .andReturn()
            .getResponse();
    String nonce =
        mapper.readTree(response.getContentAsString()).path("data").path("nonce").asText();
    assertThat(nonce).isNotBlank();
    return new Cookie(AuthCookieManager.GOOGLE_NONCE_COOKIE_NAME, nonce);
  }
}
