public interface Synchronizer extends MsgHandler {
	public void initialize(MsgHandler initProg);
    	public void sendMessage(int destId, String tag, int msg);
	public void sendMessage(int destId, String tag, int m1, int m2);
	public void sendMessage(int destId, String tag, int msg1, int msg2, NodeState state);
	public void sendMessage(int destId, String tag, Edge e);
	public void nextPulse();// block for the next pulse 
}
