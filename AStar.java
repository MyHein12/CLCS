import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class AStar<T> {
    private CLCS_solution<T> sol;
    private PriorityQueue<QNode> Q;
    private Map<List<Integer>, List<Map.Entry<Integer, Integer>>> N;
    private int stat_nodes_created;
    private int stat_nodes_expanded;
    private int stat_nodes_ignored;
    private int stat_nodes_merged;
    private int stat_max_nested;
    private List<T> alphabet;
    private List<Double> weight;
    private List<List<T>> cachedResult; //lưu trữ kết quả

    public AStar(List<List<T>> sequences, List<T> constrainedString, List<T> alphabet, List<Double> weight) {
        this.sol = new CLCS_solution<T>(sequences,constrainedString,alphabet);
        this.Q = new PriorityQueue<>(Collections.reverseOrder());
        this.N = new HashMap<>();
        this.stat_nodes_created = 0;
        this.stat_nodes_expanded = 0;
        this.stat_nodes_ignored = 0;
        this.stat_nodes_merged = 0;
        this.stat_max_nested = 0;
        this.alphabet = alphabet;
        this.weight = weight;
    }

    public List<Integer> startSearch() {
        Node root = new Node();
        root.pL = new ArrayList<>(Collections.nCopies(sol.inst.m, 1));
        root.parent = null;
        addNode(root);
        try {
            while (!Q.isEmpty()) {
                Node v = Q.poll().node;
                
                Map<Integer, List<Integer>> sigma_nd = sol.findSigmaNd(v);
                if (sigma_nd.isEmpty()) {
                    return sol.deriveSolution(v);
                }

                stat_nodes_expanded++;

                for (Map.Entry<Integer, List<Integer>> entry : sigma_nd.entrySet()) {
                    int letter = entry.getKey();
                    List<Integer> pL = entry.getValue();
                    

                    Node v_ext = sol.expandNode(v, letter, pL);
                    List<Map.Entry<Integer, Integer>> N_rel = N.get(pL);
                    
                    if (N_rel == null) {
                        addNode(v_ext);
                    } else {
                        boolean insert = true;
                        ListIterator<Map.Entry<Integer, Integer>> it_rel = N_rel.listIterator();

                        while (it_rel.hasNext()) {
                            Map.Entry<Integer, Integer> rel = it_rel.next();
                            int u_v_rel = rel.getKey();
                            int l_v_rel = rel.getValue();

                            if (insert && v_ext.u_v > u_v_rel) {
                                insertNode(v_ext, N_rel, it_rel);
                                insert = false;
                            }

                            if (l_v_rel >= v_ext.l_v && u_v_rel >= v_ext.u_v) {
                                stat_nodes_ignored++;
                                insert = false;
                                break;
                            }

                            if (v_ext.l_v >= l_v_rel && v_ext.u_v >= u_v_rel) {
                                it_rel.remove();
                                stat_nodes_merged++;
                            } else {
                                it_rel.next();
                            }
                        }

                        if (insert) {
                            addNode(v_ext, N_rel, null);
                        }
                    }
                }
            }
        } catch (OutOfMemoryError ex) {
            System.out.println("Out of memory!");
            System.out.println("UB: " + Q.peek().ub);
        }

        return new ArrayList<>();
    }

    public void printStatistics() {
        System.out.println("Node Stats (created, expanded, ignored, merged): " +
                stat_nodes_created + ", " +
                stat_nodes_expanded + ", " +
                stat_nodes_ignored + ", " +
                stat_nodes_merged);

        System.out.println("Sizes (N, Q): " + N.size() + ", " + Q.size());
        System.out.println("Max Nested (N_rel): " + stat_max_nested);
    }

    private void addNode(Node v) {
        Q.add(new QNode(v, ub(v, sol.inst)));
        stat_nodes_created++;

        if (!N.containsKey(v.pL)) {
            N.put(v.pL, new ArrayList<>(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>(v.u_v, v.l_v))));
        }
    }

    private void addNode(Node v, List<Map.Entry<Integer, Integer>> list, ListIterator<Map.Entry<Integer, Integer>> pos) {
        Q.add(new QNode(v, ub(v, sol.inst)));
        stat_nodes_created++;

        if (list == null) {
            N.put(v.pL, new ArrayList<>(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>(v.u_v, v.l_v))));
        } else {
            list.add(pos.nextIndex(), new AbstractMap.SimpleImmutableEntry<>(v.u_v, v.l_v));
            if (stat_max_nested < list.size()) {
                stat_max_nested++;
            }
        }
    }

    private void insertNode(Node v, List<Map.Entry<Integer, Integer>> list, ListIterator<Map.Entry<Integer, Integer>> pos) {
        Q.add(new QNode(v, ub(v, sol.inst)));
        stat_nodes_created++;

        if (list == null) {
            N.put(v.pL, new ArrayList<>(Collections.singletonList(new AbstractMap.SimpleImmutableEntry<>(v.u_v, v.l_v))));
        } else {
            list.add(pos.nextIndex(), new AbstractMap.SimpleImmutableEntry<>(v.u_v, v.l_v));
            if (stat_max_nested < list.size()) {
                stat_max_nested++;
            }
        }
    }

    public int ub1(Node v, CLCS_inst<T> inst) {
        int ub1 = 0;
        int[] minOccurrencesNode = new int[inst.sigma];
        Arrays.fill(minOccurrencesNode, (sol.inst.S.get(0).size()));

        for (int a = 0; a < inst.sigma; a++) {
            for (int i = 0; i < inst.m; i++) {
                if (v.pL.get(i) - 1 < sol.inst.S.get(i).size()) {
                    int occurrencePosition = sol.inst.occurance_positions.get(i).get(a).get(v.pL.get(i)-1);
                    if (occurrencePosition < minOccurrencesNode[a]) {
                        minOccurrencesNode[a] = occurrencePosition;
                    }
                } else {
                    minOccurrencesNode[a] = 0;
                }
            }
        }

        for (int c_a : minOccurrencesNode) {
            ub1 += c_a;
        }

        return v.l_v + ub1;
    }
    

    public int ub2(Node v, CLCS_inst<T> inst) {
        int ub2 = 0;
    
        if (!inst.S.isEmpty() && !sol.inst.S.get(0).isEmpty()) {
            ub2 = sol.inst.S.get(0).size();
    
            for (int i = 0; i < inst.m - 1; i++) {
                if (inst.UB == 1) {
                    if (ub2 > sol.inst.M.get(i).get(v.pL.get(i) - 1).get(v.pL.get(i + 1) - 1).get(v.u_v)) {
                        ub2 = sol.inst.M.get(i).get(v.pL.get(i) - 1).get(v.pL.get(i + 1) - 1).get(v.u_v);
                    }
                } else {
                    if (ub2 > sol.inst.M_lcs.get(i).get(v.pL.get(i) - 1).get(v.pL.get(i + 1) - 1)) {
                        ub2 = sol.inst.M_lcs.get(i).get(v.pL.get(i) - 1).get(v.pL.get(i + 1) - 1);
                    }
                }
            }
        }
    
        return v.l_v + ub2;
    }


    public int ub(Node v, CLCS_inst<T> inst) {
        int ub1 = ub1(v, inst);
        int ub2 = ub2(v, inst);
        return Math.min(ub1, ub2);
    }

    public List<List<T>> result_astar() {
        List<List<T>> result = new ArrayList<>();

        List<Integer> longestCommonSequence = startSearch();
    
        // Add the longest common sequence to the result
        List<T> longestCommonSequenceValues = new ArrayList<>();
        for (int index : longestCommonSequence) {
            longestCommonSequenceValues.add(alphabet.get(index));
        }
        result.add(longestCommonSequenceValues);
    
        // Generate other subsequences
        for (int length = 1; length < longestCommonSequence.size(); length++) {
            for (int i = 0; i <= longestCommonSequence.size() - length; i++) {
                int end = i + length;
                List<Integer> subsequence = longestCommonSequence.subList(i, end);
                List<T> subsequenceValues = new ArrayList<>();
                for (int index : subsequence) {
                    subsequenceValues.add(alphabet.get(index));
                }
    
                // Add to the result PriorityQueue
                result.add(subsequenceValues);
            }
        }
        cachedResult = result;
        return result;
    }

    public Double LengthofKLongest(List<T> kLCS){
        Double length = 0.0;
        for(T item : kLCS){
            for(int i = 0;i < alphabet.size();i++){
                if(item.equals(alphabet.get(i))){
                    length += weight.get(i);
                }
            }
        }
        return length;
    }

    // public List<List<T>> topKLongest(int k){
    //     List<List<T>> topK = new ArrayList<>();
    //     PriorityQueue<QLNode<T>> queue = new PriorityQueue<>(Collections.reverseOrder());

    //     List<List<T>> notTopK = (cachedResult != null) ? new ArrayList<>(cachedResult) : result_astar();
    //     for(List<T> item : notTopK){
    //         queue.add(new QLNode<T>(item, LengthofKLongest(item)));
    //     }
    //     for(int i = 0;i< k ;i++){
    //         List<T> klongest = queue.poll().node;
    //         topK.add(klongest);
    //     }
    //     return topK;
    // }

    public List<List<T>> topKLongest(int k) {
        List<List<T>> topK = new ArrayList<>();
        PriorityQueue<QLNode<T>> queue = new PriorityQueue<>(Collections.reverseOrder());
    
        List<List<T>> notTopK = (cachedResult != null) ? new ArrayList<>(cachedResult) : result_astar();
        for (List<T> item : notTopK) {
            queue.add(new QLNode<T>(item, LengthofKLongest(item)));
        }
    
        int resultSize = queue.size();
        if (k > resultSize) {
            System.out.println("Warning: k is greater than the length of the result!\nMaximun is top"+resultSize);
            k = resultSize;

        }
    
        for (int i = 0; i < k; i++) {
            List<T> klongest = queue.poll().node;
            topK.add(klongest);
        }
        return topK;
    }


    public static void main(String[] args) {
        try (BufferedReader br = new BufferedReader(new FileReader("student_data.json"))) {
            List<List<Student>> studentSequences = new ArrayList<>();

            StringBuilder jsonData = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                jsonData.append(line);
            }

            String jsonString = jsonData.toString();
            jsonString = jsonString.replaceAll("\\s+", "");

            // Tìm thông tin studentSequences1
            Pattern pattern1 = Pattern.compile("\"studentSequences1\":\\[(.*?)\\]");
            Matcher matcher1 = pattern1.matcher(jsonString);
            if (matcher1.find()) {
                String studentSequences1Data = matcher1.group(1);
                String[] students1 = studentSequences1Data.split("\\},\\{");

                List<Student> studentsList1 = new ArrayList<>();
                for (String student : students1) {
                    student = student.replaceAll("[\\[\\]\\{\\}]", "");
                    String[] studentInfo = student.split(",");
                    studentsList1.add(new Student(
                            studentInfo[0].split(":")[1].replaceAll("\"", ""),
                            Integer.parseInt(studentInfo[1].split(":")[1]),
                            studentInfo[2].split(":")[1].replaceAll("\"", "")
                    ));
                }
                studentSequences.add(studentsList1);
            }

            // Tìm thông tin studentSequences2
            Pattern pattern2 = Pattern.compile("\"studentSequences2\":\\[(.*?)\\]");
            Matcher matcher2 = pattern2.matcher(jsonString);
            if (matcher2.find()) {
                String studentSequences2Data = matcher2.group(1);
                String[] students2 = studentSequences2Data.split("\\},\\{");

                List<Student> studentsList2 = new ArrayList<>();
                for (String student : students2) {
                    student = student.replaceAll("[\\[\\]\\{\\}]", "");
                    String[] studentInfo = student.split(",");
                    studentsList2.add(new Student(
                            studentInfo[0].split(":")[1].replaceAll("\"", ""),
                            Integer.parseInt(studentInfo[1].split(":")[1]),
                            studentInfo[2].split(":")[1].replaceAll("\"", "")
                    ));
                }
                studentSequences.add(studentsList2);
            }

            List<Student> alphabet = new ArrayList<>();
            Set<String> studentNames = new HashSet<>();
            for (List<Student> students : studentSequences) {
                for (Student student : students) {
                    if (!studentNames.contains(student.getName())) {
                        alphabet.add(student);
                        studentNames.add(student.getName());
                    }
                }
            }
            List<Student> constrainedStudents = new ArrayList<>();
            List<Double> weight = new ArrayList<>();
            for(int i = 0 ; i < alphabet.size();i++){
                Double a = getRandomDoubleInRange(0.0, 1.0);
                weight.add(a);
            }
            AStar<Student> aStar = new AStar<Student>(studentSequences, constrainedStudents, alphabet,weight);
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter k: ");
            int k = scanner.nextInt();
            System.out.println("k=" + k + ": ");
            List<List<Student>> topK = aStar.topKLongest(k);
            for (List<Student> sequence : topK) {
                System.out.println(sequence);
            }
            scanner.close();

        } catch (Exception e) {
            System.out.println("You must create dataset by running code in file createInput.py");
        }
    }


    

    private static Double getRandomDoubleInRange(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }

        Random r = new Random();
        return min + (max - min) * r.nextDouble();
    }
}

