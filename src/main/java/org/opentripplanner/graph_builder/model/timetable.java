package org.opentripplanner.graph_builder.model;


    public class timetable {

        private int id;
        private long clusterid;
        private int currentspeed;
        private  int starttime;
        private  int endtime;
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
    }

