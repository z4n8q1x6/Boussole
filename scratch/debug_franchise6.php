<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');

echo "=== TOUS les budgets pour franchise_id=6 OU franchise_id IS NULL ===\n";
$stmt = $pdo->query("SELECT * FROM budget_previsionnel WHERE franchise_id = 6 OR franchise_id IS NULL ORDER BY annee, mois");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "\n=== Budgets pour Mai 2026 (mois=5, annee=2026), franchise_id=6 ou NULL ===\n";
$stmt = $pdo->query("SELECT 
    COALESCE(SUM(CASE WHEN type_budget='LIMITE_DEPENSE' THEN montant_cible ELSE 0 END), 0) as limite_totale,
    COALESCE(SUM(CASE WHEN type_budget='OBJECTIF_REVENU' THEN montant_cible ELSE 0 END), 0) as objectif_total
    FROM budget_previsionnel b
    WHERE (b.franchise_id = 6 OR b.franchise_id IS NULL) AND b.mois = 5 AND b.annee = 2026");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "\n=== Transactions franchise_id=6 résumé ===\n";
$stmt = $pdo->query("SELECT type, SUM(montant) as total FROM transaction WHERE franchise_id = 6 GROUP BY type");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "\n=== Solde calculé pour franchise 6 ===\n";
$stmt = $pdo->query("SELECT 
    COALESCE(SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END), 0) as recettes,
    COALESCE(SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END), 0) as depenses,
    COALESCE(SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END), 0) - COALESCE(SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END), 0) as solde
    FROM transaction WHERE franchise_id = 6");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
