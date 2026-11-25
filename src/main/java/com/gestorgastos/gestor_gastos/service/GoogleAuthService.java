package com.gestorgastos.gestor_gastos.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;

@Service
public class GoogleAuthService {

    private static final String PLACEHOLDER = "YOUR_GOOGLE_CLIENT_ID";

    private static final Set<String> EMISORES_VALIDOS =
            Set.of("accounts.google.com", "https://accounts.google.com");

    @Value("${google.client.id:YOUR_GOOGLE_CLIENT_ID.apps.googleusercontent.com}")
    private String googleClientId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleAuthService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    public record GoogleUserInfo(String sub, String email, String name, String picture) {}

    /**
     * Valida el ID token con Google. Si CUALQUIER cosa falla, devuelve vacío
     * (nunca se "adivina" el usuario a partir de un token sin verificar).
     */
    public Optional<GoogleUserInfo> verificarToken(String idToken) {

        // Sin Client ID real configurado, no se acepta ningún token
        if (idToken == null || idToken.isBlank()
                || googleClientId == null
                || googleClientId.contains(PLACEHOLDER)) {
            return Optional.empty();
        }

        try {
            // Google verifica la firma y la expiración del token
            String url = "https://oauth2.googleapis.com/tokeninfo?id_token="
                    + URLEncoder.encode(idToken, StandardCharsets.UTF_8);

            String response = restTemplate.getForObject(url, String.class);

            if (response == null) {
                return Optional.empty();
            }

            JsonNode root = objectMapper.readTree(response);

            // El token tiene que haber sido emitido PARA esta aplicación.
            // Sin esto, un token válido de cualquier otra app serviría aquí.
            if (!googleClientId.equals(root.path("aud").asText())) {
                return Optional.empty();
            }

            // Emitido por Google
            if (!EMISORES_VALIDOS.contains(root.path("iss").asText())) {
                return Optional.empty();
            }

            // Correo verificado por Google
            if (!"true".equalsIgnoreCase(root.path("email_verified").asText())) {
                return Optional.empty();
            }

            String sub = root.path("sub").asText(null);
            String email = root.path("email").asText(null);
            String name = root.path("name").asText(null);
            String picture = root.path("picture").asText(null);

            if (sub == null || sub.isBlank() || email == null || email.isBlank()) {
                return Optional.empty();
            }

            return Optional.of(new GoogleUserInfo(
                    sub.trim(),
                    email.trim().toLowerCase(),
                    name != null && !name.isBlank() ? name.trim() : email.split("@")[0],
                    picture
            ));

        } catch (Exception e) {
            // Token inválido, expirado, o Google no respondió: se rechaza.
            return Optional.empty();
        }
    }

    public String getGoogleClientId() {
        return googleClientId;
    }
}