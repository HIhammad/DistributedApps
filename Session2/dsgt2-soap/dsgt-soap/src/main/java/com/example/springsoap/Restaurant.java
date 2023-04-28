package com.example.springsoap;

import io.foodmenu.gt.webservice.Order;
import io.foodmenu.gt.webservice.Orderconfirmation;

import java.util.ArrayList;

public class Restaurant {

    ArrayList<Order> list;

    Restaurant(){
        list = new ArrayList<Order>();
    }

    public Orderconfirmation addOrder(Order order){
        list.add(order);
        Orderconfirmation confirm = new Orderconfirmation();
        confirm.setConfirmed(true);
        System.out.println(list);
        return confirm;
    }
}
