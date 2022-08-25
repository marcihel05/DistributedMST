import java.io.*;
public class ListenerThread extends Thread {
    int channel;
    MsgHandler process;
    public ListenerThread(int channel, MsgHandler process) {
        this.channel = channel;
        this.process = process;
    }
    public void run() {
        while (true) {
            try {
                Msg m = process.receiveMsg(channel);
                //System.out.println("Received msg");
                process.handleMsg(m, m.getSrcId(), m.getTag());
                //System.out.println("Handled msg");
            } catch (IOException e) {
                System.err.println(e);            
            }
        }
    }
}
