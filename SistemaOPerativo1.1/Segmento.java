/** Un tramo del diagrama de Gantt. nombre "-" significa CPU libre. */
public class Segmento {
    public final int ini;
    public int fin;
    public final String nombre;

    public Segmento(int ini, int fin, String nombre) {
        this.ini = ini;
        this.fin = fin;
        this.nombre = nombre;
    }
}
