package edu.nyu.classes.groupersync;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;

class Sets {

    static <T> Set<T> subtract(Collection<T> set1, Collection<T> set2) {
        Set<T> result = new HashSet(set1);
        result.removeAll(set2);

        return result;
    }

    interface KeyFn<T> {
        public Object key(T obj);
    }

    static <T> Set<T> union(Collection<T> set1, Collection<T> set2) {
        Set<T> result = new HashSet<T>(set1.size() + set2.size());

        result.addAll(set1);
        result.addAll(set2);

        return result;
    }

    static <T> Set<T> subtract(Collection<T> set1, Collection<T> set2, KeyFn<T> keyfn) {
        Map<Object, T> map = new HashMap<Object, T>();

        for (T obj : set1) {
            map.put(keyfn.key(obj), obj);
        }

        for (T obj : set2) {
            map.remove(keyfn.key(obj));
        }

        return new HashSet(map.values());
    }


}
