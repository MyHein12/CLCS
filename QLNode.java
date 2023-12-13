import java.util.List;

public class QLNode<T> implements Comparable<QLNode<T>> {
    List<T> node;
    double ub;

    public QLNode(List<T> node, double ub) {
        this.node = node;
        this.ub = ub;
    }

    @Override
    public int compareTo(QLNode<T> other) {
        if (this.ub == other.ub) {
            if (this.ub ==  other.ub) {
                return Double.compare(ub, other.ub);
            } else {
                return Double.compare(this.ub, other.ub);
            }
        } else {
            return Double.compare(this.ub, other.ub);
        }
    }
}
