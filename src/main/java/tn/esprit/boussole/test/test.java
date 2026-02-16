package tn.esprit.boussole.test;

import tn.esprit.boussole.models.user;
import tn.esprit.boussole.service.userService;
import tn.esprit.boussole.utils.MyBdConnexion;

import java.util.List;

public class test {
    public static void main(String[] args) {
     MyBdConnexion cnx=MyBdConnexion.getinstance();
        try {
            userService us = new userService();
            List<user> users = us.selectAll(new user());

            for (user u : users) {
                System.out.println(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
