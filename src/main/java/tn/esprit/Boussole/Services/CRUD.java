package tn.esprit.Boussole.Services;

import java.util.List;

public interface CRUD<T> {
    void insertOne(T t);
    void updateOne(T t);
    void deleteOne(T t);
    List<T> selectAll();
}
