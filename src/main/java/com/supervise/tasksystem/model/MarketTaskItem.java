package com.supervise.tasksystem.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name="MARKET_TASK_ITEM")
@Getter
@Setter
public class MarketTaskItem {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    int marketTaskItemId;
    int unqualifiedNumber;
    boolean isFinished;
    Date finishDate;
    @ManyToOne(cascade=CascadeType.REFRESH)
    @JoinColumn(name = "PRODUCT_TYPE_ID")
    ProductType productType;
    @ManyToOne(cascade=CascadeType.REFRESH)
    @JoinColumn(name = "MARKET_TASK_ID")
    MarketTask marketTask;
}
