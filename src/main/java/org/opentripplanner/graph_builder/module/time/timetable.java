package org.opentripplanner.graph_builder.module.time;


    public class timetable implements Comparable<queryData> {

        private int id;
        private long clusterid;
        private int currentspeed;
        private int starttime;
        private int endtime;
        private int daynumber;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public long getClusterid() {
            return clusterid;
        }

        public void setClusterid(long clusterid) {
            this.clusterid = clusterid;
        }

        public int getCurrentspeed() {
            return currentspeed;
        }

        public void setCurrentspeed(int currentspeed) {
            this.currentspeed = currentspeed;
        }

        public int getStarttime() {
            return starttime;
        }

        public void setStarttime(int starttime) {
            this.starttime = starttime;
        }

        public int getEndtime() {
            return endtime;
        }

        public void setEndtime(int endtime) {
            this.endtime = endtime;
        }

        public int getDaynuiber() {
            return daynumber;
        }

        public void setDaynumber(int daynuiber) {
            this.daynumber = daynuiber;
        }

        public int compareTo(timetable o) {
            if (this.getDaynuiber() != o.getDaynuiber())
                return this.getDaynuiber() - o.getDaynuiber();
            if (this.starttime != o.starttime)
                return this.starttime - o.starttime;
            return this.endtime - o.endtime;
        }

        @Override
        public int compareTo(queryData o) {
            if (o.getDay() != this.getDaynuiber())
                return o.getDay() - this.getDaynuiber();
            if (o.getTime() < this.getStarttime())
                return -1;
            if (o.getTime() < this.getEndtime())
                return 0;
            return 1;
        }
    }

