import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

/**
 * Planificacion de procesos (apropiativos): Ronda (Round Robin) y
 * Tiempo restante mas corto (SRTF).
 *
 * Validaciones:
 *  - Tiempo de llegada: 0 a 20
 *  - Tiempo de CPU y Quantum: 1 a 20
 *
 * El diagrama de ejecucion se muestra con colores de fondo ANSI.
 * Si la terminal no los soporta, defina la variable de entorno NO_COLOR=1.
 */
public class Main {
    static final int MAX_VAL = 20;
    static final int MAX_PROCESOS = 26;
    static final int POR_DEFECTO = 7;      // numero de procesos por defecto (> 5)
    static final int CELDAS_POR_LINEA = 30; // unidades de tiempo por renglon del diagrama

    static final String RESET = "\u001B[0m";
    static final boolean COLOR = System.getenv("NO_COLOR") == null;

    // Mismos colores del ejemplo: A verde claro, B rojo, C amarillo, D naranja, E morado, F verde, G celeste
    static final int[][] BASE = {
        {155, 187, 89}, {192, 0, 0}, {255, 255, 0}, {237, 125, 49},
        {112, 48, 160}, {0, 176, 80}, {146, 208, 224}
    };

    static final Scanner in = new Scanner(System.in);

    // Datos ingresados (se conservan para poder correr ambos algoritmos con los mismos datos)
    static int[] llegadas = null;
    static int[] cpus = null;

    /* ------------------------------ Entrada ------------------------------ */

    static String leerLinea() {
        if (!in.hasNextLine()) {
            System.out.println();
            System.exit(0);
        }
        return in.nextLine().trim();
    }

    /** Pide un entero en [min, max]; repite hasta que sea valido. */
    static int leerEntero(String mensaje, int min, int max) {
        while (true) {
            System.out.print(mensaje + " (" + min + "-" + max + "): ");
            String t = leerLinea();
            try {
                int v = Integer.parseInt(t);
                if (v >= min && v <= max) return v;
                System.out.println("  Error: el valor debe estar entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Error: debe ingresar un numero entero.");
            }
        }
    }

    /** Igual que leerEntero, pero si el usuario solo presiona Enter se usa el valor por defecto. */
    static int leerEnteroConDefecto(String mensaje, int min, int max, int defecto) {
        while (true) {
            System.out.print(mensaje + " (" + min + "-" + max + ", Enter = " + defecto + "): ");
            String t = leerLinea();
            if (t.isEmpty()) return defecto;
            try {
                int v = Integer.parseInt(t);
                if (v >= min && v <= max) return v;
                System.out.println("  Error: el valor debe estar entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Error: debe ingresar un numero entero.");
            }
        }
    }

    static void pedirDatos() {
        System.out.println("\n--- Ingreso de procesos ---");
        int n = leerEnteroConDefecto("Numero de procesos", 1, MAX_PROCESOS, POR_DEFECTO);
        llegadas = new int[n];
        cpus = new int[n];
        for (int i = 0; i < n; i++) {
            String nom = Proceso.nombreProceso(i);
            llegadas[i] = leerEntero("Proceso " + nom + " - tiempo de llegada", 0, MAX_VAL);
            cpus[i] = leerEntero("Proceso " + nom + " - tiempo de CPU", 1, MAX_VAL);
        }
    }

    /* ------------------------------ Colores ------------------------------ */

    static int[] rgbProceso(int i) {
        if (i < BASE.length) return BASE[i];
        Color c = Color.getHSBColor(((i * 47) % 360) / 360f, 0.55f, 0.90f);
        return new int[] {c.getRed(), c.getGreen(), c.getBlue()};
    }

    /** Texto con fondo de color y fuente negra o blanca segun la luminosidad. */
    static String conFondo(String texto, int[] rgb) {
        if (!COLOR) return texto;
        double lum = 0.299 * rgb[0] + 0.587 * rgb[1] + 0.114 * rgb[2];
        int f = lum > 150 ? 0 : 255;
        return "\u001B[48;2;" + rgb[0] + ";" + rgb[1] + ";" + rgb[2] + "m"
             + "\u001B[38;2;" + f + ";" + f + ";" + f + "m" + texto + RESET;
    }

    static String enRojo(String texto) {
        return COLOR ? "\u001B[1;31m" + texto + RESET : texto;
    }

    /* ------------------------------ Salida ------------------------------ */

    static void imprimirGantt(List<Segmento> linea, List<Proceso> procesos) {
        List<String> nombres = new ArrayList<>();
        for (Proceso p : procesos) nombres.add(p.nombre);

        // Una celda por unidad de tiempo
        List<String> celdas = new ArrayList<>();
        for (Segmento s : linea) {
            for (int t = s.ini; t < s.fin; t++) celdas.add(s.nombre);
        }
        int total = celdas.size();

        System.out.println("Orden de ejecucion:");
        for (int ini = 0; ini < total; ini += CELDAS_POR_LINEA) {
            int fin = Math.min(ini + CELDAS_POR_LINEA, total);
            StringBuilder fila = new StringBuilder();
            StringBuilder tiempos = new StringBuilder();
            for (int t = ini; t < fin; t++) {
                String nom = celdas.get(t);
                if (nom.equals("-")) {
                    fila.append(conFondo(" -  ", new int[] {210, 210, 210}));
                } else {
                    fila.append(conFondo(" " + nom + "  ", rgbProceso(nombres.indexOf(nom))));
                }
                tiempos.append(String.format("%-4d", t));
            }
            if (fin == total) tiempos.append(total);
            System.out.println(fila);
            System.out.println(tiempos);
            System.out.println();
        }
    }

    static void imprimirTabla(List<Proceso> procesos) {
        System.out.printf("%-9s%-12s%-10s%-10s%-12s%-10s%-12s%n",
                "Proceso", "T. Llegada", "T. CPU", "T. Final", "T. Retorno", "T. Espera", "T. Promedio");
        double sRet = 0, sEsp = 0, sProm = 0;
        for (int i = 0; i < procesos.size(); i++) {
            Proceso p = procesos.get(i);
            sRet += p.retorno();
            sEsp += p.espera();
            sProm += p.promedio();
            System.out.print(conFondo(" " + p.nombre + "       ", rgbProceso(i)));
            System.out.printf(Locale.US, "%-12d%-10d%-10d%-12d%-10d%-12.2f%n",
                    p.llegada, p.cpu, p.fin, p.retorno(), p.espera(), p.promedio());
        }
        int n = procesos.size();
        System.out.print(String.format(Locale.US, "%-9s%-12s%-10s%-10s%-12.2f%-10.2f",
                "Promedios", "", "", "", sRet / n, sEsp / n));
        System.out.println(enRojo(String.format(Locale.US, "%.2f", sProm / n)));
    }

    /* ------------------------------ Ejecucion ------------------------------ */

    static void ejecutar(int algoritmo) {
        int quantum = 0;
        if (algoritmo == 1) quantum = leerEntero("Quantum de ejecucion", 1, MAX_VAL);

        // Procesos nuevos en cada corrida (los algoritmos modifican el estado)
        List<Proceso> procesos = new ArrayList<>();
        for (int i = 0; i < llegadas.length; i++) {
            procesos.add(new Proceso(Proceso.nombreProceso(i), llegadas[i], cpus[i]));
        }

        List<Segmento> linea = (algoritmo == 1)
                ? Planificador.ronda(procesos, quantum)
                : Planificador.srtf(procesos);

        System.out.println();
        System.out.println(algoritmo == 1
                ? "=== Ronda (Round Robin) - Quantum = " + quantum + " - " + procesos.size() + " procesos ==="
                : "=== Tiempo restante mas corto (SRTF) - " + procesos.size() + " procesos ===");
        System.out.println();
        imprimirGantt(linea, procesos);
        imprimirTabla(procesos);
    }

    public static void main(String[] args) {
        System.out.println("=== Planificador de procesos (apropiativo) ===");
        while (true) {
            System.out.println();
            System.out.println("------------- MENU -------------");
            System.out.println("1) Ronda (Round Robin)");
            System.out.println("2) Tiempo restante mas corto (SRTF)");
            System.out.println("3) Ingresar nuevos procesos");
            System.out.println("4) Salir");
            int op = leerEntero("Seleccione una opcion", 1, 4);
            if (op == 4) break;
            if (op == 3) {
                pedirDatos();
                continue;
            }
            if (llegadas == null) pedirDatos();
            ejecutar(op);
        }
        System.out.println("Programa finalizado.");
    }
}
