public class cluster {
  private   int id;
  private speedData[] speed ;
  private edegData [] edges;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public speedData[] getSpeed() {
        return speed;
    }

    public void setSoeed(speedData[] speed) {
        this.speed = speed;
    }

    public edegData[] getEdges() {
        return edges;
    }

    public void setEdges(edegData[] edges) {
        this.edges = edges;
    }
}
