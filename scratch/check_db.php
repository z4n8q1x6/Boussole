<?php
$conn = new mysqli("localhost", "root", "", "boussole");
if ($conn->connect_error) { die("Connection failed: " . $conn->connect_error); }
$res = $conn->query("SELECT SUM(montant) FROM transaction WHERE type='RECETTE'");
echo "Transaction RECETTE: " . $res->fetch_row()[0] . "\n";
$res = $conn->query("SELECT SUM(montant) FROM transaction WHERE type='DEPENSE'");
echo "Transaction DEPENSE: " . $res->fetch_row()[0] . "\n";
