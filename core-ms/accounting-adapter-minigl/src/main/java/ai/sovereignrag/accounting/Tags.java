package ai.sovereignrag.accounting;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class Tags implements Serializable {
    private static final long serialVersionUID = -7749305134294641955L;
    private transient Set<String> ts;

    public Tags() {
        ts = Collections.synchronizedSet(new TreeSet<String>());
    }
    public Tags(String tags) {
        this();
        if (tags != null)
            setTags(tags);
    }
    public Tags(String... tags) {
        this();
        if (tags != null) {
            for (String t : tags) {
                t = t.trim();
                if (t.length() > 0)
                    ts.add(t);
            }
        }
    }
    public void setTags(String tags) {
        ts.clear();
        if (tags != null) {
            for (String t : ISOUtil.commaDecode(tags)) {
                t = t.trim();
                if (t.length() > 0)
                    ts.add(t);
            }
        }
    }
    public boolean add (String t) {
        return t != null && ts.add(t.trim());
    }
    public boolean remove (String t) {
        return t != null && ts.remove(t.trim());
    }
    public boolean contains (String t) {
        return t != null && ts.contains(t.trim());
    }
    public Iterator<String> iterator() {
        return ts.iterator();
    }
    public int size() {
        return ts.size();
    }
    public boolean containsAll (Tags tags) {
        return ts.containsAll(tags.ts);
    }
    public boolean containsAny (Tags tags) {
        for (String s : tags.ts) {
            if (ts.contains(s))
                return true;
        }
        return tags.size() == 0;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String s : ts) {
            if (sb.length() > 0)
                sb.append(',');
            for (int i = 0; i<s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '\\':
                    case ',' :
                        sb.append('\\');
                        break;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tags tagSet = (Tags) o;
        return ts.equals(tagSet.ts);
    }

    @Override
    public int hashCode() {
        return 19*ts.hashCode();
    }

    private void writeObject(ObjectOutputStream os)
            throws java.io.IOException {
        os.defaultWriteObject();
        os.writeObject(toString());
    }
    private void readObject(ObjectInputStream is)
            throws java.io.IOException, ClassNotFoundException {
        ts = new TreeSet<>();
        is.defaultReadObject();
        setTags((String) is.readObject());
    }
}