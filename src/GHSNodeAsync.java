import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashSet;

public class GHSNodeAsync extends Process{

    int uid;
    int port;
    ArrayList<Edge> edges;
    double timeout;
    NodeState state;
    int level;
    boolean active;
    int core; // brid - valjda predstavlja težinu core brida?
    //double best_weight;
    int best_weight;
    ConnectRequests connect_requests;
    Edge test_edge;
    int find_count;
    int find_source;
    int sent_connect_to;
    boolean expect_core_report;
    boolean report_over;
    int other_core_node;
    boolean discovery;
    boolean test_over;
    int leader;
    int total_number_of_nodes;
    //String[] best_path;
    ArrayList<String> best_path;
    HashSet<ArrayList<Integer>> test_requests;

    public GHSNodeAsync(Linker initComm, ArrayList<Edge> edges){
        super(initComm);
        this.edges = edges;
        state = NodeState.SLEEP;
        level = 0;
        active = true;
        this.core = -1;
        best_weight = Integer.MAX_VALUE;
        test_edge = null;
        connect_requests = new ConnectRequests();
        find_count = 0;
        find_source = -1;
        sent_connect_to = -1;
        expect_core_report = false;
        report_over = false;
        other_core_node = -1;
        discovery = false;
        test_over = false;
        leader = -1;
        total_number_of_nodes = N;
        best_path = new ArrayList<>();
        test_requests = new HashSet<ArrayList<Integer>>();
    }

    public void initiate(){
        System.out.println("Started");
        wakeup();
        while (true)
            main();
        //do_end();
    }

    public synchronized void handleMsg(Msg m, int src, String tag){
        if(tag.equals("End")){
            do_end();
        }
        else if(tag.equals("Connect")){
            System.out.println("Connect received from " + m.getSrcId());
            int c = m.getMessageInt();
            int l = m.getMessageInt2();
            do_connect(m.getSrcId(), c, l);
        }
        else if(tag.equals("Test")){
            System.out.println("Test received from " + m.getSrcId());
            int c = m.getMessageInt();
            int l = m.getMessageInt2();
            do_test(m.getSrcId(), c, l);
        }
        else if(tag.equals("Initiate")){
            System.out.println("Initiate received from " + m.getSrcId());
            int c = m.getMessageInt();
            int l = m.getMessageInt2();
            NodeState s = m.getMessageState();
            boolean merge = m.getMessageBool();
            do_initiate(m.getSrcId(), c, l, s, merge);
        }
        else if(tag.equals("Reject")){
            System.out.println("Reject received from " + m.getSrcId());
            do_reject(m.getSrcId());
        }
        else if(tag.equals("Report")){
            System.out.println("Report received from " + m.getSrcId());
            int w = m.getMessageInt();
            String[] p = m.getMessagePath();
            do_report(m.getSrcId(), w, p);
        }
        else if(tag.equals("ChangeRoot")){
            System.out.println("ChangeRoot received from " + m.getSrcId());
            String[] p = m.getMessagePath();
            do_change_root(m.getSrcId(), p);
        }

        else if(tag.equals("Accept")){
            System.out.println("Accept received from " + m.getSrcId());
            do_accept(m.getSrcId());
        }
    }

    public void wakeup()
    {
        System.out.println("Node: " + this.uid + " entered Wakeup");
        if (this.state == NodeState.SLEEP)
        {
            this.state = NodeState.FOUND;

            Edge min_weight_outgoing_edge = edges.get(0);
            int min_weight = edges.get(0).getWeight();

            for(var e:edges)
            {
                if (e.weight < min_weight)
                {
                    min_weight = e.weight;
                    min_weight_outgoing_edge = e;
                }
            }

            sendMsg(min_weight_outgoing_edge.getDest(), "Connect", core, level);

            min_weight_outgoing_edge.state = EdgeState.BRANCH;
            sent_connect_to = min_weight_outgoing_edge.getDest();
        }
    }

    public void do_connect(int sender, int core, int level)
    {
        wakeup();
        if(sent_connect_to == sender) {
            merge(sender);
            sent_connect_to = -1;
        }

        else if (level < this.level) {
        //Console.WriteLine("Node: " + this.uid + " going to absorb");
        int node = this.connect_requests.get_least_level_req();
        this.absorbe_node(node);
        this.connect_requests.requests.remove(node);
        }
        //connect_requests.insert(sender, level);
    }

    public void do_test(int sender, int core, int level)
    {
        wakeup();
        ArrayList<Integer> t = new ArrayList<>();
        t.add(level); t.add(core); t.add(sender);
        test_requests.add(t);
    }
    private void do_end(){
        active = false;
        ArrayList<Integer> branches = new ArrayList<Integer>();
        for(Edge e: edges){
            if(e.getState() == EdgeState.BRANCH)
                branches.add(e.getDest());
        }
        System.out.println(Integer.toString(uid) + " is connected to: ");
        for(int b: branches){
            System.out.println(b);
        }
    }

    private void do_initiate(int src, int core, int level, NodeState state, boolean merge){
        this.level = level;
        this.core = core;
        this.state = state;
        this.best_weight = Integer.MAX_VALUE;
        this.best_path = new ArrayList<>();

        if (merge)
        {
            this.other_core_node = src;
            this.leader = Math.max(src, this.leader);

        }
        else
        {
            this.other_core_node = -1;
        }

        getEdgeById(src).setState(EdgeState.BRANCH);

        ArrayList<Edge> my_branches = new ArrayList<Edge>();

        for (Edge e: edges)
        {
            if (e.state == EdgeState.BRANCH)
            {
                my_branches.add(e);
            }
        }

        my_branches.remove(getEdgeById(src));

        this.find_count = 0;

        for (Edge b: my_branches)
        {
            //this.send_message("Initiate", Integer.toString(core) + "," + Integer.toString(level) + "," + state.toString() + "," + "false", b);
            sendMsg(b.getDest(), "Initiate", core, level, state);
            if (state == NodeState.SEARCH)
            {
                find_count += 1;
            }
        }

        if (this.state == NodeState.SEARCH)
        {
            find_source = src;
            report_over = false;
            test();
        }
    }

    public void do_reject(int sender)
    {
        if (this.getEdgeById(sender).state == EdgeState.BASIC)
        {
            this.getEdgeById(sender).state = EdgeState.REJECTED;
        }

        this.test();
    }

    public void do_accept(int sender)
    {
        if (getEdgeById(sender).weight < best_weight)
        {
            best_path.add(Integer.toString(uid));
            best_path.add(Integer.toString(sender));
            best_weight = getEdgeById(sender).weight;

        }
        test_over = true;

    }

    public void do_report(int sender, int weight, String[] path)
    {
        if (sender == other_core_node)
        {
            expect_core_report = false;
        }

        else
        {
            if (find_count > 0)
            {
                find_count -= 1;
            }
        }

        if (weight < best_weight)
        {
            best_path = new ArrayList<>();   //?????????????????????????? - ili ne prvo na null???????????????? (MISLIM DA DA)
            best_path.add(Integer.toString(uid));
            for(var p: path)
            {
                best_path.add(p.toString());
            }
            best_weight = weight;
        }
    }


     public void do_change_root(int sender, String[] path)
     {
            if (path.length == 0)
            {
                //this.send_message("Connect", this.core.ToString() + "," + this.level.ToString(), this.getEdgeById(node));
                sendMsg(getEdgeById(sender).getDest(), "Connect", core, level);
                getEdgeById(sender).setState(EdgeState.BRANCH);
                sent_connect_to = sender;
            }
            else if (path.length > 1)
            {
                int hd = Integer.parseInt(path[1]);
                String[] t1 = new String[best_path.size()-2];
                for(int i = 2; i < best_path.size(); ++i) t1[i-2] = best_path.get(i);
                String s1 = String.join("::", t1);
                //this.send_message("ChangeRoot", s1, this.getEdgeById(hd));
                sendMsg(getEdgeById(hd).getDest(), "ChangeRoot", s1);
            }
     }

    public void report()
        {
            this.test_over = false;
            this.state = NodeState.FOUND;
            String s1 = String.join("::", best_path);
            //this.send_message("Report", Double.toString(best_weight) + "," + s1, getEdgeById(find_source));
            sendMsg(getEdgeById(find_source).getDest(), "Report", Integer.toString(best_weight) + " " + s1);
            this.report_over = true;
        }

        public void fragment_connect()
        {
            this.discovery = true;
            this.expect_core_report = false;
            this.report_over = false;

            if (Integer.parseInt((String)best_path.get(1)) != this.other_core_node)
            {
                int hd = Integer.parseInt(best_path.get(1));
                String[] t1 = new String[best_path.size() - 2];
                for(int i = 2; i < best_path.size(); ++i) t1[i-2] = best_path.get(i);
                String s1 = String.join("::", t1);
                sendMsg(getEdgeById(hd).getDest(), "ChangeRoot", s1);

            }
        }

    public void merge(int node)
        {
            Edge connection_edge = getEdgeById(node);

            int new_core = connection_edge.getWeight();
            connection_edge.setState(EdgeState.BRANCH);
            sendMsg(connection_edge.getDest(), "Initiate", Integer.toString(new_core) + " " + Integer.toString(level+1) + " " + NodeState.SEARCH.toString() + " " + "true");
            expect_core_report = true;
            discovery = false;
        }

    public void absorbe_node(int node)
    {
        //this.send_message("Initiate", Integer.toString(core) + "," + Integer.toString(level) + "," + state.toString() + "," + "false", this.getEdgeById(node));
        sendMsg(getEdgeById(node).getDest(), "Initiate", Integer.toString(core) + " " + Integer.toString(level) + " " + "false");

        getEdgeById(node).setState(EdgeState.BRANCH);

        if (state == NodeState.SEARCH)
        {
            if (find_count > 0)
            {
                find_count += 1;
            }
            else
            {
                find_count = 1;
            }
        }
    }

    private void test()
    {
        ArrayList<Edge> basic_edges = new ArrayList<Edge>();
        for(var e: edges)
        {
            if (e.getState() == EdgeState.BASIC)
            {
                basic_edges.add(e);
            }
        }
        test_over = false;

        if (basic_edges.size() > 0)
        {
            int min = basic_edges.get(0).getWeight();
            for(var e1: basic_edges)
            {
                if (e1.getWeight() < min)
                {
                    min = e1.getWeight();
                    test_edge = e1;
                }
            }

            //this.send_message("Test", Integer.toString(core) + "," + Integer.toString(level), test_edge);
            if(test_edge != null)
                sendMsg(test_edge.getDest(), "Test", core, level);

        }
        else
        {
            test_over = true;
        }
    }

    public void process_test_requests()
    {
        HashSet<ArrayList<Integer>> to_remove = new HashSet<ArrayList<Integer>>();

        for(var v: test_requests)
        {
            int L = v.get(0);
            int F = v.get(1);
            int j = v.get(2);

            if (F == this.core)
            {
                //this.send_message("Reject", "", this.getEdgeById(j));
                sendMsg(getEdgeById(j).getDest(), "Reject");
                to_remove.add(v);
            }
            else if (L <= this.level)
            {
                //this.send_message("Accept", "", this.getEdgeById(j));
                sendMsg(getEdgeById(j).getDest(), "Accept");
                to_remove.add(v);
            }

            for(var b: to_remove)
            {
                this.test_requests.remove(b);
            }
        }
    }

    public void main()
    {
        //Console.WriteLine("Node:" + this.uid + "entered main");
        while (this.active)
        {
            //Console.WriteLine("Node:" + this.uid + "entered main");
            int istina = 0;
            for(var v : test_requests)
            {
                if (v.get(1) == this.core || (v.get(1) != this.core && v.get(0) <= this.level))
                {
                    istina = 1;
                    break;
                }
            }
            //Console.WriteLine("Node: " + istina + " nakon provjere");
            //Console.WriteLine(this.connect_requests.requests.ContainsKey(this.sent_connect_to));
            if (this.connect_requests.requests.containsKey(this.sent_connect_to))
            {
                //Console.WriteLine("Node: " + this.uid + " going to merge");
                merge(sent_connect_to);
                connect_requests.requests.remove(sent_connect_to);
                sent_connect_to = -1;
            }
            else if (this.connect_requests.least_level() < this.level)
            {
                //Console.WriteLine("Node: " + this.uid + " going to absorb");
                int node = this.connect_requests.get_least_level_req();
                this.absorbe_node(node);
                this.connect_requests.requests.remove(node);
            }
            else if (istina == 1)
            {
                this.process_test_requests();
            }
            else if (this.test_over && this.find_count == 0)
            {
                //Console.WriteLine("Node: " + this.uid + " going to report");
                this.report();
            }
            else if (!expect_core_report)
            {
                if (report_over)
                {
                    if (discovery)
                    {
                        //Console.WriteLine("Node: " + this.uid + " going to fragment connect");
                        this.fragment_connect();
                    }
                }
            }
            //Thread.Sleep(1);
        }
    }

    private Edge getEdgeById(int id){
        for(var e: edges)
        {
            if ((int)e.getDest() == id)
            {
                return e;
            }
        }
        return null;
    }


}
