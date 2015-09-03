package edu.nyu.classes.groupersync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sakaiproject.db.cover.SqlService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Connection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class GrouperSyncStorage {

    private static final Log log = LogFactory.getLog(GrouperSyncStorage.class);

    interface DBAction {
        public void execute(Connection conn) throws SQLException;
    }

    static class DB {

        public static void connection(DBAction action) throws SQLException {
            Connection connection = null;
            boolean oldAutoCommit;

            try {
                connection = SqlService.borrowConnection();
                oldAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);

                action.execute(connection);

                connection.setAutoCommit(oldAutoCommit);
            } finally {
                if (connection != null) {
                    SqlService.returnConnection(connection);
                }
            }
        }
    }


    // Return the group_id of a Sakai group that was marked as needing syncing.
    //
    // Returns null if the group isn't marked for sync at all.
    public String getGrouperIdForSakaiGroup(final String sakaiGroupId) throws GrouperSyncException {
        final String[] result = new String[1];

        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("select group_id from grouper_groups where sakai_group_id = ?");
                    ps.setString(1, sakaiGroupId);

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        result[0] = rs.getString("group_id");
                    }

                    rs.close();
                    ps.close();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure when finding group ID for Sakai group: " + sakaiGroupId, e);
        }

        return result[0];
    }


    public Set<UserWithRole> getMembers(final String groupId) throws GrouperSyncException {
        final Set<UserWithRole> result = new HashSet<UserWithRole>();

        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("select netid, role from grouper_group_users where group_id = ?");
                    ps.setString(1, groupId);

                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        result.add(new UserWithRole(rs.getString("netid"), rs.getString("role")));
                    }

                    rs.close();
                    ps.close();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure when fetching members for group: " + groupId, e);
        }

        return result;
    }


    public void recordChanges(final String groupId,
                              final Set<UserWithRole> addedUsers,
                              final Set<UserWithRole> droppedUsers,
                              final Set<UserWithRole> changedRoles)
        throws GrouperSyncException {
        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;

                    // Drop users that were removed from groups or who had their roles changed
                    ps = connection.prepareStatement("delete from grouper_group_users where group_id = ? AND netid = ?");

                    for (UserWithRole dropped : Sets.union(droppedUsers, changedRoles)) {
                        ps.setString(1, groupId);
                        ps.setString(2, dropped.getUsername());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();

                    // Handle new users and users with changed roles
                    ps = connection.prepareStatement("insert into grouper_group_users (group_id, netid, role) values (?, ?, ?)");

                    for (UserWithRole added : Sets.union(addedUsers, changedRoles)) {
                        ps.setString(1, groupId);
                        ps.setString(2, added.getUsername());
                        ps.setString(3, added.getRole());
                        ps.addBatch();
                    }

                    ps.executeBatch();
                    ps.close();

                    connection.commit();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure when recording change in members for group: " + groupId, e);
        }
    }


    public Date getLastRunDate() throws GrouperSyncException {
        // Default to 30 days ago just to avoid checking *every* site...
        final Date[] result = new Date[] { new Date(System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000)) };

        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("select value from grouper_status where setting = 'last_run_time'");

                    ResultSet rs = ps.executeQuery();

                    if (rs.next()) {
                        result[0] = new Date(Long.valueOf(rs.getString(1)));
                    }

                    rs.close();
                    ps.close();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure when getting job last_run_time", e);
        }

        return result[0];
    }
    

    public void setLastRunDate(final Date date) throws GrouperSyncException {
        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("delete from grouper_status where setting = 'last_run_time'");
                    ps.executeUpdate();
                    ps.close();

                    ps = connection.prepareStatement("insert into grouper_status (setting, value) values (?, ?)");
                    ps.setString(1, "last_run_time");
                    ps.setString(2, String.valueOf(date.getTime()));
                    ps.executeUpdate();
                    ps.close();

                    connection.commit();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure when setting job last_run_time", e);
        }
    }


    // FIXME: This is just some temporary scaffolding to test the sync process
    // prior to getting the UI up.  We'll delete this at some point.
    public void prepopulateGroupsBasedOnThisOneWeirdTrick() throws GrouperSyncException {
        try {
            DB.connection(new DBAction () {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = connection.prepareStatement("select sr.realm_id" +
                                                                       " from sakai_realm sr" +
                                                                       " inner join sakai_realm_rl_gr srrg on srrg.realm_key = sr.realm_key" +
                                                                       " where srrg.user_id = (select user_id from sakai_user_id_map where eid = 'grouper_sync')");

                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        PreparedStatement insert = connection.prepareStatement("insert into grouper_groups (group_id, sakai_group_id, description) values (?, ?, ?)");

                        String groupId = rs.getString(1);
                        if (groupId.lastIndexOf("/") >= 0) {
                            groupId = groupId.substring(groupId.lastIndexOf("/") + 1);
                        }

                        insert.setString(1, "this-is-my-test-group-fa14-classes-" + groupId.substring(0, 4) + "@nyu.edu");
                        insert.setString(2, groupId);
                        insert.setString(3, groupId);

                        // This might fail for groups we've already handled, but that's OK.
                        try {
                            insert.executeUpdate();
                        } catch (SQLException e) {}
                        insert.close();
                    }

                    rs.close();
                    ps.close();

                    connection.commit();
                };
            });
        } catch (SQLException e) {
            throw new GrouperSyncException("Failure while prepopulating groups", e);
        }
    }

}
