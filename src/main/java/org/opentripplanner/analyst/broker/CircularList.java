package org.opentripplanner.analyst.broker;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 *
 */
public class CircularList<T> implements Iterable<T> {

    Node<T> head = null;

    public class CircularListIterator implements Iterator<T> {

        Node<T> curr = head;

        @Override
        public boolean hasNext() {
            return curr != null;
        }

        @Override
        public T next() {
            T currElement = curr.element;
            curr = curr.next;
            if (curr == head) {
                curr = null; // No more elements
            }
            return currElement;
        }
    }

    @Override
    public Iterator<T> iterator() {
        return new CircularListIterator();
    }

    @Override
    public void forEach(Consumer<? super T> action) {
        Node<T> curr = head;
        do {
            action.accept(curr.element);
            curr = curr.next;
        } while (curr != head);
    }

    @Override
    public Spliterator<T> spliterator() {
        throw new UnsupportedOperationException();
    }

    private static class Node<T> {
        Node prev;
        Node next;
        T element;
    }

    /** Insert an element at the tail of the circular list (the position just previous to the head). */
    public void insertAtTail (T element) {
        Node<T> newNode = new Node<>();
        newNode.element = element;
        if (head == null) {
            newNode.next = newNode;
            newNode.prev = newNode;
            head = newNode;
        } else {
            newNode.prev = head.prev;
            newNode.next = head;
            head.prev.next = newNode;
            head.prev = newNode;
        }
    }


    /** Insert a new element at the head of the circular list. */
    public void insertAtHead (T element) {
        // Add at tail and then back the head up one element, wrapping around to the tail.
        insertAtTail(element);
        head = head.prev;
    }

    /** Get the element at the head of the list without removing it. */
    public T peek () {
        return head.element;
    }

    /** Take an element off the head of the list, removing it from the list. */
    public T pop () {
        if (head == null) {
            return null;
        }
        T element = head.element;
        if (head == head.next) {
            // Last node consumed. List is now empty.
            head = null;
        } else {
            head.prev.next = head.next;
            head.next.prev = head.prev;
            head = head.next;
        }
        return element;
    }

    /**
     * Advance the head of the circular list forward to the next element.
     * Return the element at the head of the list, leaving that element in the list.
     */
    public T advance() {
        if (head == null) {
            return null;
        }
        T headElement = head.element;
        head = head.next;
        return headElement;
    }

    /**
     * Advances through the circular list returning the first element for which the predicate evaluates to true.
     * If there is no such element, returns null leaving the head of the list back in the same place.
     */
    public T advanceToElement (Predicate<T> predicate) {
        Node<T> start = head;

        if (head == null)
            return null;

        do {
            T currElement = advance();
            if (predicate.test(currElement)) {
                return currElement;
            }
        } while (head != start);
        return null;
    }

    /**
     * Remove an item from this circular list.
     */
    public boolean remove (T obj) {
        // handle the zero-length and one-length case
        if (head == null) return false;

        if (head.prev == head) {
            if (head.element.equals(obj)) {
                head = null;
                return true;
            }
            else return false;
        }

        Node<T> start = head;
        Node<T> current = head;
        do {
            if (current.element.equals(obj)) {
                // remove from rotation
                current.prev.next = current.next;
                current.next.prev = current.prev;

                if (current == head)
                    head = current.next;

                return true;
            }
            current = current.next;
        } while (current != start);

        return false;
    }

}
