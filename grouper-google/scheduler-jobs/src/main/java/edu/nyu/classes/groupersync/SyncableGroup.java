package edu.nyu.classes.groupersync;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

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
        this.members = dedupeMemberships(members);
    }

    public Set<String> getUserIds() {
        Set<String> result = new HashSet<String>();

        for (UserWithRole m : getMembers()) {
            result.add(m.getUsername());
        }

        return result;
    }

    // A site might have the user listed more than once with different roles
    // (e.g. in multiple sections).  Only keep their "best" role.
    private Collection<UserWithRole> dedupeMemberships(Collection<UserWithRole> members) {
        Map<String, UserWithRole> result = new HashMap<String, UserWithRole>();

        for (UserWithRole member : members) {
            if (result.containsKey(member.getUsername())) {
                UserWithRole existingEntry = result.get(member.getUsername());

                if (member.isMorePowerfulThan(existingEntry)) {
                    result.put(member.getUsername(), member);
                }
            } else {
                result.put(member.getUsername(), member);
            }
        }

        return result.values();
    }
}

