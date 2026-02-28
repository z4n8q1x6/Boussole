package tn.esprit.boussole.services;

import java.util.List;

public interface crud<T> {
    void insertone(T t);
    void updateone(T t);
    void deleteone(T t);
    List<T> selectAll();
}
