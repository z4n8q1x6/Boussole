<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');

echo "=== Charges pour franchise_id=6 ===\n";
$stmt = $pdo->query("SELECT * FROM charge WHERE franchise_id = 6");
$charges = $stmt->fetchAll(PDO::FETCH_ASSOC);
print_r($charges);

echo "\n=== Total charges franchise 6 ===\n";
$stmt = $pdo->query("SELECT SUM(montant) as total FROM charge WHERE franchise_id = 6");
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));

echo "\n=== Vérification : 103300 - 34600 = " . (103300 - 34600) . " ===\n";
echo "=== Vérification : 16200 (depenses transactions) + charges = ?\n";
