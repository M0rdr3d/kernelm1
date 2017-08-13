package com.latamautos.kernel.scala.infrastructure.transfer_object;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

/**
 * Created by xavier on 8/4/16.
 */
@Document(indexName = "aggregates", type = "events")
public class AggregateLifecycleEvent {

    public enum Status {
        INITIALIZED, DELETED
    }

    @Id
    public String id;
    public String aggregateName;
    public Status status;


    public AggregateLifecycleEvent(String id, String aggregateName, Status status) {
        this.id = id;
        this.aggregateName = aggregateName;
        this.status = status;
    }

    public AggregateLifecycleEvent() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
