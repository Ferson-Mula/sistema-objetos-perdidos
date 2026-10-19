package util;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Funções auxiliares para datas. */
public final class Datas {

    private Datas() {
    }

    /** Soma dias úteis (segunda a sexta) a uma data. */
    public static LocalDate somarDiasUteis(LocalDate data, int dias) {
        LocalDate d = data;
        int somados = 0;
        while (somados < dias) {
            d = d.plusDays(1);
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                somados++;
            }
        }
        return d;
    }
}
