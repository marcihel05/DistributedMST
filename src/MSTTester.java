import java.io.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

public class MSTTester {
    public static void main(String[] args) throws Exception {
        Linker comm = null;
        try {
            String baseName = args[0];
            int myId = Integer.parseInt(args[1]);
            int numProc = Integer.parseInt(args[2]);
            comm = new Linker(baseName, myId, numProc);
            Random r = new Random();
            ArrayList<Edge> edges = new ArrayList<Edge>();
            Util.println("Reading weights");
            IntLinkedList weights = new IntLinkedList();
            try {
                BufferedReader dIn = new BufferedReader(
                        new FileReader(System.getProperty("user.dir") + File.separator + "weights" + myId));
                StringTokenizer st = new StringTokenizer(dIn.readLine());
                while (st.hasMoreTokens()) {
                    int weight = Integer.parseInt(st.nextToken());
                    weights.add(weight);
                }
            } catch (FileNotFoundException e) {
                System.out.println("math ain't mathing");
                for (int j = 0; j < numProc; j++)
                    if (j != myId) weights.add(j);
            } catch (IOException e) {
                System.err.println(e);
            }
            for(int i = 0; i < numProc; ++i){
                Edge e;
                if(i != myId){
                    if (weights.getEntry(i) != -100) {
                        e = new Edge(myId, i, weights.getEntry(i), EdgeState.BASIC);
                        edges.add(e);
                    }
                }
            }
            if (args[3].equals("Sync")){
                AlphaSynch pulser = new AlphaSynch(comm);
                MSTSync node = new MSTSync(comm, edges, pulser);
                for (int i = 0; i < numProc; i++)
                    if (i != myId)
                        (new ListenerThread(i, pulser)).start();
                node.initiate();
            }
            else if (args[3].equals("Async")){
                GHSNodeAsync node = new GHSNodeAsync(comm, edges);
                for (int i = 0; i < numProc; i++)
                    if (i != myId)
                        (new ListenerThread(i, (MsgHandler)node)).start();
                node.initiate();
            }

            else if (args[3].equals("Async2")){
                Node node = new Node(comm, edges);
                for (int i = 0; i < numProc; i++)
                    if (i != myId)
                        (new ListenerThread(i, (MsgHandler)node)).start();
                node.initiate();
            }
        }
        catch (InterruptedException e) {
            if (comm != null) comm.close();
        }
        catch (Exception e) {
            System.out.println(e);
            e.printStackTrace();
        }
    }
}
