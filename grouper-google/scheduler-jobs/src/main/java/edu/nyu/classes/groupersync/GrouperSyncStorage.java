package edu.nyu.classes.groupersync;

import java.util.HashSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;


public class GrouperSyncStorage {

    static GrouperSyncStorage instance = new GrouperSyncStorage();

    // So lame.  Teehee.  Just scaffolding until we do the DB thing.
    List<String> groupMembersTable = new ArrayList<String>();
    List<String> groupEventsTable = new ArrayList<String>();
    long eventCount = 0;


    public static GrouperSyncStorage getInstance() {
        return instance;
    }

    // private Map<String, Set<String>> userIdsForGroup;
    // private Map<String, SyncableGroup> membershipsForGroup;

    // public GrouperSyncStorage() {
    //     userIdsForGroup = new HashMap<String, Set<String>>();
    //     membershipsForGroup = new HashMap<String, SyncableGroup>();
    // }


    public Set<UserWithRole> getMembers(String groupId) {
        Set<UserWithRole> result = new HashSet<UserWithRole>();

        // FIXME: Add B+Tree and indexes? (just kidding...)
        for (String row : groupMembersTable) {
            if (row.startsWith(groupId + ",")) {
                String[] bits = row.split(",");
                result.add(new UserWithRole(bits[1], bits[2]));
            }
        }

        return result;
    }

    // <id>, <timestamp>, <siteid>, <groupid>, <netid>, 'add', <role>, NULL
    // <id>, <timestamp>, <siteid>, <groupid>, <netid>, 'drop', NULL, NULL
    // <id>, <timestamp>, <siteid>, <groupid>, <netid>, 'role_change', <from_role>, <to_role>

    private String csv(Object ... stuff) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < stuff.length; i++) {
            if (i > 0) {
                sb.append(",");
            }

            sb.append(stuff[i]);
        }

        return sb.toString();
    }

    public void recordChanges(String groupId, Set<UserWithRole> addedUsers, Set<UserWithRole> droppedUsers, Set<UserWithRole> changedRoles) {
        // Update our state table

        long now = System.currentTimeMillis();

        // New users
        for (UserWithRole added : addedUsers) {
            groupMembersTable.add(csv(groupId, added.getUsername(), added.getRole()));
            groupEventsTable.add(csv(eventCount++, now, groupId, added.getUsername(), "add", added.getRole()));
        }

        // Removed users
        for (UserWithRole dropped : droppedUsers) {
            Iterator<String> it = groupMembersTable.iterator();

            while (it.hasNext()) {
                String s = it.next();

                if (s.startsWith(csv(groupId, dropped.getUsername()) + ",")) {
                    it.remove();
                }
            }

            groupEventsTable.add(csv(eventCount++, now, groupId, dropped.getUsername(), "drop", null));
        }

        // Changed roles
        for (UserWithRole changed : changedRoles) {
            Iterator<String> it = groupMembersTable.iterator();

            while (it.hasNext()) {
                String s = it.next();

                if (s.startsWith(csv(groupId, changed.getUsername()) + ",")) {
                    it.remove();
                }
            }

            groupMembersTable.add(csv(groupId, changed.getUsername(), changed.getRole()));
            groupEventsTable.add(csv(eventCount++, now, groupId, changed.getUsername(), "role_change", changed.getRole()));
        }
    }

    
    public void dump() {
        System.err.println("EVENT LOG:");
        for (String event : groupEventsTable) {
            System.err.println(event);
        }


        System.err.println("");
        System.err.println("CURRENT STATE:");
        for (String line : groupMembersTable) {
            System.err.println(line);
        }
    }

}
