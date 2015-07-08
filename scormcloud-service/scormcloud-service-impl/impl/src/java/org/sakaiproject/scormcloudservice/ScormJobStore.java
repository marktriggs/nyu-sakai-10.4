package org.sakaiproject.scormcloudservice.impl;

import org.sakaiproject.scormcloudservice.api.ScormException;

import org.sakaiproject.scormcloudservice.api.ScormException;
import org.sakaiproject.db.cover.SqlService;
import org.sakaiproject.id.cover.IdManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

class ScormJobStore {

    enum JOB_STATUS {
        NEW;
    }

    public void add(String siteId, String externalId, String resourceId) throws ScormException {
        String sql = "INSERT INTO SCS_SCORM_JOB (uuid, siteid, externalid, resourceid, update_time, status) VALUES (?, ?, ?, ?, ?, ?)";

        Connection connection = null;
        PreparedStatement ps = null;
        try {
            connection = SqlService.borrowConnection();
            ps = connection.prepareStatement(sql);
            ps.setString(1, IdManager.getInstance().createUuid());
            ps.setString(2, siteId);
            ps.setString(3, externalId);
            ps.setString(4, resourceId);
            ps.setLong(5, System.currentTimeMillis());
            ps.setString(6, JOB_STATUS.NEW.toString());

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new ScormException("Failure when adding job to store", e);
        } finally {
            if (ps != null) {
                try { ps.close (); } catch (Exception e) {}
            }

            if (connection != null) {
                SqlService.returnConnection(connection);
            }
        }
    }
}
