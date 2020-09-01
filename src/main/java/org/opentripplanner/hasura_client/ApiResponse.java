package org.opentripplanner.hasura_client;

import java.util.List;

public class ApiResponse<HASURA_OBJECT> {
    private Data<HASURA_OBJECT> data;

    public Data<HASURA_OBJECT> getData() {
        return data;
    }

    public void setData(Data<HASURA_OBJECT> data) {
        this.data = data;
    }

    public static class Data<HASURA_OBJECT2> {
        private List<HASURA_OBJECT2> items;

        public List<HASURA_OBJECT2> getItems() {
            return items;
        }

        public void setItems(List<HASURA_OBJECT2> items) {
            this.items = items;
        }
    }
}
