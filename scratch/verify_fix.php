<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');

echo "=== NOUVELLE REQUÊTE (année entière 2026, franchise 6 ou NULL) ===\n";
$stmt = $pdo->query("SELECT 
    COALESCE(SUM(CASE WHEN type_budget='LIMITE_DEPENSE' THEN montant_cible ELSE 0 END), 0) as limite_totale,
    COALESCE(SUM(CASE WHEN type_budget='OBJECTIF_REVENU' THEN montant_cible ELSE 0 END), 0) as objectif_total
    FROM budget_previsionnel 
    WHERE (franchise_id = 6 OR franchise_id IS NULL) AND annee = 2026");
$row = $stmt->fetch(PDO::FETCH_ASSOC);
echo "LIMITE DÉPENSES = " . $row['limite_totale'] . " TND\n";
echo "OBJECTIF REVENU = " . $row['objectif_total'] . " TND\n";

echo "\n=== Détail des lignes incluses ===\n";
$stmt = $pdo->query("SELECT id, mois, montant_cible, type_budget, franchise_id 
    FROM budget_previsionnel 
    WHERE (franchise_id = 6 OR franchise_id IS NULL) AND annee = 2026
    ORDER BY mois");
while ($r = $stmt->fetch(PDO::FETCH_ASSOC)) {
    echo sprintf("  id=%d | mois=%d | %d TND | %s | franchise=%s\n",
        $r['id'], $r['mois'], $r['montant_cible'], $r['type_budget'],
        $r['franchise_id'] === null ? 'GLOBAL' : $r['franchise_id']
    );
}

echo "\n=== SOLDE vérifié ===\n";
$stmt = $pdo->query("SELECT 
    COALESCE(SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END), 0) as recettes,
    COALESCE(SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END), 0) as depenses_tx
    FROM transaction WHERE franchise_id = 6");
$row = $stmt->fetch(PDO::FETCH_ASSOC);
$stmt2 = $pdo->query("SELECT COALESCE(SUM(montant), 0) as total FROM charge WHERE franchise_id = 6");
$row2 = $stmt2->fetch(PDO::FETCH_ASSOC);
echo "Recettes (transactions): " . $row['recettes'] . " TND\n";
echo "Dépenses (transactions): " . $row['depenses_tx'] . " TND\n";
echo "Charges: " . $row2['total'] . " TND\n";
echo "SOLDE = " . ($row['recettes'] - $row['depenses_tx'] - $row2['total']) . " TND\n";
