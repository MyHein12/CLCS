import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class Node {

    public List<Integer> pL;
    public Node parent;
    public int u_v;
    public int l_v;

    public Node() {
        this.pL = new ArrayList<>(Arrays.asList(0, 0));
        this.parent = null;
        this.u_v = 0;
        this.l_v = 0;
    }
}