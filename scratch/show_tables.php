<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');
$stmt = $pdo->query('SHOW TABLES');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
