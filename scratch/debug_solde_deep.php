<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');

echo "=== ÉTAT ACTUEL DE LA BASE ===\n\n";

echo "1. franchises.solde_actuel pour franchise 6:\n";
$stmt = $pdo->query("SELECT solde_actuel FROM franchises WHERE id = 6");
echo "   → " . $stmt->fetch(PDO::FETCH_ASSOC)['solde_actuel'] . " TND\n\n";

echo "2. Transactions franchise 6:\n";
$stmt = $pdo->query("SELECT 
    COALESCE(SUM(CASE WHEN type='RECETTE' THEN montant ELSE 0 END), 0) as recettes,
    COALESCE(SUM(CASE WHEN type='DEPENSE' THEN montant ELSE 0 END), 0) as depenses
    FROM transaction WHERE franchise_id = 6");
$r = $stmt->fetch(PDO::FETCH_ASSOC);
echo "   Recettes: " . $r['recettes'] . " TND\n";
echo "   Dépenses: " . $r['depenses'] . " TND\n";
echo "   calculerSolde() retournerait: " . ($r['recettes'] - $r['depenses']) . " TND\n\n";

echo "3. Charges franchise 6:\n";
$stmt = $pdo->query("SELECT COALESCE(SUM(montant), 0) as total FROM charge WHERE franchise_id = 6");
$charges = $stmt->fetch(PDO::FETCH_ASSOC)['total'];
echo "   Total: " . $charges . " TND\n\n";

echo "4. Calcul dans chargerSolde():\n";
$soldeTransactions = $r['recettes'] - $r['depenses'];
echo "   soldeTransactions = " . $soldeTransactions . "\n";
echo "   totalCharges = " . $charges . "\n";
echo "   solde final = " . ($soldeTransactions - $charges) . " TND\n\n";

echo "5. Y a-t-il des charges qui sont AUSSI des transactions DEPENSE?\n";
$stmt = $pdo->query("SELECT t.description, t.montant, t.date 
    FROM transaction t 
    WHERE t.franchise_id = 6 AND t.type = 'DEPENSE' 
    ORDER BY t.date DESC");
echo "   Transactions DEPENSE:\n";
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    echo "   → " . $row['date'] . " | " . $row['description'] . " | " . $row['montant'] . " TND\n";
}
echo "\n   Charges:\n";
$stmt = $pdo->query("SELECT titre, montant, date_charge FROM charge WHERE franchise_id = 6 ORDER BY date_charge DESC");
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    echo "   → " . $row['date_charge'] . " | " . $row['titre'] . " | " . $row['montant'] . " TND\n";
}
