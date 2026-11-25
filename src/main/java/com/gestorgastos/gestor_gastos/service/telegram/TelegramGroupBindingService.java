package com.gestorgastos.gestor_gastos.service.telegram;

import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.VinculacionChatGrupo;
import com.gestorgastos.gestor_gastos.repository.VinculacionChatGrupoRepository;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class TelegramGroupBindingService {

    @Value("${nexus.telegram.groups-enabled:false}")
    private boolean groupsEnabled;

    private final VinculacionChatGrupoRepository vinculacionRepository;
    private final TelegramAccountLinkService linkService;
    private final GrupoAccessService grupoAccessService;
    private final TelegramBotClient botClient;

    public TelegramGroupBindingService(
            VinculacionChatGrupoRepository vinculacionRepository,
            TelegramAccountLinkService linkService,
            GrupoAccessService grupoAccessService,
            TelegramBotClient botClient) {
        this.vinculacionRepository = vinculacionRepository;
        this.linkService = linkService;
        this.grupoAccessService = grupoAccessService;
        this.botClient = botClient;
    }

    @Transactional
    public String vincularChat(Long chatId, Long telegramUserId, Long grupoId) {
        if (!groupsEnabled) {
            return "La integración con grupos de Telegram está deshabilitada.";
        }
        CuentaCanal cuenta = linkService.obtenerCuentaActivaPorTelegramId(telegramUserId).orElse(null);
        if (cuenta == null) {
            return "Debes vincular tu cuenta NEXUS en privado antes de asociar un grupo.";
        }
        Grupo grupo = grupoAccessService.findGrupoSiEsMiembroActivo(grupoId, cuenta.getUsuario()).orElse(null);
        if (grupo == null) {
            return "No eres miembro activo de ese grupo en NEXUS.";
        }
        if (!botClient.isChatAdministrator(chatId, telegramUserId)) {
            return "Solo un administrador del chat de Telegram puede vincular este grupo.";
        }
        Optional<VinculacionChatGrupo> existente = vinculacionRepository.findByChatIdAndActivaTrue(chatId);
        if (existente.isPresent()) {
            return "Este chat ya está vinculado al grupo \"" + existente.get().getGrupo().getName() + "\".";
        }

        VinculacionChatGrupo v = new VinculacionChatGrupo();
        v.setChatId(chatId);
        v.setGrupo(grupo);
        v.setVinculadoPor(cuenta.getUsuario());
        v.setActiva(true);
        v.setFechaVinculacion(LocalDateTime.now());
        vinculacionRepository.save(v);

        return "Chat vinculado correctamente al grupo NEXUS \"" + grupo.getName() + "\".";
    }

    public Optional<VinculacionChatGrupo> obtenerVinculacionActiva(Long chatId) {
        return vinculacionRepository.findByChatIdAndActivaTrue(chatId);
    }
}
