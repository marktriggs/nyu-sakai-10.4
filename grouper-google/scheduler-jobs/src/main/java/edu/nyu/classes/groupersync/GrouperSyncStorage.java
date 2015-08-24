package edu.nyu.classes.groupersync;

import java.util.HashSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;


public class GrouperSyncStorage {

    static GrouperSyncStorage instance = new GrouperSyncStorage();

    public static GrouperSyncStorage getInstance() {
        return instance;
    }


    private Map<String, Set<String>> userIdsForGroup;
    private Map<String, Memberships> membershipsForGroup;

    public GrouperSyncStorage() {
        userIdsForGroup = new HashMap<String, Set<String>>();
        membershipsForGroup = new HashMap<String, Memberships>();
    }


    public Set<Membership> getMembers(String groupId) {
        Memberships memberships = membershipsForGroup.get(groupId);

        if (memberships == null) {
            return new HashSet<Membership>();
        }

        return new HashSet(memberships.getMembers());
    }

    public void recordMemberships(Memberships m) {
        membershipsForGroup.put(m.getId(), m);

        Set<String> userIds = new HashSet<String>();

        for (Membership membership : m.getMembers()) {
            userIds.add(membership.getUsername());
        }

        userIdsForGroup.put(m.getId(), userIds);
    }


    public Set<String> getUserIds(String groupId) {
        Set<String> result = userIdsForGroup.get(groupId);

        if (result == null) {
            return new HashSet<String>();
        }

        return result;
    }

}
