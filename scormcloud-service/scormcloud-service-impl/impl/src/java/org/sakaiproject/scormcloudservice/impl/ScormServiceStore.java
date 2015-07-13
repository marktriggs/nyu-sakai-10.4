package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.id.cover.IdManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// FIXME: foreign keys
/*
  create table scs_scorm_job (uuid varchar(36) primary key, siteid varchar(36), externalid varchar(255), resourceid varchar(255), ctime bigint, mtime bigint, retry_count int default 0, status varchar(32));

  alter table scs_scorm_job add index (status);

  create table scs_scorm_course (uuid varchar(36) primary key, siteid varchar(36), externalid varchar(255), resourceid varchar(255), ctime bigint, mtime bigint);

  alter table scs_scorm_course add index (siteid, externalid);

  create table scs_scorm_registration (uuid varchar(36) primary key, courseid varchar(36), userid varchar(36), ctime bigint, mtime bigint);

  alter table scs_scorm_registration add index (courseid, userid);

  create table scs_scorm_job_info (jobname varchar(36) primary key, last_run_time bigint);

  create table scs_scorm_scores (registrationid varchar(36) primary key, score int);

*/


/*
 * Manage the SCORM uploads through their processing stages.
 */
public class ScormServiceStore {

    // FIXME: add a sakai.properties entry for these.
    final int RETRY_COUNT = 3;
    final int RETRY_DELAY_MS = 0; // 600000

    enum JOB_STATUS {
        NEW,
        PROCESSING,
        COMPLETED,
        TEMPORARILY_FAILED,
        PERMANENTLY_FAILED
    }


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


    public void addCourse(final String siteId, final String externalId, final String resourceId) throws ScormException {
        final String sql = "insert into scs_scorm_job (uuid, siteid, externalid, resourceid, ctime, mtime, retry_count, status) values (?, ?, ?, ?, ?, ?, ?, ?)";

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    ps = connection.prepareStatement(sql);
                    try {
                        ps.setString(1, mintId());
                        ps.setString(2, siteId);
                        ps.setString(3, externalId);
                        ps.setString(4, resourceId);
                        ps.setLong(5, System.currentTimeMillis());
                        ps.setLong(6, System.currentTimeMillis());
                        ps.setInt(7, 0);
                        ps.setString(8, JOB_STATUS.NEW.toString());

                        ps.executeUpdate();
                        connection.commit();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });            
        } catch (SQLException e) {
            throw new ScormException("Failure when adding job to store", e);
        }
    }

    
    public List<ScormJob> getPendingJobs() throws ScormException {
        // check for things that are NEW, or failed + haven't hit their retry count + are candidates for being retried.
        final List<ScormJob> result = new ArrayList<ScormJob>();

        final String sql = "select * from scs_scorm_job where status = ? OR (status = ? AND mtime <= ?)";

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    try {
                        ps = connection.prepareStatement(sql);
                        ps.setString(1, JOB_STATUS.NEW.toString());
                        ps.setString(2, JOB_STATUS.TEMPORARILY_FAILED.toString());
                        ps.setLong(3, System.currentTimeMillis() - RETRY_DELAY_MS);

                        rs = ps.executeQuery();

                        while (rs.next()) {
                            ScormJob job = new ScormJob(rs.getString("uuid"), rs.getString("siteid"), rs.getString("externalid"), rs.getString("resourceid"));
                            result.add(job);
                        }
                    } finally {
                        if (ps != null) { ps.close(); }
                        if (rs != null) { rs.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failure when changing job status", e);
        }

        return result;
    }


    public void startProcessing(final ScormJob job) throws ScormException {
        final String sql = "update scs_scorm_job set mtime = ?, status = ? WHERE uuid = ?";

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    try {
                        ps = connection.prepareStatement(sql);
                        ps.setLong(1, System.currentTimeMillis());
                        ps.setString(2, JOB_STATUS.PROCESSING.toString());
                        ps.setString(3, job.getId());

                        ps.executeUpdate();
                        connection.commit();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failure when changing job status", e);
        }
    }


    public void recordFailure(final ScormJob job) throws ScormException {
        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    try {
                        // Update the job
                        ps = connection.prepareStatement("update scs_scorm_job set mtime = ?, status = ?, retry_count = retry_count + 1 WHERE uuid = ?");
                        ps.setLong(1, System.currentTimeMillis());
                        ps.setString(2, JOB_STATUS.TEMPORARILY_FAILED.toString());
                        ps.setString(3, job.getId());
                        ps.executeUpdate();

                        // Mark jobs as permanently failed as needed
                        ps = connection.prepareStatement("update scs_scorm_job set mtime = ?, status = ? WHERE retry_count >= ?");
                        ps.setLong(1, System.currentTimeMillis());
                        ps.setString(2, JOB_STATUS.PERMANENTLY_FAILED.toString());
                        ps.setInt(3, RETRY_COUNT);
                        ps.executeUpdate();
                        connection.commit();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failure when changing job status", e);
        }
    }


    public void markCompleted(final ScormJob job) throws ScormException {
        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    try {
                        // Update the job
                        ps = connection.prepareStatement("update scs_scorm_job set mtime = ?, status = ? WHERE uuid = ?");
                        ps.setLong(1, System.currentTimeMillis());
                        ps.setString(2, JOB_STATUS.COMPLETED.toString());
                        ps.setString(3, job.getId());
                        ps.executeUpdate();

                        // Create a new course for the job
                        ps = connection.prepareStatement("insert into scs_scorm_course (uuid, siteid, externalid, resourceid, ctime, mtime) values (?, ?, ?, ?, ?, ?)");
                        ps.setString(1, job.getId());
                        ps.setString(2, job.getSiteId());
                        ps.setString(3, job.getExternalId());
                        ps.setString(4, job.getResourceId());
                        ps.setLong(5, System.currentTimeMillis());
                        ps.setLong(6, System.currentTimeMillis());
                        ps.executeUpdate();

                        connection.commit();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failure when changing job status", e);
        }
    }


    public String findCourse(final String siteId, final String externalId) throws ScormException {
        final String[] result = { null };

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    try {
                        // Find the ID for the course
                        ps = connection.prepareStatement("select uuid from scs_scorm_course where siteid = ? AND externalid = ?");
                        ps.setString(1, siteId);
                        ps.setString(2, externalId);
                        rs = ps.executeQuery();

                        if (rs.next()) {
                            result[0] = rs.getString("uuid");
                        }
                    } finally {
                        if (rs != null) { rs.close(); }
                        if (ps != null) { ps.close(); }
                    }
                }
            });

            markCourseForGradeSync(result[0]);

            return result[0];
        } catch (Exception e) {
            throw new ScormException("Failure when searching for course", e);
        }
    }


    public String hasRegistration(final String siteId, final String externalId, final String userId) throws ScormException {
        final String[] result = { null };
        final String courseId = findCourse(siteId, externalId);

        if (courseId == null) {
            throw new ScormException("Couldn't find SCORM course");
        }

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    ResultSet rs = null;
                    try {
                        ps = connection.prepareStatement("select uuid from scs_scorm_registration where courseid = ? AND userid = ?");
                        ps.setString(1, courseId);
                        ps.setString(2, userId);

                        rs = ps.executeQuery();
                        if (rs.next()) {
                            result[0] = rs.getString("uuid");
                        }
                    } finally {
                        if (rs != null) { rs.close(); }
                        if (ps != null) { ps.close(); }
                    }
                }
            });

            return result[0];
        } catch (SQLException e) {
            throw new ScormException("Unknown registration status", e);
        }
    }


    public void recordRegistration(final String registrationId, final String courseId, final String userId)
        throws ScormException {
        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    try {
                        ps = connection.prepareStatement("insert into scs_scorm_registration (uuid, courseid, userid, ctime, mtime) values (?, ?, ?, ?, ?)");
                        ps.setString(1, registrationId);
                        ps.setString(2, courseId);
                        ps.setString(3, userId);
                        ps.setLong(4, System.currentTimeMillis());
                        ps.setLong(5, System.currentTimeMillis());

                        ps.executeUpdate();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failed to record registration", e);
        }
    }

    public String mintId() {
        return IdManager.getInstance().createUuid();
    }


    public void markCourseForGradeSync(final String courseId) throws ScormException {
        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    try {
                        // Update the job
                        ps = connection.prepareStatement("update scs_scorm_course set mtime = ? where uuid = ?");
                        ps.setLong(1, System.currentTimeMillis());
                        ps.setString(2, courseId);
                        ps.executeUpdate();
                        connection.commit();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Failure when changing job status", e);
        }
    }


    public CoursesForSync getCoursesNeedingSync() throws ScormException {
        final List<String> courseIds = new ArrayList<String>();
        final long[] lastSyncTime = new long[1];

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;
                    ResultSet rs = null;

                    try {
                        ps = connection.prepareStatement("select last_run_time from scs_scorm_job_info where jobname = 'GradeSync'");
                        rs = ps.executeQuery();

                        if (rs.next()) {
                            lastSyncTime[0] = rs.getLong("last_run_time");
                        }

                        ps = connection.prepareStatement("select uuid from scs_scorm_course where mtime >= ? AND uuid in (select distinct reg.courseid from scs_scorm_registration reg left outer join scs_scorm_scores scores on reg.uuid = scores.registrationid where scores.registrationid is null)");

                        ps.setLong(1, lastSyncTime[0]);

                        rs = ps.executeQuery();
                        while (rs.next()) {
                            courseIds.add(rs.getString("uuid"));
                        }
                    } finally {
                        if (rs != null) { rs.close(); }
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Unknown registration status", e);
        }

        return new CoursesForSync(courseIds, lastSyncTime[0]);
    }


    public void setLastSyncTime(final Date newSyncTime) throws ScormException {
        final long syncOffsetAmount = 30000;

        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;

                    long offsetTime = newSyncTime.getTime() - syncOffsetAmount;

                    try {
                        ps = connection.prepareStatement("insert into scs_scorm_job_info (jobname, last_run_time) values ('GradeSync', ?)");
                        ps.setLong(1, offsetTime);
                        ps.executeUpdate();
                    } catch (SQLException e) {
                        ps = connection.prepareStatement("update scs_scorm_job_info set last_run_time = ? where jobname = 'GradeSync'");
                        ps.setLong(1, offsetTime);
                        ps.executeUpdate();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Couldn't update sync time", e);
        }
    }


    public void recordScore(final String registrationId, final Long score) throws ScormException {
        try {
            DB.connection(new DBAction() {
                public void execute(Connection connection) throws SQLException {
                    PreparedStatement ps = null;

                    try {
                        ps = connection.prepareStatement("insert into scs_scorm_scores (registrationid, score) values (?, ?)");
                        ps.setString(1, registrationId);
                        ps.setLong(2, score);
                        ps.executeUpdate();
                    } finally {
                        if (ps != null) { ps.close(); }
                    }
                }
            });
        } catch (SQLException e) {
            throw new ScormException("Couldn't update sync time", e);
        }
    }

}
