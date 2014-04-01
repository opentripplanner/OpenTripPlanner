package org.opentripplanner.osm;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.Lists;

public class Relation extends Tagged implements Serializable {

    private static final long serialVersionUID = 1L;

//    Node nodes;
//    Way ways;
//    Relation relations;

    List<Member> members = Lists.newArrayList();
    
    public static enum Type {
        NODE, WAY, RELATION;
    }
    
    public static class Member implements Serializable {
        private static final long serialVersionUID = 1L;
        Type type;
        long id;
        String role;
        @Override
        public String toString() {
            return String.format("%s %s %d", role, type.toString(), id);
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Relation with tags ");
        sb.append(tags);
        sb.append('\n');
        for (Member member : members) {
            sb.append("  ");
            sb.append(member.toString());
            sb.append('\n');
        }
        sb.append('\n');
        return sb.toString();
    }
}
