import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.javatuples.Pair;



public class MSTSync extends Process {
    Synchronizer s;
    ArrayList<Edge> outgoingEdges; //all adjacent edges
    int fragmentId; // id of leader node // F
    int sizeOfFragment; // number of nodes in fragment // L
    ArrayList<Edge> connected = new ArrayList<Edge>(); //adjacent edges that belong to MST
    Edge MWOE;
    int counter = 0; //num od DetMWOE messages received
    boolean connect = true; // flag for ConnectEdge message
    boolean update = false; //flag for UpdateFL message
    boolean determine = false; // flag for DetMWOE message
    boolean inform = false; // flag for InformMWOE message
    Edge sendFlagEdge = null; //flag for testing if the ConnectEdge message is sent
    boolean cycle; // flag for Cycle message
    int cycleSrc = -1;
    IntLinkedList cycleDest = new IntLinkedList();
    String lastTag = " ";
    int lastSrc = -1;
    ArrayList<Pair> previousRoundMsgs = new ArrayList<Pair>();
    boolean active = true;
    double notActiveSince = N*N*Math.ceil(Math.log(N))+ N*(N-1)/2;
    int p = 0;

    public MSTSync(Linker initComm, ArrayList<Edge> edges, Synchronizer initS){
        super(initComm);
        outgoingEdges = edges;
        Collections.sort(outgoingEdges);
        MWOE = outgoingEdges.get(0);
        fragmentId = myId;
        sizeOfFragment = 1;
        s = initS;
    }

    public void initiate(){
        cycle = false;
        if(MWOE == null) System.out.println("math ain't mathing");
        s.initialize(this);
        for(int pulse = 0; pulse < N*N*Math.ceil(Math.log(N)) + N*(N-1)/2; ++ pulse){
            //if(active)
            ++p;
           System.out.println("Novi puls");
           sendMessage(lastSrc, lastTag);
            s.nextPulse();
            System.out.println("pripadam clusteru "+ fragmentId + ", a velicina clustera je " + sizeOfFragment);
            if(!active) {
                //System.out.println("not active");
                //notActiveSince = pulse;
            }

        }
        System.out.println("Node " + myId + " is connected to: ");
        for(Edge e: connected){
            System.out.println(e.getDest());
        }
        System.out.println("jesam li aktivan " + active);
        System.out.println("nisam aktivan os pulsa broj " + notActiveSince);
    }

    private void sendMessage(int src, String tag){
       // System.out.println("Send");
        if(!active) return;
        if(MWOE != null && outgoingEdges.size() > 0 && connect){
            int dest = MWOE.getDest();
            //sendFlag = MWOE.getDest();
            sendFlagEdge = MWOE;
            //System.out.println(MWOE);
            s.sendMessage(MWOE.getDest(), "ConnectEdge", fragmentId, sizeOfFragment);
            //System.out.println(MWOE);
            //sendMsg(MWOE.getDest(), "ConnectEdge", fragmentId, size_of_fragment);

            //return;
        }
        if(update){
            System.out.println("idem u update");
            System.out.println("sizeof(connected) je " + connected.size());
            Iterator<Edge> it = connected.iterator();
            while(it.hasNext()){
                int dest = it.next().getDest();
                if(!tag.equals("UpdateFL") || dest != src)
                    s.sendMessage(dest, "UpdateFL", fragmentId, sizeOfFragment);
            }

            if(sizeOfFragment == N) {
                active = false;
                notActiveSince = p;
            }
            update = false;
            //return;
        }

        if(myId != fragmentId && determine){
            //if(!tag.equals("DetMWOE")) MWOE = outgoing_edges.get(0);
            //System.out.println("sizeof(connected) je " + connected.size());
            Iterator<Edge> it = connected.iterator();
            while(it.hasNext()){
                int dest = it.next().getDest();
                if(!tag.equals("DetMWOE") || dest != src){
                    if(MWOE != null)
                        s.sendMessage(dest, "DetMWOE", MWOE);
                    else{
                        Edge nullEdge = new Edge(-1,-1,-1,EdgeState.BASIC);
                        s.sendMessage(dest, "DetMWOE", nullEdge);
                    }
                }

            }
            determine = false;
            counter = 0;
            //return;
        }

        if(cycle && cycleSrc >= 0 && !containsEdgeConnectedByDest(cycleSrc)){
            s.sendMessage(cycleSrc, "Cycle", fragmentId);
            cycle = false;
            cycleSrc = -1;
        }
        else if(cycle && containsEdgeConnectedByDest(cycleSrc)){
            cycle = false;
            cycleSrc = -1;
        }

        if(inform){
            //System.out.println("sizeof(connected) je " + connected.size());
            Iterator<Edge> it = connected.iterator();
            while(it.hasNext()){
                int dest = it.next().getDest();
                if(!tag.equals("InformMWOE") || dest != src){
                    if(MWOE != null)
                        s.sendMessage(dest, "InformMWOE", MWOE);
                    else{
                        Edge nullEdge = new Edge(-1,-1,-1,EdgeState.BASIC);
                        s.sendMessage(dest, "InformMWOE", nullEdge);
                    }
                }

            }
            inform = false;
        }

    }

    public synchronized void handleMsg(Msg m, int src, String tag){
        //System.out.println("Handle message mst");
        if(tag.equals("ConnectEdge")){
            //System.out.println("ConnectEdge received from " + m.getSrcId());
            lastTag = tag;
            lastSrc = src;
            receive_ConnectEdge(m, src);
        }

        else if(tag.equals("UpdateFL")){
            //System.out.println("UpdateFL received from " + m.getSrcId());
            lastTag = tag;
            lastSrc = src;
            receive_UpdateFL(m, src);
        }

        else if(tag.equals("DetMWOE")){
            //System.out.println("DetMWOE received from " + m.getSrcId());
            Edge MWOE1 = m.getMessageEdge();
            lastTag = tag;
            lastSrc = src;
            receive_DetMWOE(m, src, MWOE1);
        }

        else if(tag.equals("InformMWOE")){
            //System.out.println("InformMWOE received from " + m.getSrcId());
            Edge MWOE1 = m.getMessageEdge();
            lastTag = tag;
            lastSrc = src;
            receive_InformMWOE(m, src, MWOE1);
        }

        else if (tag.equals("Cycle")){
            removeEdgeByDest(src);
            if(outgoingEdges.size() > 0)
                MWOE = outgoingEdges.get(0);
            else MWOE = null;
        }
    }

    private void receive_ConnectEdge(Msg m, int src){
        int senderFragmentId = m.getMessageInt();
        if(sendFlagEdge.getDest() == src){
            //System.out.println("sendFlag == src");
            s.sendMessage(src,"ConnectEdge", fragmentId, sizeOfFragment);
            if(fragmentId != senderFragmentId){
                System.out.println("različiti clusteri");
                sizeOfFragment += m.getMessageInt2();
                connected.add(sendFlagEdge);
                outgoingEdges.remove(sendFlagEdge);
                connect = false;
                //sendFlag = -1;
                sendFlagEdge = null;
                if(fragmentId < senderFragmentId){
                    System.out.println(senderFragmentId);
                    update = true;
                    determine = true;
                }
                fragmentId = Math.min(fragmentId, senderFragmentId);
                System.out.println("fragmentId je " + fragmentId);
                //MWOE = outgoing_edges.remove(0);
                //outgoing_edges.add(MWOE);
            }
            else{
                System.out.println("ciklus");
                outgoingEdges.remove(sendFlagEdge);
                determine = true;
                connect = false;
                //sendFlag = -1;
                sendFlagEdge = null;
                cycle = true;
                cycleSrc = src;

            }
            if(outgoingEdges.size() > 0)
                MWOE = outgoingEdges.get(0);
            else MWOE = null;
            System.out.println("preostalih bridova za provjeriti je " + outgoingEdges.size());
            //outgoing_edges.add(MWOE);
        }
        else{
            if(fragmentId == senderFragmentId){
                cycle = true;
                cycleSrc = src;
                System.out.println("ciklusSrc je " + cycleSrc);
                removeEdgeByDest(src);
            }
            //System.out.println("src != flag");
            System.out.println("src je " + src + ", a flag je " + sendFlagEdge.getDest());
        }
       // sendMessage(m.srcId, "ConnectEdge");
    }

    private void receive_UpdateFL(Msg m, int src){
        if(m.getMessageInt() <= fragmentId && sizeOfFragment <= m.getMessageInt2()){
            fragmentId = m.getMessageInt();
            sizeOfFragment = m.getMessageInt2();
            determine = true;
            update = true;
            if(outgoingEdges.size() > 0)
                MWOE = outgoingEdges.get(0);
            else MWOE = null;
        }
        else if(m.getMessageInt() == fragmentId && sizeOfFragment > m.getMessageInt2()){
            s.sendMessage(src, "UpdateFL", fragmentId, sizeOfFragment);
        }

        //sendMessage(m.srcId, "UpdateFL");
    }

    private void receive_DetMWOE(Msg m, int src, Edge MWOE1){
        System.out.println("Stari MWOE je " + MWOE);
        //if(MWOE1.getSrc() == myId && containsEdge(MWOE1))
        MWOE = minEdge(MWOE,  MWOE1);
        //else if(MWOE1.getSrc() != myId && MWOE1.getDest() != myId) MWOE = minEdge(MWOE,  MWOE1);
        System.out.println("Novi MWOE je " + MWOE);
        determine = true;
        counter++;
        //System.out.println("counter je "+ counter + ", a size_of_fragment je " + size_of_fragment);
        if(myId == fragmentId && counter == sizeOfFragment - 1){
            counter = 0;
            inform = true;
            //if(outgoing_edges.contains(MWOE)) connect = true;
            System.out.println("provjera postoji li " + MWOE);
            if(containsEdge(MWOE)) {
                System.out.println(MWOE);
                System.out.println("contains MWOE");
                connect = true;
            }
        }
        //sendMessage(m.srcId, "detMWOE");
    }

    private void receive_InformMWOE(Msg m, int src, Edge MWOE1){
        System.out.println("handling InformMWOE");
        inform = true;
        MWOE = MWOE1;
        System.out.println("provjeravam sadrzi li " + MWOE);
        if(containsEdge(MWOE)) {
            System.out.println("contains MWOE");
            connect = true;
        }

        //sendMessage(m.srcId, "InformMWOE");
    }

    private Edge minEdge(Edge e1, Edge e2){
        if(e1 == null) return e2;
        else if(e2 == null) return e1;
        else return (e1.getWeight() < e2.getWeight()) ? e1 : e2;
    }


    private boolean containsEdge(Edge e){
        if(e == null) return false;
        for (Edge e1: outgoingEdges) {
            if(e1.getDest() == e.getDest() && e1.getWeight() == e.getWeight() && e1.getSrc() == e.getSrc())
                return true;
        }
        return false;
    }

    private void removeEdgeByDest(int d){
        int index = -1;
        for(int i = 0; i < outgoingEdges.size(); ++i){
            if(outgoingEdges.get(i).getDest() == d) {
                index = i;
                break;
            }
        }
        if(index > -1)
        outgoingEdges.remove(index);
    }

    private boolean containsEdgeConnectedByDest(int d){
        for(Edge e: connected){
            if(e.getDest() == d) return true;
        }
        return false;
    }
}
