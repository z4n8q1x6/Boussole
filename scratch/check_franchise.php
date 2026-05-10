<?php
$pdo = new PDO('mysql:host=localhost;dbname=boussole', 'root', '');
$stmt = $pdo->query('SELECT id, nom FROM franchise');
print_r($stmt->fetchAll(PDO::FETCH_ASSOC));
