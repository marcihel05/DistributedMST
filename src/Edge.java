public class Edge implements Comparable<Edge>{

    int src;
    int dest;
    int weight;
    EdgeState state = EdgeState.BASIC;
    
    public Edge(int s, int d, int w, EdgeState st){
        src = s;
        dest = d;
        weight = w;
        state = st;
    }
    
    public int getDest(){
        return dest;
    }
    
    public int getWeight(){
        return weight;
    }

    public int getSrc() {return src; }
    
    public EdgeState getState(){
        return state;
    }
    
    public void setState(EdgeState s){
        state = s;
    }

    @Override
    public String toString(){
        return String.valueOf(src) + " " + String.valueOf(dest) + " " + String.valueOf(weight);
    }

    @Override
    public int compareTo(Edge otherEdge){
        return Integer.compare(weight, otherEdge.getWeight());
    }

}
