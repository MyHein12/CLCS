
public class QNode implements Comparable<QNode> {
    Node node;
    int ub;

    public QNode(Node node, int ub) {
        this.node = node;
        this.ub = ub;
    }

    @Override
    public int compareTo(QNode other) {
        if (this.ub == other.ub) {
            if (this.node.l_v == other.node.l_v) {
                return Integer.compare(this.node.u_v, other.node.u_v);
            } else {
                return Integer.compare(this.node.l_v, other.node.l_v);
            }
        } else {
            return Integer.compare(this.ub, other.ub);
        }
    }
}
