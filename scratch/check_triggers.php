<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');
$stmt = $pdo->query('SHOW TRIGGERS');
while ($row = $stmt->fetch(PDO::FETCH_ASSOC)) {
    print_r($row);
}
echo "Done.\n";
