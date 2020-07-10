package com.toptal.springBatch.domain;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.Calendar;


@Data
@Entity
public class Customer {
    @Id
    private int id;
    private String name;
    private Calendar birthday;
    private int transactions;
}
