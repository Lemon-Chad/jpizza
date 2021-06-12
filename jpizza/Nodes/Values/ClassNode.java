package lemon.jpizza.Nodes.Values;

import lemon.jpizza.Contextuals.Context;
import lemon.jpizza.Nodes.Definitions.MethDefNode;
import lemon.jpizza.Nodes.Node;

import java.util.List;

public class ClassNode extends Node {
    Context context;
    List<MethDefNode> methods;

    public ClassNode(Context context, List<MethDefNode> methods) {
        this.context = context;
        this.methods = methods;
    }

}
