package util;

import java.time.LocalDateTime;

/**
 * Relógio do sistema. Permite "avançar o tempo" para demonstrar prazos
 * (48 h, 15 dias, 60 dias, 90 dias) sem esperar dias reais.
 */
public class Relogio {

    private long segundosAvancados = 0;

    public LocalDateTime agora() {
        return LocalDateTime.now().plusSeconds(segundosAvancados);
    }

    public void avancarDias(int dias) {
        segundosAvancados += dias * 24L * 3600L;
    }

    public void avancarHoras(int horas) {
        segundosAvancados += horas * 3600L;
    }
}
