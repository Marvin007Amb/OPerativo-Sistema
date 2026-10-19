public class Proceso {
    public final String nombre;
    public final int llegada;
    public final int cpu;
    public int restante;
    public int inicio = -1;
    public int fin = -1;

    public Proceso(String nombre, int llegada, int cpu) {
        this.nombre = nombre;
        this.llegada = llegada;
        this.cpu = cpu;
        this.restante = cpu;
    }

    /** Nombre del proceso: A..Z y luego P26, P27... */
    public static String nombreProceso(int i) {
        return i < 26 ? String.valueOf((char) ('A' + i)) : "P" + i;
    }

    /** Retorno = Final - Llegada */
    public int retorno() {
        return fin - llegada;
    }

    /** Espera = Retorno - CPU */
    public int espera() {
        return retorno() - cpu;
    }

    /** Tiempo promedio = Retorno / CPU */
    public double promedio() {
        return (double) retorno() / cpu;
    }

    /** Respuesta = Primer inicio - Llegada */
    public int respuesta() {
        return inicio - llegada;
    }
}
