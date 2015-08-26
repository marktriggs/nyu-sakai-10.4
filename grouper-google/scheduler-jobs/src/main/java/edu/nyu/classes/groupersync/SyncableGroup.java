package edu.nyu.classes.groupersync;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

// FIXME: Lombok!
class SyncableGroup {

    private Collection<UserWithRole> members;
    private String id;
    private String title;

    public Collection<UserWithRole> getMembers() {
        return members;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public SyncableGroup(String id, String title, Collection<UserWithRole> members) {
        this.id = id;
        this.title = title;
        this.members = members;
    }

    public Set<String> getUserIds() {
        Set<String> result = new HashSet<String>();

        for (UserWithRole m : getMembers()) {
            result.add(m.getUsername());
        }

        return result;
    }
}

