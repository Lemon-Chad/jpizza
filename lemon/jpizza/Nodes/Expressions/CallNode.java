package lemon.jpizza.Nodes.Expressions;

import lemon.jpizza.Constants;
import lemon.jpizza.Nodes.Node;

import java.util.List;

public class CallNode extends Node {
    public Node nodeToCall;
    public List<Node> argNodes;

    public CallNode(Node nodeToCall, List<Node> argNodes) {
        this.nodeToCall = nodeToCall;
        this.argNodes = argNodes;

        pos_start = nodeToCall.pos_start.copy();
        pos_end = (argNodes != null && argNodes.size() > 0 ? argNodes.get(argNodes.size() - 1) : nodeToCall).pos_end.copy();
        jptype = Constants.JPType.Call;
    }

}
