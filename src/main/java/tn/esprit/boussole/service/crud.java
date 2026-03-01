package tn.esprit.boussole.service;

import java.sql.SQLException;
import java.util.List;

public interface crud<T> {
<<<<<<< HEAD
    void insertone(T t) throws SQLException;
    void updateone(T t) throws SQLException;
    void deleteone(T t) throws SQLException;
    List<T> selectAll(T t) throws SQLException;
    List<T> selectAll();
=======
    void insertone(T t)throws SQLException;
    void updateone(T t)throws SQLException;
    void deleteone(T t)throws SQLException;
    List<T> selectAll(T t)throws SQLException;
>>>>>>> 2118e9cc01de212c47c7cbfda8004c4fa0bea0f9
}
