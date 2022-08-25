import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

public class ConnectRequests {

    public HashMap<Integer, Integer> requests;

    public ConnectRequests()
    {
        requests = new HashMap<>();
    }


    public void insert(int p, int level)
    {
        if (!requests.containsKey(p))
            requests.replace(p,level);
    }

    public int get_least_level_req()
    {  //get the key corresponding to the minimum value in the dictionary
        if (requests.size() != 0) {
            Integer minObj = Integer.MAX_VALUE;
            int min = Integer.MAX_VALUE;
            for (Map.Entry<Integer, Integer> x : requests.entrySet()) {
                if (x.getValue() < min) {
                    min = x.getValue();
                    minObj = x.getKey();
                }
            }
            return minObj;
        }
        else return -1;
    }

    public int least_level()
    {  // get that minimum value
        if (this.requests.size()!= 0 && this.get_least_level_req() != -1)
            return requests.get(get_least_level_req());
        else
            return Integer.MAX_VALUE;
    }
}
