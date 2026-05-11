<?php
$conn = new mysqli("localhost", "root", "", "boussole");
if ($conn->connect_error) { die("Connection failed: " . $conn->connect_error); }
$res = $conn->query("SELECT MONTH(date), YEAR(date), SUM(montant) FROM transaction WHERE type='RECETTE' GROUP BY MONTH(date), YEAR(date)");
while ($row = $res->fetch_row()) {
    echo "RECETTE " . $row[0] . "/" . $row[1] . ": " . $row[2] . "\n";
}
$res = $conn->query("SELECT MONTH(date), YEAR(date), SUM(montant) FROM transaction WHERE type='DEPENSE' GROUP BY MONTH(date), YEAR(date)");
while ($row = $res->fetch_row()) {
    echo "DEPENSE " . $row[0] . "/" . $row[1] . ": " . $row[2] . "\n";
}
