<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');

echo "=== TOUS les budgets de la table budget_previsionnel ===\n";
$stmt = $pdo->query("SELECT * FROM budget_previsionnel ORDER BY franchise_id, annee, mois");
$rows = $stmt->fetchAll(PDO::FETCH_ASSOC);
foreach ($rows as $r) {
    echo sprintf("id=%d | mois=%d | annee=%d | montant=%d | type=%s | franchise_id=%s\n",
        $r['id'], $r['mois'], $r['annee'], $r['montant_cible'], $r['type_budget'], 
        $r['franchise_id'] === null ? 'NULL(global)' : $r['franchise_id']
    );
}

echo "\n=== Pour franchise 6, mois courant (Mai 2026) ===\n";
echo "Budgets applicables: franchise_id=6 OU franchise_id IS NULL, mois=5, annee=2026\n";
$stmt = $pdo->query("SELECT * FROM budget_previsionnel WHERE (franchise_id = 6 OR franchise_id IS NULL) AND mois = 5 AND annee = 2026");
$applicable = $stmt->fetchAll(PDO::FETCH_ASSOC);
if (empty($applicable)) {
    echo "AUCUN budget trouvé pour ce mois!\n";
} else {
    foreach ($applicable as $r) {
        echo sprintf("  → id=%d, montant=%d, type=%s, franchise_id=%s\n",
            $r['id'], $r['montant_cible'], $r['type_budget'],
            $r['franchise_id'] === null ? 'NULL(global)' : $r['franchise_id']
        );
    }
}
