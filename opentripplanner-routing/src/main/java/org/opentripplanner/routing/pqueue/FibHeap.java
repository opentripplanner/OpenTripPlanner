/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.pqueue;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

class FibNode<T> {
    boolean mark;

    int degree;

    LinkedList<FibNode<T>> children;

    FibNode<T> parent;

    double key;

    T payload;

    FibNode(T payload, double key) {
        this.children = new LinkedList<FibNode<T>>();
        this.key = key;
        this.degree = 0;
        this.parent = null;
        this.mark = false;
        this.payload = payload;
    }

    void addChild(FibNode<T> child) {
        this.children.add(child);
    }

    public String toString() {
        return (new Double(this.key)).toString();
    }
}

public class FibHeap<T> implements OTPPriorityQueue<T> {
    
    public static final OTPPriorityQueueFactory FACTORY = new FibHeapFactory();

    int n;

    public FibNode<T> min;

    public LinkedList<FibNode<T>> root_list;

    public HashMap<T, FibNode<T>> directory;

    private FibNode<T>[] rootnode_with_degree;

    int maxSize, maxRootNodes;

    private FibNode<T>[] roots;

    private int getMaxRootNodes(int maxSize) {
        // ceil(log2(maxSize))
        return 32 - Integer.numberOfLeadingZeros(maxSize);
    }

    @SuppressWarnings("unchecked")
    public FibHeap(int maxSize) {
        n = 0;
        min = null;
        root_list = new LinkedList<FibNode<T>>();
        directory = new HashMap<T, FibNode<T>>();
        this.maxSize = maxSize;
        maxRootNodes = getMaxRootNodes(maxSize);
        rootnode_with_degree = new FibNode[maxRootNodes];
        roots = new FibNode[maxRootNodes];
    }

    FibHeap<T> union(FibHeap<T> H1, FibHeap<T> H2) {
        FibHeap<T> H = new FibHeap<T>(H1.maxSize + H2.maxSize);
        H.root_list.addAll(H1.root_list);
        H.root_list.addAll(H2.root_list);
        H.min = H1.min;

        if (H1.min == null || (H2.min != null && H2.min.key < H1.min.key)) {
            H.min = H2.min;
        }

        H.n = H1.n + H2.n;

        return H;
    }

    void link(FibNode<T> y, FibNode<T> x) {
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

        roots = root_list.toArray(roots);
        for (int i = 0; i < roots.length; i++) {
            FibNode<T> x = roots[i];
            if (x == null) { 
                break;
            }
            int d = x.degree;
            while (rootnode_with_degree[d] != null) {
                FibNode<T> y = rootnode_with_degree[d];

                // System.out.println( y+" and "+x+" with degree "+d+", link" );

                if (x.key > y.key) {
                    FibNode<T> z = y;
                    y = x;
                    x = z;
                }
                this.link(y, x);
                rootnode_with_degree[d] = null;
                d = d + 1;
            }
            rootnode_with_degree[d] = x;
        }

        min = null;

        for (FibNode<T> w : root_list) {
            if (min == null || w.key < min.key) {
                min = w;
            }
        }
    }

    @Override
    public void insert(T payload, double key) {
        FibNode<T> x = new FibNode<T>(payload, key);
        this.directory.put(payload, x);

        this.root_list.add(x);

        if (this.min == null || x.key < this.min.key) {
            this.min = x;
        }
        this.n = this.n + 1;
    }

    @Override
    public void insert_or_dec_key(T payload, double key) {
        FibNode<T> exists = this.directory.get(payload);
        if (exists != null) {
            this.decrease_node_key(exists, key);
        } else {
            this.insert(payload, key);
        }
    }

    @Override
    public T extract_min() {
        FibNode<T> z = this.min;

        // System.out.println( "going to return "+z+"; cleaning up" );

        ListIterator<FibNode<T>> e = z.children.listIterator();
        while (e.hasNext()) {
            FibNode<T> x = e.next();
            this.root_list.add(x);
            x.parent = null;
        }

        this.root_list.remove(z);
        this.consolidate();

        this.n = this.n - 1;
        this.directory.remove(z.payload);
        
        return z.payload;
    }

    void decrease_node_key(FibNode<T> x, double k) {
        if (k >= x.key) {
            return;
        }
        assert k < x.key : "new key " + k + " is greater than current key " + x.key;

        x.key = k;
        FibNode<T> parent = x.parent;
        if (parent != null && x.key < parent.key) {
            this.cut(x, parent);
            this.cascading_cut(parent);
        }

        if (x.key < this.min.key) {
            this.min = x;
        }
    }

    public boolean contains(Object payload) {
        return directory.containsKey(payload);
    }
    
    public void adjust_weight_if_exists(T payload, double k) {
        FibNode<T> x =  directory.get(payload);
        if (x == null) {
            return;
        }
        if (x.key > k) {
            decrease_node_key(x, k);
        } else if (x.key < k) {
            decrease_node_key(x, Double.NEGATIVE_INFINITY);
            extract_min();
            insert(payload, k);
        }
    }
    
    void decrease_key(T payload, double k) {
        FibNode<T> x = this.directory.get(payload);

        this.decrease_node_key(x, k);
    }

    void cut(FibNode<T> x, FibNode<T> y) {
        y.children.remove(x);
        y.degree = y.degree - 1;
        this.root_list.add(x);
        x.parent = null;
        x.mark = false;
    }

    void cascading_cut(FibNode<T> y) {
        FibNode<T> z = y.parent;
        if (z != null) {
            if (y.mark == false) {
                y.mark = true;
            } else {
                this.cut(y, z);
                this.cascading_cut(z);
            }
        }
    }

    @Override
    public int size() {
        return this.n;
    }

    @Override
    public boolean empty() {
        return this.n == 0;
    }

    public static void main(String args[]) {
        FibHeap<String> fh = new FibHeap<String>(1000);

        Random rr = new Random();
        for (int i = 0; i < 1000; i++) {
            int randkey = rr.nextInt(10000);
            System.out.println("inserting " + randkey);
            fh.insert(Integer.toString(i) + " : " + randkey, randkey);
        }

        for (int i = 0; i < 1000; i++) {
            // System.out.print( fh.min.key+", " );
            System.out.println(fh.extract_min());
        }
    }

    @Override
    public double peek_min_key() {
        if (min == null) {
            return Double.NaN;
        }
        return min.key;
    }

    @Override
    public T peek_min() {
        return min.payload;
    }
    
    private static class FibHeapFactory implements OTPPriorityQueueFactory {
        @Override
        public <T> OTPPriorityQueue<T> create(int maxSize) {
            return new FibHeap<T>(maxSize);
        }
    }
}