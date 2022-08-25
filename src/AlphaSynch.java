import java.util.LinkedList;
public class AlphaSynch extends Process implements Synchronizer {
    int pulse = -1;
    int acksNeeded = 0;
    IntLinkedList unsafe = new IntLinkedList();
    LinkedList nextPulseMsgs = new LinkedList();//msgs for next pulse
    boolean meSafe;
    MsgHandler prog;
    public AlphaSynch(Linker initComm) {
        super(initComm);
    }
    public synchronized void initialize(MsgHandler initProg) {
        prog = initProg;
        startPulse();
        notifyAll();
    }
    void startPulse(){
        unsafe.addAll(comm.neighbors);
        meSafe = false;
        pulse ++;
        Util.println("**** new pulse ****: " + pulse);
    }
    public synchronized void handleMsg(Msg m, int src, String tag) {
        //System.out.println("Handle msg sinkronizator");
        while (pulse < 0) myWait();
        if (tag.equals("synchAck")) {
            //System.out.println("synchAck");
            acksNeeded--;
            if (acksNeeded == 0) notifyAll();
        } else if (tag.equals("safe")) {
            while (!unsafe.contains(src)) myWait();
            unsafe.removeObject(src);
            if (unsafe.isEmpty()) notifyAll();
        } else { // application message
            //System.out.println("app message");
            sendMsg(src, "synchAck", 0);
            while (!unsafe.contains(src)) myWait();
            //System.out.println("out from while");
            if (meSafe) {
                //System.out.println("meSafe is true");
                nextPulseMsgs.add(m);
            }
            else {
                //System.out.println("hendlaj u programu");
                //System.out.println("hendlaj u programu opet");
                prog.handleMsg(m, src, tag);
                //System.out.println(prog.toString());
                //System.out.println("hendlano u programu");
            }
        }
    }
    public synchronized void sendMessage(int destId, String tag, int msg) {
        acksNeeded++;
        sendMsg(destId, tag, msg);
    }

    public synchronized void sendMessage(int destId, String tag, int m1, int m2){
        acksNeeded++;
        sendMsg(destId, tag, m1, m2);
    }

    public synchronized void sendMessage(int destId, String tag, int msg1, int msg2, NodeState state){
        acksNeeded++;
        sendMsg(destId, tag, String.valueOf(msg1) + " " + String.valueOf(msg2) + " " + state.toString() + " ");
    }

    public synchronized void sendMessage(int destId, String tag, Edge e) {
        acksNeeded++;
        sendMsg(destId, tag, e.toString() + " ");
    }

    public synchronized void nextPulse() {
        System.out.println("entered nextPulse");
        while (acksNeeded != 0){
            System.out.println("acksNeeded je " + acksNeeded);
            myWait();
        }
        System.out.println("no acks needed");
        meSafe = true;
        sendToNeighbors("safe", 0);
        System.out.println("sizeof(unsafe) je " + unsafe.size());
        while (!unsafe.isEmpty()) myWait();
        System.out.println("unsafe is empty");
        startPulse();
        while (!nextPulseMsgs.isEmpty()) {//act on msgs received earlier
            //System.out.println("nextPulseMsg is not empty");
            Msg m = (Msg) nextPulseMsgs.removeFirst();
            prog.handleMsg(m, m.getSrcId(), m.getTag());
        }
        notifyAll();
        System.out.println("izlazim iz nextPulse");
    }
}
