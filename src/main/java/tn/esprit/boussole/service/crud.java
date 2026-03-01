package tn.esprit.boussole.service;

import java.sql.SQLException;
import java.util.List;

public interface crud<T> {
    void insertone(T t) throws SQLException;
    void updateone(T t) throws SQLException;
    void deleteone(T t) throws SQLException;
    List<T> selectAll(T t) throws SQLException;
    List<T> selectAll();
}