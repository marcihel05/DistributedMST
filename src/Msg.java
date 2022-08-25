import java.util.*;
public class Msg {
    int srcId, destId;
    String tag;
    String msgBuf;
    public Msg(int s, int t, String msgType, String buf) {
        this.srcId = s;
        destId = t;
        tag = msgType;
        msgBuf = buf;
    }
    public int getSrcId() {
        return srcId;
    }
    public int getDestId() {
        return destId;
    }
    public String getTag() {
        return tag;
    }
    public String getMessage() {
        return msgBuf;
    }
    public int getMessageInt() {
        StringTokenizer st = new StringTokenizer(msgBuf);
        return Integer.parseInt(st.nextToken());
    }
    
    public int getMessageInt2() {
        StringTokenizer st = new StringTokenizer(msgBuf);
        int firstInt = Integer.parseInt(st.nextToken());
        return Integer.parseInt(st.nextToken());
    }

    public int getMessageInt3() {
        StringTokenizer st = new StringTokenizer(msgBuf);
        int firstInt = Integer.parseInt(st.nextToken());
        int secInt = Integer.parseInt(st.nextToken());
        return Integer.parseInt(st.nextToken());
    }

    public int getMessageInt4() {
        StringTokenizer st = new StringTokenizer(msgBuf);
        int firstInt = Integer.parseInt(st.nextToken());
        int secToken = Integer.parseInt(st.nextToken());
        int thirdToken = Integer.parseInt(st.nextToken());
        return Integer.parseInt(st.nextToken());
    }

    public Edge getMessageEdge(){
        StringTokenizer st = new StringTokenizer(msgBuf);
        int src = Integer.parseInt(st.nextToken());
        int dest = Integer.parseInt(st.nextToken());
        int weight = Integer.parseInt(st.nextToken());
        Edge ret = new Edge(src, dest, weight, EdgeState.BASIC);
        if(ret.getWeight() == -1) return null;
        return ret;
    }

    public boolean getMessageBool(){
        var split = msgBuf.split(" ");
        String torf = split[3];
        if(torf == "true") return true;
        return false;
    }

    public String[] getMessagePath(){
        var split = msgBuf.split(" ");
        String path = split[split.length-1];
        var retPath = path.split("::");
        return retPath;

    }

    public NodeState getMessageState(){
        var split = msgBuf.split(" ");
        String state = split[3];
        System.out.println(state);
        return NodeState.valueOf(state);
    }

    public static Msg parseMsg(StringTokenizer st){
        int srcId = Integer.parseInt(st.nextToken());
        int destId = Integer.parseInt(st.nextToken());
        String tag = st.nextToken();
        String buf = st.nextToken("#");
        return new Msg(srcId, destId, tag, buf);
    }
    public String toString(){
        String s = String.valueOf(srcId)+" " +
                    String.valueOf(destId)+ " " +
                    tag + " " + msgBuf + "#";
        return s;
    }
}
