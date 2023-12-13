import java.util.*;

public class CLCS_inst<T> {

    public int m;                   // Number of input strings.
    public int n;                   // Length of the longest input string.
    public int p;                   // Length of the constrained string.
    public int sigma;               // Size of the alphabet.
    public int UB;
    public List<T> listChar;                  // An (trivial) upper bound for the solution length.
    public List<List<Integer>> S;  // Vector of input sequences encoded by integers from [0,sigma).
    public List<List<List<Integer>>> occurance_positions; // Structure for reading the number of occurrences of letter <a> of some string i<=m.
    public List<List<List<Integer>>> successors;  // Succ structure (preprocessing).
    public List<List<List<Integer>>> M_lcs;  // M stores score matrices (relaxed version only M_{i, i+1}) (LCS case).
    public List<List<List<List<Integer>>>> M;  // M stores score matrices (relaxed version only M_{i, i+1}) (CLCS case).
    public List<T> int2Char;  // Translation table for internal letters corresponding to integers in [0,sigma) to real alphabet.
    public Map<T, Integer> char2Int;  // Translation table for real alphabet into integers in [0,sigma).
    public List<Integer> P;  // Constraint string.
    public List<List<Integer>> embed_end;  // Embed structure (preprocessing).
    public List<List<Double>> P_m;

    public CLCS_inst(List<List<T>> sequences, List<T> constrainedString, List<T> alphabet) {
        this.m = sequences.size();
        this.n = sequences.stream().map(List::size).max(Integer::compare).orElse(0);
        this.p = constrainedString.size();
        this.sigma = alphabet.size();
        this.UB = 0;
        this.listChar = alphabet;
        this.char2Int = new HashMap<>();
        for (int i = 0; i < alphabet.size(); i++) {
            char2Int.put(alphabet.get(i), i);
        }
        this.int2Char = new ArrayList<T>(alphabet);
        this.successors = new ArrayList<>();
        this.occurance_positions = new ArrayList<>();
        this.S = new ArrayList<>();
        for (List<T> seq : sequences) {
            List<Integer> seqList = new ArrayList<>();
            for (T c : seq) {
                char2Int.get(c);
                seqList.add(char2Int.get(c));
            }
            S.add(seqList);
        }
        this.P = new ArrayList<>();
        if(constrainedString.size() > 0){
            for (T c : constrainedString) {
            P.add(char2Int.get(c));
        }
        }
        this.M_lcs = new ArrayList<>();
        this.M = new ArrayList<>();
        structure_occurances();
        structure_embeddings();
        P_matrix();
        store_M();
    }

    public void structure_occurances() {
        occurance_positions = new ArrayList<>();
        successors = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            List<List<Integer>> occurances_letters = new ArrayList<>();
            List<List<Integer>> successors_letters = new ArrayList<>();

            for (int a = 0; a < sigma; a++) {
                occurances_letters.add(occurances_string_letter(S.get(i), a));
                successors_letters.add(successor_string_letter(S.get(i), a));
            }

            occurance_positions.add(occurances_letters);
            successors.add(successors_letters);
        }
    }

    public void P_matrix() {
        P_m = new ArrayList<>(Collections.nCopies(n + 1, new ArrayList<>(Collections.nCopies(n + 1, 0.0))));

        for (int q = 0; q <= n; ++q) {
            for (int k = 0; k <= n; ++k) {
                if (k == 0) {
                    P_m.get(k).set(q, 1.0);
                } else if (k > q) {
                    P_m.get(k).set(q, 0.0);
                } else {
                    P_m.get(k).set(q, (1.0 / sigma) * P_m.get(k - 1).get(q - 1) + ((double) sigma - 1.0) / (sigma) * P_m.get(k).get(q - 1));
                }
            }
        }
    }

    public List<Integer> occurances_string_letter(List<Integer> str, int a) {
        List<Integer> occurances_letter = new ArrayList<>(Collections.nCopies(n,0));
        int size = str.size();
        int number = 0;
        for (int i = size-1; i >= 0; i--) {
            if (str.get(i) == a)
                number++;
            occurances_letter.set(i,number);
        }
        return occurances_letter;
    }

    public List<Integer> successor_string_letter(List<Integer> str, int a) {
        List<Integer> successor_letter = new ArrayList<>(Collections.nCopies(n, 0));
        int size = str.size();
        int number = size + 1;
        for (int i = size - 1; i >= 0; --i) {
            if (str.get(i) == a) {
                number = i + 1;
                successor_letter.set(i, number);
            } else {
                successor_letter.set(i, number);
            }
        }
        if (str.get(0) == a) {
            successor_letter.set(0, 1);
        }
        return successor_letter;
    }

    public List<List<Integer>> occurances_all_letters(List<Integer> str) {
        List<List<Integer>> occurances_all = new ArrayList<>();
        for (int a = 0; a < sigma; a++) {
            occurances_all.add(occurances_string_letter(str, a));
        }
        return occurances_all;
    }

    public List<List<Integer>> successors_all_letters(List<Integer> str) {
        List<List<Integer>> successors_all = new ArrayList<>();
        for (int a = 0; a < sigma; a++) {
            successors_all.add(successor_string_letter(str, a));
        }
        return successors_all;
    }

    public void structure_embeddings() {
        embed_end = new ArrayList<>();
        for (int i = 0; i < m; i++) {
            int p_u = p - 1;
            List<Integer> s_i = S.get(i);
            List<Integer> embed_end_i = new ArrayList<>();
            for (int j = s_i.size() - 1; j >= 0 && p_u >= 0; j--) {
                if (s_i.get(j).equals(P.get(p_u))) {
                    embed_end_i.add(j);
                    p_u--;
                }
            }
            Collections.reverse(embed_end_i);
            embed_end_i.add(s_i.size());
            embed_end.add(embed_end_i);
        }
    }

    public List<List<Integer>> lcs_m_ij(int i, int j) {
        List<List<Integer>> m_ij = new ArrayList<>();
        for (int x = 0; x <= S.get(i).size(); x++) {
            List<Integer> row = new ArrayList<>();
            for (int y = 0; y <= S.get(j).size(); y++) {
                row.add(0);
            }
            m_ij.add(row);
        }

        for (int x = S.get(i).size(); x >= 0; x--) {
            for (int y = S.get(j).size(); y >= 0; y--) {
                if (x == S.get(i).size()) {
                    m_ij.get(x).set(y, 0);
                } else if (y == S.get(j).size()) {
                    m_ij.get(x).set(y, 0);
                } else if (S.get(i).get(x).equals(S.get(j).get(y))) {
                    m_ij.get(x).set(y, m_ij.get(x + 1).get(y + 1) + 1);
                } else {
                    m_ij.get(x).set(y, Math.max(m_ij.get(x).get(y + 1), m_ij.get(x + 1).get(y)));
                }
            }
        }
        return m_ij;
    }

    public List<List<List<Integer>>> clcs_m_ij(int i, int j) {
        List<List<List<Integer>>> m_ij = new ArrayList<>(Collections.nCopies(S.get(i).size() + 1,
                new ArrayList<>(Collections.nCopies(S.get(j).size() + 1, new ArrayList<>(Collections.nCopies(p + 1, 0))))));

        for (int x = S.get(i).size() - 1; x >= 0; x--) {
            for (int y = S.get(j).size() - 1; y >= 0; y--) {
                for (int z = p; z >= 0; z--) {
                    if (S.get(i).get(x).equals(S.get(j).get(y))) {
                        if (z != p && S.get(i).get(x).equals(P.get(z))) {
                            m_ij.get(x).get(y).set(z, 1 + m_ij.get(x + 1).get(y + 1).get(z + 1));
                        } else {
                            m_ij.get(x).get(y).set(z, 1 + m_ij.get(x + 1).get(y + 1).get(z));
                        }
                    } else {
                        m_ij.get(x).get(y).set(z, Math.max(m_ij.get(x + 1).get(y).get(z), m_ij.get(x).get(y + 1).get(z)));
                    }
                }
            }
        }
        return m_ij;
    }

    public void store_M() {
        if (UB != 0) {
            System.out.println("Using CLCS scoring matrices...");
        }

        for (int i = 0; i < m - 1; i++) {
            if (UB == 0) {
                M_lcs.add(lcs_m_ij(i, i + 1));
            } else {
                M.add(clcs_m_ij(i, i + 1));
            }
        }
    }

    public void write(int detailed) {
        System.out.println("m = " + m);
        System.out.println("n = " + n);
        System.out.println("p = " + p);
        System.out.println("sigma = " + sigma);
        System.out.println("UB = " + UB);

        if (detailed == 1) {
            System.out.println("S:");
            for (int i = 0; i < m; i++) {
                System.out.println("s" + i + ": " + S.get(i));
            }

            System.out.println("occurance_positions:");
            for (int i = 0; i < m; i++) {
                for (int a = 0; a < sigma; a++) {
                    System.out.println("s" + i + " occurances of " + a + ": " + occurance_positions.get(i).get(a));
                }
            }

            System.out.println("successors:");
            for (int i = 0; i < m; i++) {
                for (int a = 0; a < sigma; a++) {
                    System.out.println("s" + i + " successors of " + a + ": " + successors.get(i).get(a));
                }
            }

            if (UB != 0) {
                System.out.println("M_lcs:");
                for (int i = 0; i < m - 1; i++) {
                    System.out.println("M_lcs_" + i + ", " + (i + 1) + ":");
                    for (List<Integer> row : M_lcs.get(i)) {
                        System.out.println(row);
                    }
                }

                System.out.println("M (CLCS):");
                for (int i = 0; i < m - 1; i++) {
                    System.out.println("M_CLCS_" + i + ", " + (i + 1) + ":");
                    for (List<List<Integer>> row : M.get(i)) {
                        for (List<Integer> scoreMatrix : row) {
                            System.out.println(scoreMatrix);
                        }
                    }
                }
            }
        }

        System.out.println("P:");
        System.out.println(P);
        System.out.println("embed_end:");
        for (int i = 0; i < m; i++) {
            System.out.println("embed_" + i + ": " + embed_end.get(i));
        }

        System.out.println("P_m:");
        for (List<Double> row : P_m) {
            System.out.println(row);
        }
    }

    public static void main(String[] args){
        List<List<Student>> studentSequences = new ArrayList<>();
        studentSequences.add(Arrays.asList(
                new Student("Alice", 20, "Computer Science"),
                new Student("Bob", 22, "Mathematics"),
                new Student("Charlie", 21, "Physics")
        ));

        studentSequences.add(Arrays.asList(
                new Student("David", 23, "Chemistry"),
                new Student("Alice", 20, "Computer Science"),
                new Student("Bob", 22, "Mathematics")
        ));

        List<Student> alphabet = Arrays.asList(
                new Student("Alice", 20, "Computer Science"),
                new Student("Bob", 22, "Mathematics"),
                new Student("Charlie", 21, "Physics"),
                new Student("David", 23, "Chemistry")
        );  
        List<Student> constrainedStudents = new ArrayList<>();
        CLCS_inst<Student> aStar = new CLCS_inst<Student>(studentSequences, constrainedStudents, alphabet);
        aStar.write(1);
    }
}
