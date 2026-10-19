import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

public class Planificador {

    /**
     * Round Robin (Ronda). Si un proceso llega justo cuando termina un quantum,
     * entra a la cola ANTES que el proceso que fue expulsado.
     */
    public static List<Segmento> ronda(List<Proceso> procesos, int quantum) {
        List<Proceso> pend = new ArrayList<>(procesos);
        pend.sort(Comparator.<Proceso>comparingInt(p -> p.llegada)
                .thenComparing(p -> p.nombre));

        List<Segmento> linea = new ArrayList<>();
        Deque<Proceso> cola = new ArrayDeque<>();
        int t = 0, idx = 0, terminados = 0;
        int n = pend.size();

        while (idx < n && pend.get(idx).llegada <= t) cola.addLast(pend.get(idx++));

        while (terminados < n) {
            if (cola.isEmpty()) {
                // CPU ociosa hasta la siguiente llegada
                int tSig = pend.get(idx).llegada;
                linea.add(new Segmento(t, tSig, "-"));
                t = tSig;
                while (idx < n && pend.get(idx).llegada <= t) cola.addLast(pend.get(idx++));
                continue;
            }
            Proceso p = cola.pollFirst();
            if (p.inicio < 0) p.inicio = t;
            int ejec = Math.min(quantum, p.restante);
            linea.add(new Segmento(t, t + ejec, p.nombre));
            t += ejec;
            p.restante -= ejec;
            while (idx < n && pend.get(idx).llegada <= t) cola.addLast(pend.get(idx++));
            if (p.restante > 0) {
                cola.addLast(p);
            } else {
                p.fin = t;
                terminados++;
            }
        }
        return linea;
    }

    /** Tiempo restante mas corto (apropiativo). Desempate: el que llego primero. */
    public static List<Segmento> srtf(List<Proceso> procesos) {
        List<Segmento> linea = new ArrayList<>();
        int t = 0, terminados = 0;
        int n = procesos.size();

        while (terminados < n) {
            Proceso elegido = null;
            for (Proceso p : procesos) {
                if (p.restante > 0 && p.llegada <= t) {
                    if (elegido == null
                            || p.restante < elegido.restante
                            || (p.restante == elegido.restante && p.llegada < elegido.llegada)
                            || (p.restante == elegido.restante && p.llegada == elegido.llegada
                                && p.nombre.compareTo(elegido.nombre) < 0)) {
                        elegido = p;
                    }
                }
            }
            if (elegido == null) {
                // CPU ociosa hasta la siguiente llegada
                int tSig = Integer.MAX_VALUE;
                for (Proceso p : procesos) {
                    if (p.restante > 0 && p.llegada < tSig) tSig = p.llegada;
                }
                linea.add(new Segmento(t, tSig, "-"));
                t = tSig;
                continue;
            }
            if (elegido.inicio < 0) elegido.inicio = t;

            // Se ejecuta 1 unidad de tiempo (permite expropiar en cada instante)
            Segmento ultimo = linea.isEmpty() ? null : linea.get(linea.size() - 1);
            if (ultimo != null && ultimo.nombre.equals(elegido.nombre) && ultimo.fin == t) {
                ultimo.fin = t + 1;
            } else {
                linea.add(new Segmento(t, t + 1, elegido.nombre));
            }
            elegido.restante--;
            t++;
            if (elegido.restante == 0) {
                elegido.fin = t;
                terminados++;
            }
        }
        return linea;
    }
}
