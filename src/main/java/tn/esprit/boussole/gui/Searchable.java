package tn.esprit.boussole.gui;

/**
 * Interface que les contrôleurs enfants implémentent pour recevoir
 * le texte de recherche global depuis le header.
 */
public interface Searchable {
    void onSearch(String keyword);
}

