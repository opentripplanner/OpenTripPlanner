package org.opentripplanner.hasura_client;

import java.util.List;

public class ApiResponse<H> {
    private Data<H> data;

    public List<H> items;

    public List<H> getItems() {
        return items;
    }

    public void setItems(List<H> items) {
        this.items = items;
    }

    public Data<H> getData() {
        return data;
    }

    public void setData(Data<H> data) {
        this.data = data;
    }


    public static class Data<H2> {

        private List<H2> items;

        public List<H2> getItems() {
            return items;
        }

        public void setItems(List<H2> items) {
            this.items = items;
        }
    }
}
