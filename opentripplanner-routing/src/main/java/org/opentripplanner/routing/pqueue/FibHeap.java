package org.opentripplanner.routing.pqueue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

interface AbstractDirectoryPriorityQueue {
    void insert(Object payload, double key);

    void insert_or_dec_key(Object payload, double key);

    Object extract_min();

    int getN();
}

class FibNode {
    boolean mark;

    int degree;

    LinkedList<FibNode> children;

    FibNode parent;

    double key;

    Object payload;

    FibNode(Object payload, double key) {
        this.children = new LinkedList<FibNode>();
        this.key = key;
        this.degree = 0;
        this.parent = null;
        this.mark = false;
        this.payload = payload;
    }

    void addChild(FibNode child) {
        this.children.add(child);
    }

    public String toString() {
        return (new Double(this.key)).toString();
    }
}

public class FibHeap implements AbstractDirectoryPriorityQueue {
    private static final double LOG2 = Math.log(2);

    int n;

    public FibNode min;

    public LinkedList<FibNode> root_list;

    public HashMap<Object, FibNode> directory;

    private FibNode[] rootnode_with_degree;

    int maxSize, maxRootNodes;

    private int getMaxRootNodes(int maxSize) {
        return (int) Math.ceil(Math.log(maxSize) / LOG2);
    }

    public FibHeap(int maxSize) {
        n = 0;
        min = null;
        root_list = new LinkedList<FibNode>();
        directory = new HashMap<Object, FibNode>();
        this.maxSize = maxSize;
        maxRootNodes = getMaxRootNodes(maxSize);
        rootnode_with_degree = new FibNode[maxRootNodes];
    }

    static FibHeap union(FibHeap H1, FibHeap H2) {
        FibHeap H = new FibHeap(H1.maxSize + H2.maxSize);
        H.root_list.addAll(H1.root_list);
        H.root_list.addAll(H2.root_list);
        H.min = H1.min;

        if (H1.min == null || (H2.min != null && H2.min.key < H1.min.key)) {
            H.min = H2.min;
        }

        H.n = H1.n + H2.n;

        return H;
    }

    void link(FibNode y, FibNode x) {
        // System.out.println( y+" now child of "+x );

        this.root_list.remove(y);
        x.addChild(y);
        x.degree += 1;
        y.mark = false;
        y.parent = x;

        // System.out.println( x+" now has degree "+x.degree );
    }

    void consolidate() {
        // System.out.println( "consolidating" );

        for (int i = 0; i < maxRootNodes; ++i) {
            rootnode_with_degree[i] = null;
        }

        Object[] root_list_array = this.root_list.toArray();
        for (int i = 0; i < root_list_array.length; i++) {
            FibNode w = (FibNode) root_list_array[i];
            FibNode x = w;
            int d = x.degree;
            while (rootnode_with_degree[d] != null) {
                FibNode y = rootnode_with_degree[d];

                // System.out.println( y+" and "+x+" with degree "+d+", link" );

                if (x.key > y.key) {
                    FibNode z = y;
                    y = x;
                    x = z;
                }
                this.link(y, x);
                rootnode_with_degree[d] = null;
                d = d + 1;
            }
            rootnode_with_degree[d] = x;
        }

        this.min = null;

        ListIterator<FibNode> e = this.root_list.listIterator();
        while (e.hasNext()) {
            FibNode w = e.next();
            if (this.min == null || w.key < this.min.key) {
                this.min = w;
            }
        }
    }

    public void insert(Object payload, double key) {
        FibNode x = new FibNode(payload, key);
        this.directory.put(payload, x);

        this.root_list.add(x);

        if (this.min == null || x.key < this.min.key) {
            this.min = x;
        }
        this.n = this.n + 1;
    }

    public void insert_or_dec_key(Object payload, double key) {
        FibNode exists = (FibNode) this.directory.get(payload);
        if (exists != null) {
            this.decrease_node_key(exists, key);
        } else {
            this.insert(payload, key);
        }
    }

    public Object extract_min() {
        FibNode z = this.min;

        // System.out.println( "going to return "+z+"; cleaning up" );

        ListIterator<FibNode> e = z.children.listIterator();
        while (e.hasNext()) {
            FibNode x = e.next();
            this.root_list.add(x);
            x.parent = null;
        }

        this.root_list.remove(z);
        this.consolidate();

        this.n = this.n - 1;

        return z.payload;
    }

    void decrease_node_key(FibNode x, double k) {
        assert k < x.key : "new key " + k + " is greater than current key " + x.key;

        x.key = k;
        FibNode y = x.parent;
        if (y != null && x.key < y.key) {
            this.cut(x, y);
            this.cascading_cut(y);
        }

        if (x.key < this.min.key) {
            this.min = x;
        }
    }

    void decrease_key(Object payload, double k) {
        FibNode x = (FibNode) this.directory.get(payload);

        this.decrease_node_key(x, k);
    }

    void cut(FibNode x, FibNode y) {
        y.children.remove(x);
        y.degree = y.degree - 1;
        this.root_list.add(x);
        x.parent = null;
        x.mark = false;
    }

    void cascading_cut(FibNode y) {
        FibNode z = y.parent;
        if (z != null) {
            if (y.mark == false) {
                y.mark = true;
            } else {
                this.cut(y, z);
                this.cascading_cut(z);
            }
        }
    }

    public int getN() {
        return this.n;
    }

    public boolean empty() {
        return this.n == 0;
    }

    public static void selfTest() {
        FibHeap fh = new FibHeap(1000);

        Random rr = new Random();
        for (int i = 0; i < 1000; i++) {
            int randkey = rr.nextInt(10000);
            System.out.println("inserting " + randkey);
            fh.insert(Integer.toString(i), randkey);
        }

        for (int i = 0; i < 1000; i++) {
            // System.out.print( fh.min.key+", " );
            System.out.println(fh.extract_min());
        }
    }

}