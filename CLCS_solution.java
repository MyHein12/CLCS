import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CLCS_solution<T> {

    public CLCS_inst<T> inst;
    public List<List<Integer>> S;  // Vector of input sequences encoded by integers from [0,sigma).
    public List<List<List<Integer>>> occurance_positions; // Structure for reading the number of occurrences of letter <a> of some string i<=m.
    public List<List<List<Integer>>> successors;  // Succ structure (preprocessing).
    public List<List<List<Integer>>> M_lcs;  // M stores score matrices (relaxed version only M_{i, i+1}) (LCS case).
    public List<List<List<List<Integer>>>> M;  // M stores score matrices (relaxed version only M_{i, i+1}) (CLCS case).
    public List<Integer> P;  // Constraint string.
    public List<List<Integer>> embed_end;  // Embed structure (preprocessing).

    public CLCS_solution(List<List<T>> sequences, List<T> constrainedString, List<T> alphabet) {
        this.inst = new CLCS_inst<T>(sequences, constrainedString, alphabet);
        this.S = inst.S;
        this.successors = inst.successors;
        this.occurance_positions = inst.occurance_positions;
        this.M_lcs = inst.M_lcs;
        this.M = inst.M;
        this.P = inst.P;
        this.embed_end = inst.embed_end;
        
    }

    public boolean checkDomination(List<Integer> pL1, List<Integer> pL2) {
        int matchPos = 0;
        for (int j = 0; j < pL1.size(); j++) {
            if (pL1.get(j).equals(pL2.get(j))) {
                matchPos++;
            } else if (pL1.get(j) < pL2.get(j)) {
                return false;
            }
        }
        return matchPos != pL2.size();
    }

    public Map<Integer, List<Integer>> findSigmaNd(Node v) {
        Map<Integer, List<Integer>> sigmaNd = new HashMap<>();
        Map<Integer, List<Integer>> sigmaFeasible = findFeasibleSigma(v);

        for (Map.Entry<Integer, List<Integer>> entry1 : sigmaFeasible.entrySet()) {
            int letter1 = entry1.getKey();
            List<Integer> pL1 = entry1.getValue();
            boolean letter1IsDominated = false;

            for (Map.Entry<Integer, List<Integer>> entry2 : sigmaFeasible.entrySet()) {
                int letter2 = entry2.getKey();
                List<Integer> pL2 = entry2.getValue();

                if (letter1 != letter2) {
                    letter1IsDominated = checkDomination(pL1, pL2);
                }

                if (letter1IsDominated) {
                    break;
                }
            }

            if (!letter1IsDominated) {
                sigmaNd.put(letter1, pL1);
            }
        }

        return sigmaNd;
    }

    public Map<Integer, List<Integer>> findFeasibleSigma(Node v) {
        Map<Integer, List<Integer>> feasibleSigma = new HashMap<>();
    
        for (int a = 0; a < inst.sigma; a++) {
            boolean isFeasibleLetter = true;
            List<Integer> pLa = new ArrayList<>();
    
            int pu = 0;
            if (v.u_v < inst.P.size()) {
                pu = (a == inst.P.get(v.u_v)) ? v.u_v + 1 : v.u_v;
            } else {
                pu = v.u_v;
            }
    
            for (int i = 0; i < inst.m; i++) {
                if (v.pL.get(i) - 1 < S.get(i).size()) {
                    if (successors.get(i).get(a).get(v.pL.get(i) - 1) > embed_end.get(i).get(pu)
                            || v.pL.get(i) > S.get(i).size()) {
                        isFeasibleLetter = false;
                        break;
                    }
                    pLa.add(successors.get(i).get(a).get(v.pL.get(i) - 1) + 1);
                } else {
                    isFeasibleLetter = false;
                    break;
                }
            }
    
            if (isFeasibleLetter) {
                feasibleSigma.put(a, pLa);
            }
        }
    
        return feasibleSigma;
    }

    public Node expandNode(Node parent, int letter, List<Integer> pL) {
        Node child = new Node();
        child.parent = parent;
        child.l_v = parent.l_v + 1;

        if (parent.u_v < inst.P.size()) {
            child.u_v = (P.get(parent.u_v) == letter) ? parent.u_v + 1 : parent.u_v;
        } else {
            child.u_v = parent.u_v;
        }

        child.pL = new ArrayList<>(pL);
        return child;
    }

    public List<Integer> deriveSolution(Node v) {
        List<Integer> s = new ArrayList<>();
        while (v.parent != null) {
            s.add(S.get(0).get(v.pL.get(0) - 2));
            v = v.parent;
        }
        Collections.reverse(s);
        return s;
    }

        public static void main(String[] args) {
        List<List<String>> ab = new ArrayList<>();
        ab.add(Arrays.asList("b", "c", "a", "a", "c", "b", "d", "b", "a"));
        ab.add(Arrays.asList("c", "b", "c", "c", "a", "d", "c", "b", "b", "d"));
        List<String> alphabet = Arrays.asList("a", "b", "c", "d");
        List<String> constrainedString = Arrays.asList("c", "b", "b");
        CLCS_solution<String> a = new CLCS_solution<>(ab, constrainedString, alphabet);

        System.out.println(a.S);
    }


}
