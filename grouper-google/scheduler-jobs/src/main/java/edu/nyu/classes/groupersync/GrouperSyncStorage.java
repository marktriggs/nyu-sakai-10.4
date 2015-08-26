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
    private Map<String, SyncableGroup> membershipsForGroup;

    public GrouperSyncStorage() {
        userIdsForGroup = new HashMap<String, Set<String>>();
        membershipsForGroup = new HashMap<String, SyncableGroup>();
    }


    public Set<UserWithRole> getMembers(String groupId) {
        SyncableGroup memberships = membershipsForGroup.get(groupId);

        if (memberships == null) {
            return new HashSet<UserWithRole>();
        }

        return new HashSet(memberships.getMembers());
    }

    public void recordMembers(SyncableGroup group) {
        membershipsForGroup.put(group.getId(), group);

        Set<String> userIds = new HashSet<String>();

        for (UserWithRole user : group.getMembers()) {
            userIds.add(user.getUsername());
        }

        userIdsForGroup.put(group.getId(), userIds);
    }


    public Set<String> getUserIds(String groupId) {
        Set<String> result = userIdsForGroup.get(groupId);

        if (result == null) {
            return new HashSet<String>();
        }

        return result;
    }

}
