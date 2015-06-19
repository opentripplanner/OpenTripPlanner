package org.opentripplanner.analyst.broker;

public class QueuePath {

    public String queueType;
    public String userId;
    public String graphId;
    public String jobId; // should be generated like task IDs
    public int taskId = -1;

    /**
     * path is /queueType/userId/graphId/jobId/taskId
     */
    public QueuePath (String uri) {
        String[] pathComponents = uri.split("/");
        // position 0 is empty because the path starts with a slash
        if (pathComponents.length > 1 && !pathComponents[1].isEmpty()) {
            queueType = pathComponents[1];
        }
        if (pathComponents.length > 2 && !pathComponents[2].isEmpty()) {
            userId = pathComponents[2];
        }
        if (pathComponents.length > 3 && !pathComponents[3].isEmpty()) {
            graphId = pathComponents[3];
        }
        if (pathComponents.length > 4 && !pathComponents[4].isEmpty()) {
            jobId = pathComponents[4];
        }
        if (pathComponents.length > 5 && !pathComponents[5].isEmpty()) {
            taskId = Integer.parseInt(pathComponents[5]);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        QueuePath queuePath = (QueuePath) o;

        if (taskId != queuePath.taskId) return false;
        if (graphId != null ? !graphId.equals(queuePath.graphId) : queuePath.graphId != null) return false;
        if (jobId != null ? !jobId.equals(queuePath.jobId) : queuePath.jobId != null) return false;
        if (queueType != null ? !queueType.equals(queuePath.queueType) : queuePath.queueType != null) return false;
        if (userId != null ? !userId.equals(queuePath.userId) : queuePath.userId != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = queueType != null ? queueType.hashCode() : 0;
        result = 31 * result + (userId != null ? userId.hashCode() : 0);
        result = 31 * result + (graphId != null ? graphId.hashCode() : 0);
        result = 31 * result + (jobId != null ? jobId.hashCode() : 0);
        result = 31 * result + taskId;
        return result;
    }

    @Override
    public String toString() {
        return "QueuePath{" +
                "queueType='" + queueType + '\'' +
                ", userId='" + userId + '\'' +
                ", graphId='" + graphId + '\'' +
                ", jobId='" + jobId + '\'' +
                ", taskId=" + taskId +
                '}';
    }
}
