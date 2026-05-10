<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');
$stmt = $pdo->query('SHOW COLUMNS FROM franchises');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
