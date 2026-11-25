package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Cliente HTTP ligero para Telegram Bot API usando RestTemplate (B4).
 * Nunca registra URLs completas con el botToken ni cabeceras sensibles.
 */
@Component
public class TelegramBotClient {

    private static final String API_BASE = "https://api.telegram.org/bot";

    @Value("${nexus.telegram.enabled:false}")
    private boolean enabled;

    @Value("${nexus.telegram.bot-token:}")
    private String botToken;

    @Value("${nexus.telegram.poll-timeout:25}")
    private int pollTimeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private volatile RestTemplate restTemplate;

    public record InlineButton(String text, String callbackData) {}
    public record PollResult(boolean ok, int httpStatus, long retryAfterSeconds, List<JsonNode> updates) {}

    private RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            synchronized (this) {
                if (restTemplate == null) {
                    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
                    factory.setConnectTimeout(5000);
                    factory.setReadTimeout((Math.max(5, pollTimeoutSeconds) + 10) * 1000);
                    restTemplate = new RestTemplate(factory);
                }
            }
        }
        return restTemplate;
    }

    public boolean isConfigured() {
        return enabled && botToken != null && !botToken.isBlank();
    }

    public PollResult getUpdates(long offset, int timeoutSeconds) {
        if (!isConfigured()) {
            return new PollResult(false, 401, 0, List.of());
        }
        String url = API_BASE + botToken.trim() + "/getUpdates?offset=" + offset + "&timeout=" + timeoutSeconds;
        try {
            ResponseEntity<String> resp = getRestTemplate().getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            if (!root.path("ok").asBoolean(false)) {
                return new PollResult(false, resp.getStatusCode().value(), 0, List.of());
            }
            List<JsonNode> list = new ArrayList<>();
            for (JsonNode item : root.path("result")) {
                list.add(item);
            }
            return new PollResult(true, 200, 0, list);
        } catch (HttpStatusCodeException ex) {
            long retryAfter = 0;
            try {
                JsonNode err = objectMapper.readTree(ex.getResponseBodyAsString());
                retryAfter = err.path("parameters").path("retry_after").asLong(0);
            } catch (Exception ignored) {
            }
            return new PollResult(false, ex.getStatusCode().value(), retryAfter, List.of());
        } catch (ResourceAccessException ex) {
            return new PollResult(false, 504, 0, List.of());
        } catch (Exception ex) {
            return new PollResult(false, 500, 0, List.of());
        }
    }

    public boolean sendMessage(long chatId, String text, List<List<InlineButton>> inlineKeyboard) {
        if (!isConfigured()) {
            return false;
        }
        String url = API_BASE + botToken.trim() + "/sendMessage";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        if (inlineKeyboard != null && !inlineKeyboard.isEmpty()) {
            List<List<Map<String, String>>> rows = new ArrayList<>();
            for (List<InlineButton> row : inlineKeyboard) {
                List<Map<String, String>> btns = new ArrayList<>();
                for (InlineButton b : row) {
                    btns.add(Map.of("text", b.text(), "callback_data", b.callbackData()));
                }
                rows.add(btns);
            }
            body.put("reply_markup", Map.of("inline_keyboard", rows));
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            getRestTemplate().postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean answerCallbackQuery(String callbackQueryId, String text) {
        if (!isConfigured() || callbackQueryId == null || callbackQueryId.isBlank()) {
            return false;
        }
        String url = API_BASE + botToken.trim() + "/answerCallbackQuery";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (text != null && !text.isBlank()) {
            body.put("text", text);
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            getRestTemplate().postForEntity(url, new HttpEntity<>(body, headers), String.class);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isChatAdministrator(long chatId, long telegramUserId) {
        if (!isConfigured()) {
            return false;
        }
        String url = API_BASE + botToken.trim() + "/getChatMember?chat_id=" + chatId + "&user_id=" + telegramUserId;
        try {
            ResponseEntity<String> resp = getRestTemplate().getForEntity(url, String.class);
            JsonNode root = objectMapper.readTree(resp.getBody());
            String status = root.path("result").path("status").asText("");
            return "creator".equals(status) || "administrator".equals(status);
        } catch (Exception ex) {
            return false;
        }
    }
}
