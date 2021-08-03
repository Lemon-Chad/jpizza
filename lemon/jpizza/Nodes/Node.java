package lemon.jpizza.Nodes;

import lemon.jpizza.Constants.JPType;
import lemon.jpizza.Position;

import java.io.Serializable;

public class Node implements Serializable {
    public Position pos_start;
    public Position pos_end;
    public JPType jptype;
}
