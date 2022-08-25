/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.util.ArrayList;
import java.util.Collections;

/**
 *
 * @author helena
 *
 */

public class Node extends Process {


    ArrayList<Edge> neighbors;
    //IntLinkedList weights;
    int clusterId = -1;
    int level = 0;
    Edge best_edge;
    int best_weight = -1;
    Edge test_edge = null;
    int parent = -1;
    int find_count = 0;
    boolean done = false;
    NodeState state = NodeState.SLEEP;
    ArrayList<Msg> waitList = new ArrayList<>();
    
    
    public Node(Linker initComm, ArrayList<Edge> initCost){
        super(initComm);
        neighbors = initCost;
        clusterId = myId;
        //weights = new IntLinkedList();
        Collections.sort(neighbors);
        //pošalji najkraćem poruku
    }
    

    public void initiate(){
        wakeup();
        System.out.println("proba za enum " + NodeState.SLEEP);
        System.out.println("proba za enum " + NodeState.SLEEP.toString());
        while (true){
            if(waitList.size() > 0) handleWaitList();
        }
    }
        
    public void handleMsg(Msg m, int src, String tag){

        // int edgeIndex = getEdgeById(src).getDest();
        //int destId = neighbors.get(edgeIndex).getDest();
        
        if(tag.equals("Initiate")){
            level = m.getMessageInt();
            clusterId = m.getMessageInt2();
            state = m.getMessageState();
            parent = src;
            best_edge = null;
            best_weight = -1;
            for(Edge e : neighbors){
                if(e.getDest() != src && e.getState() == EdgeState.BRANCH){
                    sendMsg(e.getDest(), "Initiate", level, clusterId, state);
                    if(state == NodeState.FOUND) ++find_count;
                }
            }
            if(state == NodeState.FOUND) test();
        }
        
        else if(tag.equals("Connect")){
            int srcLevel = m.getMessageInt();
            if(level > srcLevel){
                getEdgeById(src).setState(EdgeState.BRANCH);
                sendMsg(src, "Initiate", level, clusterId,  state);
                if(state == NodeState.FOUND) find_count++;
            }
            else if(getEdgeById(src).getState() == EdgeState.BASIC){
            waitList.add(m);
            }
            else{
                sendMsg(src,"Initiate", level+1, getEdgeById(src).getWeight(),  "FOUND");
            }
        }
        
        
        else if(tag.equals("Test")){
            if(m.getMessageInt() > level){
                waitList.add(m);
            }
            if(m.getMessageInt2() != clusterId){
                sendMsg(src, "Accept", "");
            }
            else {
                if(getEdgeById(src).getState() == EdgeState.BASIC) getEdgeById(src).setState(EdgeState.REJECTED);
                else if(getEdgeById(src) != test_edge) sendMsg(src, "Reject", "");
                else test();
            }
        }
        
        else if(tag.equals("Accept")){
            test_edge = null;
            if(getEdgeById(src).getWeight() < best_weight){
                best_edge = getEdgeById(src);
                best_weight = best_edge.getWeight();
            }
            report();
        }
        
        else if(tag.equals("Reject")){
            if(getEdgeById(src).getState() == EdgeState.BASIC)
                getEdgeById(src).setState(EdgeState.REJECTED);
            test();
        }
        
        else if(tag.equals("Report")){
            if(src != parent){
                find_count -= 1;
                if(m.getMessageInt3() < best_weight){
                    best_weight = m.getMessageInt3();
                    best_edge = getEdgeById(src);
                    
                }
                report();
            }
            else{
                if(state == NodeState.FOUND){
                    waitList.add(m);
                }
                else if(m.getMessageInt3() > best_weight) changeRoot();
                else if(m.getMessageInt3() == Integer.MAX_VALUE && best_weight == Integer.MAX_VALUE) {
                    done = true;
                    System.out.println("Node " + myId + " is connected to");
                    for(Edge e: neighbors){
                        if(e.getState() == EdgeState.BRANCH) System.out.println(e.getDest());
                    }
                }
            }
        }
        
        else if(tag.equals("Change_root")){
            changeRoot();
        }
        //handleWaitList();
    }

    private void handleWaitList(){
        ArrayList<Msg> toRemove = new ArrayList<>();
        for(Msg m : waitList){
            if(m.getTag().equals("Connect")){
                int srcLevel = m.getMessageInt();
                if(level > srcLevel){
                    getEdgeById(m.getSrcId()).setState(EdgeState.BRANCH);
                    sendMsg(m.getSrcId(), "Initiate", level, clusterId, state);
                    if(state == NodeState.FOUND) find_count++;
                    toRemove.add(m);
                }
                else if(getEdgeById(m.getSrcId()).getState() == EdgeState.BASIC){
                    if(!waitList.contains(m))
                        waitList.add(m);
                }
                else{
                    sendMsg(m.getSrcId(),"Initiate", level+1, getEdgeById(m.getSrcId()).getWeight(),   "FOUND");
                    toRemove.add(m);
                }
            }

            else if(m.getTag().equals("Test")){
                if(m.getMessageInt() > level){
                    if(!waitList.contains(m))
                        waitList.add(m);
                }
                if(m.getMessageInt2() != clusterId){
                    sendMsg(m.getSrcId(), "Accept", "");
                    toRemove.add(m);
                }
                else {
                    if(getEdgeById(m.getSrcId()).getState() == EdgeState.BASIC) getEdgeById(m.getSrcId()).setState(EdgeState.REJECTED);
                    else if(getEdgeById(m.getSrcId()) != test_edge) sendMsg(m.getSrcId(), "Reject", "");
                    else test();
                    toRemove.add(m);
                }
            }

            else if(m.getTag().equals("Report")){
                if(m.getSrcId()!= parent){
                    find_count -= 1;
                    if(m.getMessageInt3() < best_weight){
                        best_weight = m.getMessageInt3();
                        best_edge = getEdgeById(m.getSrcId());

                    }
                    report();
                    toRemove.add(m);
                }
                else{
                    if(state == NodeState.FOUND){
                        if(!waitList.contains(m))
                            waitList.add(m);
                    }
                    else if(m.getMessageInt3() > best_weight) {
                        changeRoot();
                        toRemove.add(m);
                    }
                    else if(m.getMessageInt3() == Integer.MAX_VALUE && best_weight == Integer.MAX_VALUE) {
                        toRemove.add(m);
                        done = true;
                        System.out.println("Node " + myId + " is connected to");
                        for(Edge e: neighbors){
                            if(e.getState() == EdgeState.BRANCH) System.out.println(e.getDest());
                        }
                    }
                }
            }

        }
        for(Msg m: toRemove){
            waitList.remove(m);
        }
    }

    private void wakeup(){
        neighbors.get(0).setState(EdgeState.BRANCH);
        find_count = 0;
        level = 0;
        state = NodeState.FOUND;
        sendMsg(neighbors.get(0).getDest(), "Connect", level);
    }
    private void test(){
        boolean flag_found = false;
        for(Edge e : neighbors){
            if(e.getState() == EdgeState.BASIC && !flag_found){
                flag_found = true;
                test_edge = e;
                sendMsg(e.getDest(), "Test", level, clusterId);
            }
        }
        
        if(!flag_found){
            test_edge = null;
            report();
        }
    }
    
    private void report(){
        if(find_count == 0 && test_edge == null){
            state = NodeState.FOUND;
            sendMsg(parent, "Report", 0, 0, best_weight);
        }
    }
    
    private void changeRoot(){
        int best_edge_index = best_edge.getDest();
        //Edge best_edge = getEdgeById(b)
        if (best_edge.getState() == EdgeState.BRANCH){
            sendMsg(best_edge.getDest(), "Change_root");
        }
        else{
            neighbors.get(best_edge_index).setState(EdgeState.BRANCH);
            sendMsg(best_edge.getDest(), "Connect", level);
        }
    }

    private Edge getEdgeById(int id){
        for(Edge e: neighbors)
        {
            if (e.getDest() == id)
            {
                return e;
            }
        }
        return null;
    }
}
