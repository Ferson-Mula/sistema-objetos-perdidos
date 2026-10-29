package service;

import java.time.LocalDateTime;

/**
 * Relógio do sistema. Pode ser avançado à mão para simular a passagem do tempo
 * (assim vemos os prazos de 30, 60 e 90 dias sem esperar semanas).
 */
public class Relogio {

    private LocalDateTime agora = LocalDateTime.now().withSecond(0).withNano(0);

    public LocalDateTime agora() {
        return agora;
    }

    public void avancarHoras(long horas) {
        agora = agora.plusHours(horas);
    }

    public void avancarDias(long dias) {
        agora = agora.plusDays(dias);
    }
}
