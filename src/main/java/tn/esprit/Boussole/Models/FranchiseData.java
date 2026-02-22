package tn.esprit.Boussole.Models;

import org.apache.commons.math3.ml.clustering.Clusterable;

public class FranchiseData implements Clusterable {
    private final int id;
    private final String label;
    private final double recettes;
    private final double depenses;

    public FranchiseData(int id, String label, double recettes, double depenses) {
        this.id = id;
        this.label = label;
        this.recettes = recettes;
        this.depenses = depenses;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public double getRecettes() {
        return recettes;
    }

    public double getDepenses() {
        return depenses;
    }

    @Override
    public double[] getPoint() {
        // Le clustering se fera sur 2 dimensions : Recettes (X) et Dépenses (Y)
        return new double[]{recettes, depenses};
    }

    @Override
    public String toString() {
        return String.format("%s (Rec: %.2f, Dep: %.2f)", label, recettes, depenses);
    }
}

